package com.wallet.controller;

import com.wallet.controller.dto.CreateWalletRequest;
import com.wallet.controller.dto.DepositRequest;
import com.wallet.controller.dto.OperationResponse;
import com.wallet.controller.dto.TransactionListResponse;
import com.wallet.controller.dto.WalletLookupRequest;
import com.wallet.controller.dto.WalletResponse;
import com.wallet.controller.dto.WalletTransactionsQueryRequest;
import com.wallet.controller.dto.WithdrawalRequest;
import com.wallet.controller.dto.WithdrawalSuccessResponse;
import com.wallet.domain.Wallet;
import com.wallet.service.WalletService;
import com.wallet.service.dto.WalletOperationResult;
import com.wallet.service.dto.WithdrawalCommandResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 REST API. 조회·거래 목록도 모두 POST로 받아 요청 본문에 식별자를 담는다.
 * 출금 성공은 200, 잔액 부족은 409 {@link ProblemDetail} 로 구분한다.
 */
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse create(@Valid @RequestBody CreateWalletRequest request) {
        long initial = request.initialBalance() != null ? request.initialBalance() : 0L;
        Wallet wallet = walletService.createWallet(initial);
        return WalletResponse.from(wallet);
    }

    @PostMapping("/lookup")
    public WalletResponse lookup(@Valid @RequestBody WalletLookupRequest request) {
        return WalletResponse.from(walletService.getWallet(request.walletId()));
    }

    @PostMapping("/deposit")
    public OperationResponse deposit(@Valid @RequestBody DepositRequest request) {
        WalletOperationResult result = walletService.deposit(
                request.walletId(),
                request.amount(),
                request.description()
        );
        return OperationResponse.from(result);
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<?> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        WithdrawalCommandResult result = walletService.withdraw(
                request.walletId(),
                request.amount(),
                request.transactionId()
        );
        if (result instanceof WithdrawalCommandResult.Success success) {
            return ResponseEntity.ok(new WithdrawalSuccessResponse(
                    request.transactionId().trim(),
                    request.walletId(),
                    request.amount(),
                    success.balanceAfter(),
                    success.idempotentReplay()
            ));
        }
        WithdrawalCommandResult.Insufficient insufficient = (WithdrawalCommandResult.Insufficient) result;
        return ResponseEntity.status(HttpStatus.CONFLICT).body(insufficientBalanceProblem(insufficient));
    }

    @PostMapping("/transactions")
    public TransactionListResponse listTransactions(@Valid @RequestBody WalletTransactionsQueryRequest request) {
        return new TransactionListResponse(walletService.listLedgerForWallet(request.walletId()));
    }

    private static ProblemDetail insufficientBalanceProblem(WithdrawalCommandResult.Insufficient insufficient) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Insufficient balance");
        pd.setTitle("Insufficient balance");
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("currentBalance", insufficient.balanceAtDecision());
        pd.setProperty("requestedAmount", insufficient.requestedAmount());
        pd.setProperty("idempotentReplay", insufficient.idempotentReplay());
        pd.setType(URI.create("https://wallet.example/problems/insufficient-balance"));
        return pd;
    }
}
