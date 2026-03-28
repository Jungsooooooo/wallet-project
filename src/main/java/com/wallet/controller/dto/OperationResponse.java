package com.wallet.controller.dto;

import com.wallet.domain.LedgerEntryType;
import com.wallet.service.dto.WalletOperationResult;
import java.time.Instant;

/**
 * 입금(및 동일 패턴의 잔액 변경) 처리 결과를 API로 내려줄 때 사용.
 * 원장에 기록된 한 건과 처리 후 잔액을 함께 담는다.
 *
 * @param ledgerEntryId 원장 행 ID
 * @param type 원장 유형(입금 등)
 * @param amount 해당 건 금액
 * @param balanceAfter 처리 직후 잔액
 * @param recordedAt 원장 기록 시각
 * @param idempotentReplay 동일 요청 재전송으로 이미 처리된 결과를 다시 돌려준 경우 true
 */
public record OperationResponse(
        Long ledgerEntryId,
        LedgerEntryType type,
        long amount,
        long balanceAfter,
        Instant recordedAt,
        boolean idempotentReplay
) {

    public static OperationResponse from(WalletOperationResult r) {
        return new OperationResponse(
                r.ledgerEntryId(),
                r.type(),
                r.amount(),
                r.balanceAfter(),
                r.recordedAt(),
                r.idempotentReplay()
        );
    }
}
