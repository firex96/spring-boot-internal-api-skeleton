# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Build
```bash
# Build the project
./gradlew build

# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test
```

### Run
```bash
# 로컬 인프라(MySQL·Redis) — 스키마·계정·업무 테이블까지 만든다
docker compose up -d

# Run the Spring Boot application (SPRING_PROFILES_ACTIVE 가 없으면 local)
./gradlew bootRun

# 비밀값 출처 프로필로 실행 (aws · azure · gcp, 필요하면 ,loki)
SPRING_PROFILES_ACTIVE=aws AWS_SECRET_ID=… ./gradlew bootRun
```

### Code Quality
```bash
# Run ktlint checks
./gradlew ktlintCheck

# Auto-format code with ktlint
./gradlew ktlintFormat

# Run all checks (including tests and ktlint)
./gradlew check
```

### Testing

테스트는 3가지 유형으로 분리되어 있습니다:

| 유형 | 소스 경로 | 설명 | 속도 |
|------|----------|------|------|
| Unit Test | `src/test/unit/` | 순수 유닛 테스트 (MockK, Spring Context 없음) | 빠름 |
| Integration Test | `src/test/integration/` | 통합 테스트 (@SpringBootTest, DB 연동) | 중간 |
| E2E Test | `src/test/e2e/` | End-to-End 테스트 (실제 HTTP 요청) | 느림 |

```bash
# 유닛 테스트만 실행 (가장 빠름)
./gradlew test

# 통합 테스트만 실행
./gradlew integrationTest

# E2E 테스트만 실행 — docker compose up -d 선행, check 에 묶지 않는다
./gradlew e2eTest

# 전체 테스트 실행 (check = ktlint + test + integrationTest)
./gradlew check

# 특정 테스트 클래스 실행
./gradlew test --tests "com.rex.api.internal.skeleton.*TestClassName"
./gradlew integrationTest --tests "*ProductBizServiceTest"
./gradlew e2eTest --tests "*E2ETest"

# 테스트 상세 출력
./gradlew test --info
```

**테스트 유형별 역할:**

1. **Unit Test** (`src/test/unit/`)
   - MockK으로 의존성 Mock, Spring Context 로드 없음
   - 비즈니스 로직, 유틸리티, Mapper 테스트
   - 예: `ProductBizServiceUnitTest`, `SecretKeyCoverageTest`

2. **Integration Test** (`src/test/integration/`)
   - `@SpringBootTest`, `@MockkBean`, Testcontainers 사용
   - Repository CRUD, Service 통합, Controller MockMvc 테스트
   - DB 연동이 필요한 테스트는 여기서 수행
   - 예: `ProductBizServiceTest`, `TestJpaRepositoryTest`, `AwsSecretsManagerProfileIntegrationTest`(LocalStack)

3. **E2E Test** (`src/test/e2e/`)
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `TestRestTemplate` 사용
   - 실제 HTTP 요청/응답 검증, 외부 API Mock 동작 확인
   - API 엔드포인트 접근 가능 여부 및 응답 형식 검증
   - 외부 API는 `@MockkBean`으로 Mock 처리
   - docker compose 로 띄운 로컬 인프라에 `local,e2e` 프로필로 붙는다
   - 예: `ProductRestControllerE2ETest`

**테스트 설정 파일:**
- `config/KotestProjectConfig.kt`: Kotest + Spring Extension 설정
- `config/IntegrationTestConfiguration.kt`: 통합 테스트용 빈 설정 (인메모리 캐시 등)
- `config/TestcontainersConfiguration.kt`: Testcontainers DB 설정
- `config/E2ETestConfiguration.kt`: E2E 테스트용 빈 설정

## Architecture Overview

This is a Spring Boot API skeleton project with a layered architecture designed for multi-datasource scenarios.

### Layer Structure

1. **Controller Layer** (`controller/`)
   - REST endpoints using Spring WebMVC
   - Uses request/response models from `model/` package
   - Handles HTTP concerns only

2. **Service Layer** (`service/`)
   - **Business Services** (`service/biz/`): Core business logic
   - **Mapping Services** (`service/mapping/`): Converts between DTOs and models using MapStruct
   - Services return DTOs, not entities

