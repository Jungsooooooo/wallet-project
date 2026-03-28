package com.wallet.controller.dto;

import com.wallet.domain.Wallet;
import java.time.Instant;

/**
 * 지갑 생성·조회 API의 공통 응답 형태(현재 잔액과 메타데이터).
 *
 * @param id 지갑 UUID
 * @param balance 현재 잔액
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 잔액 변경 시각
 */
public record WalletResponse(String id, long balance, Instant createdAt, Instant updatedAt) {

    public static WalletResponse from(Wallet w) {
        return new WalletResponse(w.getId(), w.getBalance(), w.getCreatedAt(), w.getUpdatedAt());
    }
}
