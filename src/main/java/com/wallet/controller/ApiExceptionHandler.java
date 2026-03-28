package com.wallet.controller;

import com.wallet.exception.IdempotencyConflictException;
import com.wallet.exception.WalletNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 지갑 API 도메인 예외와 공통 검증 오류를 {@link ProblemDetail}(RFC 9457 스타일) 로 통일해 응답한다.
 * 출금 잔액 부족(409)은 {@link com.wallet.controller.WalletController} 에서 직접 조립한다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 잘못된 인자 등으로 400 응답. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Bad request");
        pd.setProperty("timestamp", Instant.now());
        pd.setType(URI.create("https://wallet.example/problems/bad-request"));
        return pd;
    }

    /** 존재하지 않는 지갑 ID → 404. */
    @ExceptionHandler(WalletNotFoundException.class)
    ProblemDetail handleNotFound(WalletNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Wallet not found");
        pd.setProperty("timestamp", Instant.now());
        pd.setType(URI.create("https://wallet.example/problems/wallet-not-found"));
        return pd;
    }

    /** 동일 {@code transactionId} 가 다른 요청에 이미 사용됨 → 409. */
    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotency(IdempotencyConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Idempotency conflict");
        pd.setProperty("timestamp", Instant.now());
        pd.setType(URI.create("https://wallet.example/problems/idempotency-conflict"));
        return pd;
    }

    /** Bean Validation 실패(@NotNull 등) → 400, 필드별 오류 목록 포함. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Invalid request");
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .toList());
        pd.setType(URI.create("https://wallet.example/problems/validation-error"));
        return pd;
    }
}
