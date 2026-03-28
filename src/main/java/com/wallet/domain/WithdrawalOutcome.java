package com.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 출금 멱등 결과 저장소. PK는 클라이언트가 부여한 {@code transactionId} 전역 유일 키.
 * 성공 시 잔액·원장 행 ID를, 실패(잔액 부족) 시에도 동일 키로 한 번만 기록해 재요청 시 같은 응답을 재현한다.
 */
@Entity
@Table(name = "withdrawal_outcomes")
public class WithdrawalOutcome {

    @Id
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "wallet_id", nullable = false, length = 36)
    private String walletId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WithdrawalOutcomeStatus status;

    @Column(name = "balance_at_outcome")
    private Long balanceAtOutcome;

    @Column(name = "ledger_entry_id")
    private Long ledgerEntryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WithdrawalOutcome() {
    }

    public WithdrawalOutcome(
            String transactionId,
            String walletId,
            long amount,
            WithdrawalOutcomeStatus status,
            Long balanceAtOutcome,
            Long ledgerEntryId
    ) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.amount = amount;
        this.status = status;
        this.balanceAtOutcome = balanceAtOutcome;
        this.ledgerEntryId = ledgerEntryId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getWalletId() {
        return walletId;
    }

    public long getAmount() {
        return amount;
    }

    public WithdrawalOutcomeStatus getStatus() {
        return status;
    }

    public Long getBalanceAtOutcome() {
        return balanceAtOutcome;
    }

    public Long getLedgerEntryId() {
        return ledgerEntryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
