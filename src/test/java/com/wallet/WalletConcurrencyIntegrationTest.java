package com.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.wallet.controller.dto.WithdrawalRequest;
import com.wallet.repository.WalletLedgerRepository;
import com.wallet.service.WalletService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * 실제 HTTP로 애플리케이션을 띄운 뒤 동시 출금·멱등을 검증하는 통합 테스트.
 * 과제 요구: 동일 {@code walletId}에 다수 스레드가 동시 출금해도 과출금·음수 잔액이 없어야 함.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class WalletConcurrencyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletLedgerRepository ledgerRepository;

    /**
     * 100스레드 × 10,000원 출금, 서로 다른 {@code transactionId} → 총 요청 1,000,000원이지만 초기 잔액 500,000원이므로
     * 성공 50건(합계 500,000)만 허용되고 나머지는 409(잔액 부족)여야 한다.
     * 최종 잔액 0, 원장 출금 합 = 초기 잔액으로 과출금이 없음을 확인한다.
     */
    @Test
    void hundredThreadsWithdrawingTenThousandEach_neverOverdrafts_andTotalWithdrawnEqualsSuccessCount()
            throws Exception {
        var wallet = walletService.createWallet(500_000L);
        String walletId = wallet.getId();

        int threads = 100;
        long each = 10_000L;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();

        String url = "http://localhost:" + port + "/api/wallets/withdrawals";
        for (int i = 0; i < threads; i++) {
            int idx = i;
            Callable<ResponseEntity<String>> task = () -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // 스레드마다 고유 멱등 키 → 100번 모두 "별도 출금 시도"로 간주
                WithdrawalRequest body = new WithdrawalRequest(walletId, each, "TX-STRESS-" + idx);
                return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            };
            futures.add(pool.submit(task));
        }

        int ok = 0;
        int conflict = 0;
        for (Future<ResponseEntity<String>> f : futures) {
            ResponseEntity<String> r = f.get();
            if (r.getStatusCode() == HttpStatus.OK) {
                ok++;
            } else if (r.getStatusCode() == HttpStatus.CONFLICT) {
                conflict++;
            } else {
                throw new AssertionError("Unexpected status: " + r.getStatusCode() + " body=" + r.getBody());
            }
        }
        pool.shutdown();

        assertThat(ok + conflict).isEqualTo(threads);
        assertThat(ok).isEqualTo(50);
        assertThat(conflict).isEqualTo(50);

        var refreshed = walletService.getWallet(walletId);
        assertThat(refreshed.getBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(refreshed.getBalance()).isEqualTo(0L);

        // 성공한 출금만 원장에 쌓이므로 합계는 초기 잔액과 같아야 함
        long sumWithdrawals = ledgerRepository.findByWallet_Id(walletId).stream()
                .filter(e -> e.getEntryType().name().equals("WITHDRAWAL"))
                .mapToLong(e -> e.getAmount())
                .sum();

        assertThat(sumWithdrawals).isEqualTo(500_000L);
        assertThat((long) ok * each).isEqualTo(500_000L);
    }

    /**
     * 동일 {@code transactionId}로 두 번 호출 시 한 번만 차감되고, 두 번째는 200 + {@code idempotentReplay: true}.
     */
    @Test
    void duplicateTransactionId_replaysSuccessWithoutDoubleWithdraw() {
        var wallet = walletService.createWallet(50_000L);
        String walletId = wallet.getId();

        String url = "http://localhost:" + port + "/api/wallets/withdrawals";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        WithdrawalRequest body = new WithdrawalRequest(walletId, 10_000L, "TXN_UUID_12345");

        ResponseEntity<String> first = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        ResponseEntity<String> second = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).contains("\"idempotentReplay\":true");

        assertThat(walletService.getWallet(walletId).getBalance()).isEqualTo(40_000L);
    }

    /**
     * 잔액 부족으로 거절된 출금도 outcome에 남으므로, 동일 {@code transactionId} 재요청 시 409 재현 + 잔액 불변.
     */
    @Test
    void duplicateTransactionId_replaysInsufficientBalanceResponse() {
        var wallet = walletService.createWallet(5_000L);
        String walletId = wallet.getId();

        String url = "http://localhost:" + port + "/api/wallets/withdrawals";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        WithdrawalRequest body = new WithdrawalRequest(walletId, 10_000L, "TXN_FAIL_ONCE");

        ResponseEntity<String> first = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        ResponseEntity<String> second = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(first.getBody()).contains("\"currentBalance\":5000");
        assertThat(second.getBody()).contains("\"idempotentReplay\":true");

        assertThat(walletService.getWallet(walletId).getBalance()).isEqualTo(5_000L);
    }
}
