package com.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 입금 요청. 금액은 1 이상의 정수(원).
 *
 * @param walletId 대상 지갑 ID
 * @param amount 입금액
 * @param description 선택 설명(원장에 남길 수 있음)
 */
public record DepositRequest(
        @NotBlank(message = "walletId is required")
        @JsonProperty("walletId")
        String walletId,

        @NotNull(message = "amount is required")
        @Min(1)
        @JsonProperty("amount")
        Long amount,

        String description
) {
}
