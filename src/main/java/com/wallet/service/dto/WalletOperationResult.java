package com.wallet.service.dto;

import com.wallet.domain.LedgerEntryType;
import java.time.Instant;

/**
 * 서비스 계층에서 입금 등 작업을 마친 뒤 컨트롤러·{@link com.wallet.controller.dto.OperationResponse} 로 넘기는 값 객체.
 *
 * @param ledgerEntryId 원장 행 ID
 * @param type 원장 유형
 * @param amount 해당 건 금액
 * @param balanceAfter 처리 직후 잔액
 * @param recordedAt 원장 시각
 * @param idempotentReplay 멱등 재생 여부
 */
public record WalletOperationResult(
        Long ledgerEntryId,
        LedgerEntryType type,
        long amount,
        long balanceAfter,
        Instant recordedAt,
        boolean idempotentReplay
) {
}
