package com.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.wallet.controller.dto.WalletTransactionsQueryRequest;
import com.wallet.controller.dto.WithdrawalRequest;
import com.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * 거래 목록 API 응답이 스펙대로 snake_case JSON 필드를 포함하는지 HTTP 통합 테스트로 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class WalletTransactionsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletService walletService;

    /** 출금 한 건을 만든 뒤 {@code /api/wallets/transactions} 본문에 기대 필드명이 있는지 확인한다. */
    @Test
    void listTransactions_returnsSnakeCaseFields() {
        var wallet = walletService.createWallet(20_000L);
        String walletId = wallet.getId();

        String withdrawUrl = "http://localhost:" + port + "/api/wallets/withdrawals";
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
                withdrawUrl,
                HttpMethod.POST,
                new HttpEntity<>(new WithdrawalRequest(walletId, 1000L, "TX-LIST-1"), jsonHeaders),
                String.class);

        String listUrl = "http://localhost:" + port + "/api/wallets/transactions";
        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> list = restTemplate.exchange(
                listUrl,
                HttpMethod.POST,
                new HttpEntity<>(new WalletTransactionsQueryRequest(walletId), listHeaders),
                String.class);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).contains("\"transactions\"");
        assertThat(list.getBody()).contains("\"transaction_id\"");
        assertThat(list.getBody()).contains("\"wallet_id\":\"");
        assertThat(list.getBody()).contains("\"withdrawal_amount\"");
        assertThat(list.getBody()).contains("\"balance\"");
        assertThat(list.getBody()).contains("\"withdrawal_date\"");
    }
}
