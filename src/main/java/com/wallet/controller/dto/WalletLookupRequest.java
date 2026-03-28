package com.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST {@code /api/wallets/lookup} 로 지갑을 조회할 때 쓰는 식별자.
 *
 * @param walletId 조회할 지갑 UUID 문자열(JSON 필드명 {@code walletId})
 */
public record WalletLookupRequest(
        @NotBlank(message = "walletId is required")
        @Size(max = 64)
        @JsonProperty("walletId")
        String walletId
) {
}
