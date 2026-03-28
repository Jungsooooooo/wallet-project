package com.wallet.exception;

/**
 * 요청한 지갑 ID에 해당하는 행이 없을 때 던진다.
 * {@link com.wallet.controller.ApiExceptionHandler} 에서 HTTP 404 {@link org.springframework.http.ProblemDetail} 로 변환된다.
 */
public class WalletNotFoundException extends RuntimeException {

    /**
     * @param walletId 찾지 못한 지갑 식별자(메시지에 포함)
     */
    public WalletNotFoundException(String walletId) {
        super("Wallet not found: " + walletId);
    }
}