3. **Persistence Layer** (`persistence/`)
   - **JPA** (`persistence/jpa/`): Entities and repositories for standard CRUD operations
   - **JDBC** (`persistence/jdbc/`): JDBC template for complex queries
   - Multi-datasource configuration supporting 2 MySQL schemas: main (@Primary), sub (each split read/write)

4. **Cross-Cutting Concerns**
   - **Aspects** (`aspect/`): AOP for logging, monitoring (e.g., @SlowMethod)
   - **Filters** (`filter/`): TraceId filter for distributed tracing
   - **Exception Handling** (`handler/`): Global exception handlers
   - **Configuration** (`config/`): Spring configurations including async, client, and persistence

### Key Technical Decisions

1. **Multi-Datasource Architecture**: The project is configured to work with 2 MySQL schemas (`main` @Primary, `sub`) simultaneously, each with its own configuration in `config/persistence/`. Non-primary JDBC access must be qualified (`subNamedParameterJdbcOperations`).

2. **Dual Persistence Strategy**: Both JPA and JDBC Template are available. Use JPA for standard CRUD operations and JDBC Template for complex queries that are difficult to port.

3. **MapStruct for Object Mapping**: All conversions between entities, DTOs, and models use MapStruct mappers. Note: Boolean fields with 'is' prefix won't map correctly.

4. **Kotlin Annotation Target**: In Kotlin data classes, annotations like `@JsonProperty` must use `@field:` target to apply to backing fields (e.g., `@field:JsonProperty("user_name")`). Without this, the annotation applies to the constructor parameter and won't be detected via reflection on fields.

5. **Kotest for Testing**: Uses Kotest framework (not JUnit) with MockK for mocking and Fixture Monkey for test data generation.

6. **Secret Source Profiles**: Secrets are referenced only as kebab-case logical keys (`${db-writer-password}`). Environment variables or the `aws` (Secrets Manager) · `azure` (Key Vault) · `gcp` (Secret Manager) profile fill them. All three cloud starters are `runtimeOnly` and disabled in the base `application.yml`.

7. **Virtual Threads**: Enabled Java 21 virtual threads for better concurrency handling.

### Naming Conventions

### 공통 명명 규칙 (모든 스켈레톤 프로젝트 공통)

> **정본은 [`README.md`](README.md) 「코딩 규약」 절이다.** 명명 표·클래스 접미사·1클래스1파일·트랜잭션·String Enum 이 전부 거기 있다.
>
> 이 절에 표를 다시 두지 않는다 — 사본이 둘이 되면 조용히 어긋난다. 실제로 그런 상태였고(README 쪽이 CLAUDE.md 의 부분집합이었다) 2026-08-31 에 정리했다.
>
> 세 스켈레톤의 그 절은 **글자 단위로 동일**해야 한다. 규약을 고치려면 **세 레포 README 를 함께** 고친다.

### Coding Guidelines

1. **1 Class per 1 File Principle**
   - One top-level `class`/`interface`/`object` (including enum, data, annotation) per file
   - Exceptions: Kotlin extension files (top-level functions/properties only) and `sealed` hierarchies (a sealed type and its direct subtypes); `typealias` and `fun main` are not counted
   - Group related files in sub-packages (e.g., `dto/example/`, `enums/example/`)
   - Detected by `OneClassPerFileConventionTest` (all three skeletons); file name = class name is enforced by ktlint `standard:filename`

2. **Model vs DTO vs Entity Usage**
   - **Model (`model/`)**: Controller layer request/response ONLY
   - **DTO (`dto/`)**: Service layer data transfer, server-to-server communication
   - **Entity (`persistence/`)**: Database mapping only, never exposed to upper layers
   - Conversion flow: Entity ↔ DTO ↔ Model (via MapStruct)
   - 선언 형태(Model·DTO·JdbcEntity = data class + val, JPA Entity = class + var)·Model 전 프로퍼티 `@field:JsonProperty`·nullable 은 DB 와 일치·`!!` 금지·전부-기본값 금지·MapStruct `unmappedTargetPolicy = ERROR`(non-null 타깃 매핑 명시, `is`-prefix `@Mapping`) — [.claude/rules/conventions.md](.claude/rules/conventions.md) 「타입 선언」. 메서드명 단수·복수 규칙은 같은 문서 「명명」

