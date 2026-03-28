package com.wallet.service;

import com.wallet.controller.dto.TransactionRowResponse;
import com.wallet.controller.time.TransactionDateTimeFormatter;
import com.wallet.domain.LedgerEntryType;
import com.wallet.domain.Wallet;
import com.wallet.domain.WalletLedgerEntry;
import com.wallet.domain.WithdrawalOutcome;
import com.wallet.domain.WithdrawalOutcomeStatus;
import com.wallet.exception.IdempotencyConflictException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.WalletLedgerRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.WithdrawalOutcomeRepository;
import com.wallet.service.dto.WalletOperationResult;
import com.wallet.service.dto.WithdrawalCommandResult;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 잔액·출금·원장 처리.
 * <p>
 * 입금/출금은 같은 지갑에 대해 동시에 들어올 수 있으므로, 지갑 행을 {@link com.wallet.repository.WalletRepository#findByIdForUpdate(String)} 로
 * 잠근 뒤 잔액을 바꾼다(비관적 락). 한 트랜잭션 안에서 잔액 갱신과 원장 저장을 함께 커밋한다.
 * <p>
 * 출금 멱등성: 클라이언트 {@code transactionId}당 최초 결과를 {@link WithdrawalOutcome} 에 저장하고, 동일 키 재요청 시
 * 잔액을 다시 바꾸지 않고 저장된 결과를 재현한다. 잔액 부족으로 거절된 경우도 같은 행에 남겨 동일한 실패 응답을 재현한다.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final WithdrawalOutcomeRepository outcomeRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletLedgerRepository ledgerRepository,
            WithdrawalOutcomeRepository outcomeRepository
    ) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.outcomeRepository = outcomeRepository;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(String walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    @Transactional
    public Wallet createWallet(long initialBalance) {
        Wallet wallet = new Wallet(initialBalance);
        return walletRepository.save(wallet);
    }

    @Transactional
    public WalletOperationResult deposit(String walletId, long amount, String description) {
        // 동시 입금 직렬화: 해당 wallet 행에 대해 SELECT ... FOR UPDATE 유사 동작
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        wallet.add(amount);
        walletRepository.save(wallet);
        WalletLedgerEntry entry = new WalletLedgerEntry(
                wallet,
                LedgerEntryType.DEPOSIT,
                amount,
                wallet.getBalance(),
                null,
                null,
                description
        );
        entry = ledgerRepository.save(entry);
        return new WalletOperationResult(
                entry.getId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getCreatedAt(),
                false
        );
    }

    @Transactional
    public WithdrawalCommandResult withdraw(String walletId, long amount, String transactionIdRaw) {
        String transactionId = normalizeTransactionId(transactionIdRaw);
        // 지갑 락을 먼저 잡는다. 동일 transactionId 재요청도 이 뒤에서 outcome 조회로 재생되므로 이중 출금이 나지 않는다.
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        return outcomeRepository.findById(transactionId)
                .map(o -> replayWithdrawal(o, walletId, amount))
                .orElseGet(() -> processFirstWithdrawal(wallet, walletId, transactionId, amount));
    }

    /** 이미 저장된 출금 결과(성공/잔액부족)를 그대로 돌려준다. */
    private WithdrawalCommandResult replayWithdrawal(WithdrawalOutcome o, String walletId, long amount) {
        assertOutcomeMatchesRequest(o, walletId, amount);
        if (o.getStatus() == WithdrawalOutcomeStatus.SUCCESS) {
            return new WithdrawalCommandResult.Success(
                    o.getBalanceAtOutcome() != null ? o.getBalanceAtOutcome() : 0L,
                    o.getLedgerEntryId(),
                    true
            );
        }
        return new WithdrawalCommandResult.Insufficient(
                o.getBalanceAtOutcome() != null ? o.getBalanceAtOutcome() : 0L,
                amount,
                true
        );
    }

    /**
     * 최초 출금 시도: 잔액 검사 → 부족하면 outcome만 저장하고 종료.
     * 충분하면 잔액 차감·원장·성공 outcome 저장. 동시에 같은 transactionId로 두 스레드가 들어오면
     * DB 유니크 제약 후 보조 스레드는 catch 후 replay 경로로 합류한다.
     */
    private WithdrawalCommandResult processFirstWithdrawal(
            Wallet wallet,
            String walletId,
            String transactionId,
            long amount
    ) {
        if (wallet.getBalance() < amount) {
            outcomeRepository.saveAndFlush(new WithdrawalOutcome(
                    transactionId,
                    walletId,
                    amount,
                    WithdrawalOutcomeStatus.INSUFFICIENT_FUNDS,
                    wallet.getBalance(),
                    null
            ));
            return new WithdrawalCommandResult.Insufficient(wallet.getBalance(), amount, false);
        }

        wallet.subtract(amount);
        walletRepository.save(wallet);

        WalletLedgerEntry entry;
        try {
            entry = ledgerRepository.saveAndFlush(new WalletLedgerEntry(
                    wallet,
                    LedgerEntryType.WITHDRAWAL,
                    amount,
                    wallet.getBalance(),
                    transactionId,
                    null,
                    null
            ));
        } catch (DataIntegrityViolationException ex) {
            // 원장/결과 동시 삽입 경합: 먼저 커밋된 쪽 outcome 기준으로 재생
            WithdrawalOutcome raced = outcomeRepository.findById(transactionId).orElseThrow();
            return replayWithdrawal(raced, walletId, amount);
        }

        try {
            outcomeRepository.save(new WithdrawalOutcome(
                    transactionId,
                    walletId,
                    amount,
                    WithdrawalOutcomeStatus.SUCCESS,
                    wallet.getBalance(),
                    entry.getId()
            ));
        } catch (DataIntegrityViolationException ex) {
            WithdrawalOutcome raced = outcomeRepository.findById(transactionId).orElseThrow();
            return replayWithdrawal(raced, walletId, amount);
        }

        return new WithdrawalCommandResult.Success(wallet.getBalance(), entry.getId(), false);
    }

    /** 같은 transactionId로 다른 지갑/금액을 재사용하려 할 때 충돌 처리. */
    private static void assertOutcomeMatchesRequest(WithdrawalOutcome o, String walletId, long amount) {
        if (!o.getWalletId().equals(walletId)) {
            throw new IdempotencyConflictException("transactionId is already bound to another wallet");
        }
        if (o.getAmount() != amount) {
            throw new IdempotencyConflictException("transactionId was already processed with a different amount");
        }
    }

    @Transactional(readOnly = true)
    public List<TransactionRowResponse> listLedgerForWallet(String walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException(walletId);
        }
        return ledgerRepository.findByWallet_IdOrderByCreatedAtDesc(walletId).stream()
                .map(e -> toRow(walletId, e))
                .toList();
    }

    private static TransactionRowResponse toRow(String walletId, WalletLedgerEntry e) {
        String date = TransactionDateTimeFormatter.format(e.getCreatedAt());
        // API 스펙: 출금은 withdrawal_amount, 입금은 deposit_amount 쪽에 표시
        if (e.getEntryType() == LedgerEntryType.WITHDRAWAL) {
            return new TransactionRowResponse(
                    e.getTransactionId(),
                    walletId,
                    e.getAmount(),
                    null,
                    e.getBalanceAfter(),
                    date
            );
        }
        return new TransactionRowResponse(
                null,
                walletId,
                null,
                e.getAmount(),
                e.getBalanceAfter(),
                date
        );
    }

    private static String normalizeTransactionId(String transactionIdRaw) {
        if (transactionIdRaw == null) {
            throw new IllegalArgumentException("transactionId is required");
        }
        String id = transactionIdRaw.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        return id;
    }
}
