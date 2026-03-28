package com.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 한 줄짜리 원장/거래 표현. API 스펙상 snake_case 필드명을 쓴다.
 * 출금이면 {@code withdrawal_amount} 만, 입금이면 {@code deposit_amount} 만 채우는 식으로 구분한다.
 *
 * @param transactionId 출금 건의 멱등 키(입금은 null일 수 있음)
 * @param walletId 지갑 ID
 * @param withdrawalAmount 출금액(해당 행이 출금일 때)
 * @param depositAmount 입금액(해당 행이 입금일 때)
 * @param balance 해당 거래 처리 직후 잔액 스냅샷
 * @param withdrawalDate 표시용 날짜 문자열(서비스 포맷터 기준)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionRowResponse(
        @JsonProperty("transaction_id") String transactionId,

        @JsonProperty("wallet_id") String walletId,

        @JsonProperty("withdrawal_amount") Long withdrawalAmount,

        @JsonProperty("deposit_amount") Long depositAmount,

        @JsonProperty("balance") Long balance,

        @JsonProperty("withdrawal_date") String withdrawalDate
) {
}