3. **Layered Service Pattern**
   - **Controller** → injects MappingService only (allow-list: MappingService plus request-boundary helpers under `config/`·`util/`; anything else — BizService · EntityService · Repository · Client — is a violation), responds with Model
   - **MappingService** (`service/mapping/`) → converts DTO↔Model, calls BizService — injects BizService · Mapper · Properties only
   - **BizService** (`service/biz/`) → business logic, returns DTO — never injects a Repository (EntityService · `client/~Client` sit below it)
   - 접미사는 `~MappingService`·`~BizService`·`~EntityService`·`~Client`·`~Controller` 만 — 검출 `ServiceNamingConventionTest`·`LayeringConventionTest`
   - **EntityService** (`persistence/.../service/`) → persistence operations, returns DTO

4. **Transaction Management**
   - `@Transactional(readOnly = true)` 클래스 레벨 기본, 변경 메서드만 `@Transactional`(메서드가 클래스를 덮는다)
   - **모든 `@Transactional` 에 `transactionManager` 명시(primary `mainTransactionManager` 포함)** — 데이터소스가 여럿이라 묵시는 primary 로 간다
   - 경계는 BizService·EntityService — MappingService·HTTP 클라이언트 서비스에는 붙이지 않는다
   - 정본 `.claude/rules/conventions.md` 「데이터베이스」 3, 검출 `TransactionManagerConventionTest`
   - `@Async` 는 같은 프록시 규칙 — 같은 클래스 안 호출은 동기. 비동기는 별도 빈(`ExampleAsyncProxyWorker`)으로. 검출 `AsyncSelfInvocationConventionTest`

5. **Exception Handling Pattern**
   - **Business exceptions extend `CodedException`; the HTTP status and division code come from its `ErrorCode` alone.** For a new error, add an `ErrorCode` entry and `throw CodedException(ErrorCode.X)` — create a subclass only when the same code is thrown from several places or must be told apart in a `catch`. No constructor takes a string code
   - `GlobalExceptionHandler` handles `CodedException` in one handler. Every handler responds through `respond(errorCode, reason)`, so **the body `status` always equals the HTTP status**. No handler sets the HTTP status separately from the code
   - Common exceptions: `ClientBadRequestException` (400 G001) · `InvalidRequestParameterException` (400 G001) · `ItemNotFoundException` (404 G009) · `ApiCallException` (default 502 G014, unreachable upstream 503 G016)
   - **Response text is user-facing** — `reason` is the exception message for 4xx business exceptions, `field: validation message` for validation failures, and a fixed sentence from `handler/ErrorReason` otherwise (5xx included). `resultMessage` (= `ErrorCode.message`) is a human-readable title; codes shared across stacks use the same values as asp (canonical table: SKELETON `docs/error-codes.md`). Never put a cause's message, an upstream response text, or a URL into a business exception message — keep details in logs and `cause`
   - The 5xx sentence is chosen by status: 502–504 get the upstream sentence (`ErrorReason.forServerError`)
   - Logging happens in one place, `respond` — 4xx `warn` (original message), 5xx `error` (stack trace)
   - Detection: `ErrorCodeContractTest` (status 400–599, unique division codes, title format) · `ErrorResponseContractIntegrationTest` (body `status` = HTTP status, no internal details in response text)

6. **Caching Pattern**
   - Use `@CacheableNullSafe` instead of `@Cacheable` to prevent caching null values
   - Cache at EntityService level (closest to persistence)
   - Redis-based with configurable TTL

7. **String Enum Pattern**
   - Enums implement `StringEnum` interface with `val value: String`
   - Use `@JsonValue` on value property for JSON serialization
   - Use `AbstractStringEnumConverter` for JPA persistence
   - Use `StringEnumUtil.fromValue()` for conversion

