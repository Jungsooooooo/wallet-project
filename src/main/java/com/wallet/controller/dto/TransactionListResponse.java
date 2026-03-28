package com.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 지갑별 원장 목록 응답. JSON 필드명은 {@code transactions}.
 *
 * @param transactions 시간 순(또는 서비스가 정한 순서) 거래 행 목록
 */
public record TransactionListResponse(
        @JsonProperty("transactions") List<TransactionRowResponse> transactions
) {
}
