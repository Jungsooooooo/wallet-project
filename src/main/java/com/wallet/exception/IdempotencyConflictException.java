package com.wallet.exception;

/**
 * 클라이언트 {@code transactionId} 가 이미 다른 출금 요청(다른 지갑·금액 등)에 묶여 있어,
 * 동일 키로 다른 내용의 출금을 시도했을 때 던진다.
 * {@link com.wallet.controller.ApiExceptionHandler} 에서 HTTP 409 로 변환된다.
 */
public class IdempotencyConflictException extends RuntimeException {

    /**
     * @param message 클라이언트에 그대로 노출할 충돌 사유(영문 메시지)
     */
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
