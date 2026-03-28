package com.wallet.controller.dto;

import jakarta.validation.constraints.Min;

/**
 * 지갑 생성 요청 본문. {@code initialBalance} 는 생략 시 0으로 간주한다.
 *
 * @param initialBalance 생성 직후 잔액(원 단위 정수, 0 이상)
 */
public record CreateWalletRequest(
        @Min(0)
        Long initialBalance
) {
}