8. **External API Client Pattern**
   - Define interface with `@HttpExchange` annotations
   - Configure via `ClientProperties` (timeouts, connection pool)
   - Wrap in a `client/` component with the `~Client` suffix (e.g., `CoreApiClient`, `ExampleWebClient`) — a BizService (`CoreApiBizService`) injects it; MappingService never injects a Client directly
   - Use `WebClientUtil` for retry logic and error handling

9. **Async Processing Pattern**
   - `@Async` 는 별도 워커 빈에 둔다(`ExampleAsyncProxyWorker`) — 같은 클래스 안에서 부르면 프록시를 지나지 않아 동기로 돈다
   - `MdcTaskDecorator`·`AuditorContextTaskDecorator` 로 traceId·감사 주체 전파
   - requestId 로 상태 추적(`ExampleAsyncProxyStateStore`)

10. **Performance Monitoring**
    - Use `@SlowMethod` annotation for slow method detection
    - Configurable threshold, argument logging
    - Integrates with MDC for traceId correlation

11. **Distributed Tracing**
    - `TraceIdFilter` extracts/generates traceId from `X-Trace-Id` header
    - MDC stores traceId for all log statements
    - `ClientConfiguration`'s `ExchangeFilterFunction` propagates traceId as the `X-Trace-Id` header on `@HttpExchange`/`WebClient` outbound calls

12. **JPA Auditing**
    - Entities extend `BaseAuditedEntity` for auto audit fields
    - `SimpleAuditorAware` reads the auditor from **`AuditorContext`**. **MDC is for logging only and no longer feeds auditing** — an empty audit subject writes a wrong value to the DB, so the channels are separate
    - **This repo has no authentication framework**, so the auditor cannot be derived from an authenticated principal. `HeaderAuditorContextFilter` copies `X-User-Id` into the context — **only when `app.audit.trust-user-header` is on (default `false`)**
    - Falls back to `SYSTEM_USER_ID` when it is off, when the header is absent/unparsable/out of `Int` range, or for non-request contexts
    - **Turning the flag on asserts a deployment contract this code does not enforce**: the service is reachable only through the gateway, and the gateway overwrites `X-User-Id` from its own authentication result so a caller cannot forge it. **Do not turn it on without that boundary.** Signing the header at the gateway would let the service verify it — that needs an infrastructure change, not an authentication framework
    - Async propagation is wired in `AsyncConfiguration` by composing `MdcTaskDecorator` with `AuditorContextTaskDecorator` (empty MDC)
    - `AuditConfiguration` names the provider explicitly: `@EnableJpaAuditing(auditorAwareRef = "auditorProvider")`

### Environment Configuration

- **Profiles**: 비밀값 출처만 고른다 — `local`(docker compose 값) · `aws` · `azure` · `gcp`, 조합용 `loki`. 환경(dev·prod)별 프로필은 없다 — 환경 차이는 환경변수로 준다
- **Configuration Files**: `application.yml`(논리 키 참조) + `application-{local,aws,azure,gcp}.yml`
- **설정값·비밀값**: yml 은 kebab-case 논리 키만 참조, 평문 금지. 정본 [.claude/rules/conventions.md](.claude/rules/conventions.md) 「설정값·비밀값 규약」. 논리 키 목록은 README 「설정과 비밀값」·`.env.example`
- **로컬 개발 환경**: `docker compose up -d` 로 MySQL·Redis 를 띄우고 `local` 프로필로 붙는다. DDL 은 `db/manual/create-*.sql` 이 정본(compose·Testcontainers 공용)
- **Logging**: `logback-spring.xml` 하나 — `<springProfile>` 분기. 콘솔(+파일), `loki` 프로필을 조합하면 Loki
- **Dependencies**: All library versions are defined directly in `build.gradle.kts` (no version catalog)

### Important Notes

- The project follows Korean naming conventions and documentation
- ktlint is strictly enforced - run `./gradlew ktlintFormat` before committing
- All async operations preserve MDC context for distributed tracing
- The project includes OpenAPI/Swagger documentation at `/swagger-ui.html`

---

