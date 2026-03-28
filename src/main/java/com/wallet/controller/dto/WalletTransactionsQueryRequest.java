package com.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * POST {@code /api/wallets/transactions} 로 해당 지갑의 원장 목록을 조회할 때 사용.
 *
 * @param walletId 거래 내역을 볼 지갑 ID
 */
public record WalletTransactionsQueryRequest(
        @NotBlank(message = "walletId is required")
        @JsonProperty("walletId")
        String walletId
) {
}
