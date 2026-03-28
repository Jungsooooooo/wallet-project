package com.wallet.controller.dto;

/**
 * 출금 성공(HTTP 200) 응답 본문. 잔액 부족은 컨트롤러에서 {@code ProblemDetail}(409) 로 별도 처리한다.
 *
 * @param transactionId 요청에 실었던 멱등 키(정규화·trim 반영)
 * @param walletId 지갑 ID
 * @param amount 출금액
 * @param balanceAfter 출금 직후 잔액
 * @param idempotentReplay 동일 {@code transactionId} 재요청으로 재생된 응답이면 true
 */
public record WithdrawalSuccessResponse(
        String transactionId,
        String walletId,
        long amount,
        long balanceAfter,
        boolean idempotentReplay
) {
}