## 미결 항목 처리

- 지적을 "별도 티켓" 으로 미루려면 **티켓 식별자·담당·시점**을 함께 적는다. 셋 중 하나라도 없으면 그것은 티켓이 아니라 **미해결로 방치한 것**이므로, 지금 처리하거나 사용자에게 판단을 요청한다.
- "나중에 한다" 를 선택할 때는 **미루는 이득**을 한 줄로 적는다. 적을 수 없으면 미루지 않는다.
- 문서에 남긴 미결 항목은 다음 게이트에서 다시 확인한다.

### 테스트 소스셋에서 MapStruct 를 쓰려면

테스트 소스셋(`src/test/{unit,integration,e2e}`)에서 `@Mapper` 를 **직접 선언**하려면 그 소스셋에 kapt 설정을 추가한다.

```kotlin
val kaptIntegrationTest: Configuration by configurations.getting {
    extendsFrom(configurations.getByName("kapt"))
}
```

현재는 세 스켈레톤 어디에도 테스트 소스셋 `@Mapper` 가 없어 설정을 두지 않는다. 테스트가 쓰는 `*MapperImpl` 은 `src/main` 에서 생성돼 test 컴파일 클래스패스에 이미 올라온다.


## MySQL 스키마 및 계정

- 업무 스키마는 `skeleton_main`(@Primary) · `skeleton_sub` 다. 업무 테이블과 데이터는 기동 시 자동 생성/변경하지 않는다 — 각 EMF 가 `hibernate.hbm2ddl.auto=none` 이다.
- DDL 정본은 `src/main/resources/db/manual/create-{main,sub}.sql` 이다. docker compose 초기화(`docker/mysql/init`)와 Testcontainers(`withInitScript`)가 같은 파일을 쓴다. 재실행해도 기존 행을 지우지 않아야 한다(`ManualBusinessSchemaIntegrationTest`).
- 런타임 read/write 는 `skeleton_reader`/`skeleton_writer` 를 쓴다. 논리 키는 `db-{reader,writer}-{username,password}` 다.
- 스키마·테이블의 문자셋·콜레이션은 `utf8mb4` / `utf8mb4_0900_ai_ci` 다.
- 이 레포는 Flyway 를 쓰지 않으며 admin 자격증명을 요구하지 않는다.

## 코드 정리·재검증 범위

- `/simplify` 는 구현자가 테스트 통과 후, 코드 리뷰(PR) 전에 1회 실행한다. 단일 파일 수준의 소규모 변경은 생략 가능. 실행 후 테스트를 재확인한다. 리뷰어는 읽기·판정 전용이므로 실행하지 않는다 — 가독성 문제는 Request Changes 로 돌려보낸다.
- 리뷰·검증에서 발견한 문제를 수정한 뒤의 재검증은 변경 범위에 맞춘다. 검증 리포트 또는 PR 본문에 재검증 범위와 근거(변경 파일 → 영향 경로)를 적는다. "처음부터 전체" 는 횡단 변경일 때만 한다.

| 수정의 변경 범위 | 재검증 범위 |
|---|---|
| 국소 — 단일 모듈 내부, 공용 코드 미변경 | 해당 모듈 테스트 재실행 + 변경 지점과 그것을 쓰는 경로 |
| 공통 — 공용 설정·베이스 클래스·공용 라이브러리 | 사용처를 Grep 으로 열거해 적고, 그 경로들을 재검증 |
| 횡단 — 규약 변경·인증/보안 계층 | `./gradlew check` 전체 재실행 |

## 에이전트 지침

에이전트 작업의 진입점은 [AGENTS.md](AGENTS.md)다. 프로젝트 규칙은 `CLAUDE.md`와 README 「코딩 규약」 원본을 공유한다. `.claude/rules/conventions.md`(front matter `paths`: `**/*.kt` · `**/build.gradle*` · `**/*.sql` · `**/*.yml`)가 있으며, `AGENTS.md`의 탐색 절차에 따라 그 경로를 만지는 작업이면 본문을 읽고 적용한다. 규칙 본문을 별도 사본으로 복제하지 않는다.
