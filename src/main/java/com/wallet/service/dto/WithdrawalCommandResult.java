package com.wallet.service.dto;

/**
 * 출금 한 번의 비즈니스 결과. 성공 시 잔액·원장 ID, 실패 시 결정 시점 잔액과 요청액을 구분해 담는다.
 */
public sealed interface WithdrawalCommandResult {

    /**
     * 출금이 반영된 경우(실제 차감이 일어났거나, 동일 {@code transactionId} 로 이미 성공한 걸 재현).
     *
     * @param balanceAfter 출금 반영 후(또는 재생 시점의) 잔액
     * @param ledgerEntryId 원장 행 ID(멱등 재생만 한 경우 null일 수 있음)
     * @param idempotentReplay 동일 키 재요청으로 이 결과를 다시 돌려준 경우 true
     */
    record Success(
            long balanceAfter,
            Long ledgerEntryId,
            boolean idempotentReplay
    ) implements WithdrawalCommandResult {
    }

    /**
     * 잔액이 출금액보다 부족해 거절된 경우(또는 동일 키로 과거에 부족으로 기록된 것을 재현).
     *
     * @param balanceAtDecision 판단 시점 잔액
     * @param requestedAmount 요청 출금액
     * @param idempotentReplay 동일 키로 과거 실패를 재현한 경우 true
     */
    record Insufficient(
            long balanceAtDecision,
            long requestedAmount,
            boolean idempotentReplay
    ) implements WithdrawalCommandResult {
    }
}
