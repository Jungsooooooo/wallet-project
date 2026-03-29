# 💰 Wallet API

Spring Boot 기반 **지갑(잔액) 관리 API** 과제 구현체입니다. 입금·출금·거래 이력 조회를 제공하고, 출금 **멱등성**과 **동시성 제어**를 만족하도록 설계했습니다.

---

## 📋 과제 개요

### 목표

- 사용자(지갑)별 **잔액**을 안전하게 관리한다.
- 입금·출금 시 **원장(ledger)** 에 거래가 **한 건씩 누적**되는 구조로 이력을 남긴다.
- 동시에 많은 출금 요청이 와도 **잔액이 음수로 떨어지지 않도록** 하고, **총 출금액이 초기 잔액을 넘지 않도록** 한다.

### 핵심 구현 요소

| 구분 | 내용 |
|------|------|
| 🔒 **동시성** | 같은 `walletId`에 대한 입·출금은 JPA **비관적 쓰기 락**(`PESSIMISTIC_WRITE`)으로 직렬화 |
| 🔁 **출금 멱등** | 클라이언트가 부여한 `transactionId`당 **최초 결과만** 반영하고, 재요청 시 **동일 결과 재현** (성공·잔액 부족 모두) |
| 📒 **원장** | 성공한 입금·출금은 `wallet_ledger`에 행으로 저장; 잔액 부족 출금은 멱등용 `withdrawal_outcomes`에 기록 |
| ⚠️ **오류 응답** | 잔액 부족 시 **409** + `ProblemDetail` 스타일 본문 (`currentBalance`, `requestedAmount`, `idempotentReplay` 등) |

### 동시성 검증 (통합 테스트)

- **하나의 `walletId`** 를 대상으로 **다수 스레드**가 동시에 출금 API를 호출하는 **통합 테스트** 포함
- 예시 시나리오: **100개 스레드**가 각각 **10,000원** 출금 요청 → 최종 잔액이 **0 미만이 아니고**, 원장상 **총 출금 합이 초기 잔액을 초과하지 않음**을 검증
- 동일 `transactionId` 재전송 시 **이중 출금 없음**·**동일 실패 응답 재현** 테스트 포함

### API 형태

- 조회·변경 모두 **POST** + JSON 본문으로 `walletId` 등을 전달 (과제/클라이언트 스펙에 맞춤)
- 상세 스키마는 `postman/wallet-api.postman_collection.json` 참고

---

## ✅ 사전 요구 사항

| 항목 | 버전 / 비고 |
|------|-------------|
| ☕ JDK | **17** |
| 🐳 Docker | (선택) PostgreSQL 프로필 `docker` 사용 시 |

---

## 🚀 클론 후 실행 (기본: H2)

기본 프로필은 **`local`** → **인메모리 H2**, 별도 DB 설치 **불필요**합니다.

```bash
git clone <저장소 URL>
cd wallet-project
```

### 🪟 Windows

```powershell
.\gradlew.bat bootRun
```

### 🍎 macOS / 🐧 Linux

```bash
chmod +x gradlew
./gradlew bootRun
```

브라우저·클라이언트에서 **👉 http://localhost:8080** 으로 호출하면 됩니다.

### 🔧 JDK 경로 (선택)

`gradle.properties`에 `org.gradle.java.home`이 특정 PC 경로로 잡혀 있을 수 있어요. 본인 JDK 17 경로와 다르면 **수정**하거나, `JAVA_HOME`만 맞춘 뒤 **해당 줄 삭제**해도 됩니다.

---

## 🐘 PostgreSQL로 실행 (선택)

1. **DB 기동**

   ```bash
   docker compose up -d
   ```

2. **`docker` 프로필**로 앱 실행

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=docker'
   ```

   Windows:

   ```powershell
   .\gradlew.bat bootRun --args="--spring.profiles.active=docker"
   ```

`docker-compose.yml`과 `application.yml`의 `docker` 프로필이 동일 접속 정보(`walletdb` / `wallet` / 포트 `5432`)를 사용합니다.

---

## 📡 API 한눈에 보기

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/wallets` | 지갑 생성 (`initialBalance` 선택) |
| POST | `/api/wallets/lookup` | 지갑 조회 |
| POST | `/api/wallets/deposit` | 입금 |
| POST | `/api/wallets/withdrawals` | 출금 (`transactionId` 멱등) |
| POST | `/api/wallets/transactions` | 거래(원장) 목록 |

📮 **Postman**: `postman/wallet-api.postman_collection.json` import 후 사용

---

## 🧪 테스트 실행

```bash
./gradlew test
```

동시 출금·멱등 재생 등 **통합 테스트**가 포함되어 있습니다.

---

## 🛠 기술 스택

- Java 17 · Spring Boot 3.4  
- Spring Data JPA · Validation  
- Flyway  
- DB: 기본 **H2** (`local`) / 선택 **PostgreSQL** (`docker`)
