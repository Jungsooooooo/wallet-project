package com.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 출금 요청. {@code transactionId} 는 전역에서 유일해야 하며, 동일 값으로 재요청 시 멱등(이전 결과 재현)된다.
 *
 * @param walletId 출금할 지갑 ID
 * @param amount 출금액(양수)
 * @param transactionId 클라이언트 멱등 키(최대 64자)
 */
public record WithdrawalRequest(
        @NotBlank(message = "walletId is required")
        @JsonProperty("walletId")
        String walletId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        @JsonProperty("amount")
        Long amount,

        @NotBlank(message = "transactionId is required")
        @Size(max = 64, message = "transactionId too long")
        @JsonProperty("transactionId")
        String transactionId
) {
}
