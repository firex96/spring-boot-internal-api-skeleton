# Internal API Skeleton

**게이트웨이 뒤에서 도는 내부 API 프로젝트를 시작할 때 복사하는 템플릿이다.** 레이어드 구조, 다중 데이터소스, 테스트 3분류, 감사 필드 자동 주입, 클라우드별 비밀값 출처 프로필이 이미 배선돼 있다.

이 문서는 **이 레포를 처음 복사한 사람**을 대상으로 한다. 위에서부터 읽고 「시작하기」를 따라 하면 새 프로젝트가 뜬다.

> **이 스켈레톤에는 인증 프레임워크가 없다.** `spring-boot-starter-security` 를 쓰지 않는다. **게이트웨이를 통해서만 도달 가능하다**는 배포 전제 위에 서 있으며, 그 전제를 코드가 강제하지 않는다. 외부에 직접 노출할 API 라면 `spring-boot-public-api-skeleton` 을 쓴다.

## 형제 스켈레톤

같은 규약을 공유하는 스켈레톤이 셋이다. **어느 것을 복사할지 먼저 고른다.**

| 스켈레톤 | 쓰는 곳 | 인증 |
|---|---|---|
| `spring-boot-public-api-skeleton` | 외부에 노출되는 API | Spring Security + JWT |
| **`spring-boot-internal-api-skeleton`** (이 레포) | 게이트웨이 뒤의 내부 API | 없음 — 망 경계에 의존 |
| `spring-batch-skeleton` | 배치·스케줄 작업 | 웹훅 API Key |

세 레포의 **「코딩 규약」 절은 글자 단위로 동일**하다. 규약을 고치려면 셋을 함께 고친다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Spring Boot | 4.1.1 |
| Kotlin | 2.3.21 |
| Java | 21 |
| Gradle | 9.3.0 |
| ktlint | 1.7.1 |
| DB | MySQL 8 |

**여기 적힌 숫자는 빌드 파일이 정본이다.** 어긋나면 `build.gradle.kts` · `gradle-wrapper.properties` 를 따른다. Jackson 등 **Spring Boot BOM 이 관리하는 라이브러리는 여기 적지 않는다** — 확인하려면 `./gradlew dependencies` 를 쓴다.

## 시작하기

### 1. 사전 준비

```bash
sdk list java && sdk use java 21.0.2-tem     # JDK 21
./gradlew --version                           # Gradle 은 wrapper 를 쓴다 — 별도 설치 불필요
docker info                                   # 로컬 실행(docker compose)과 통합 테스트(Testcontainers)에 쓴다
```

### 2. 이름을 바꾼다 — 다섯 곳

복사 직후 **반드시** 고쳐야 하는 곳이다. 하나라도 빠뜨리면 스켈레톤 이름이 그대로 남는다.

| 고칠 것 | 좌표 | 현재 값 |
|---|---|---|
| 루트 프로젝트 이름 | `settings.gradle.kts:1` | `internal-api-skeleton` |
| group | `build.gradle.kts` `group` | `com.rex` |
| 애플리케이션 이름 | `src/main/resources/application.yml` `spring.application.name` | `internal-api-skeleton` |
| base package | `src/main/kotlin/com/rex/api/internal/skeleton/` | — |
| **Kotest 설정 FQN 3곳** | `build.gradle.kts` `kotest.framework.config.fqn` | 아래 |

> **Kotest FQN 을 잊지 않는다.** base package 를 바꾸면 아래 세 줄이 가리키던 클래스가 사라진다. **빌드 스크립트 안의 문자열이라 IDE 의 패키지 이름 변경이 따라가지 않는다.**
>
> ```kotlin
> systemProperty("kotest.framework.config.fqn", "com.rex.api.internal.skeleton.config.KotestUnitProjectConfig")
> systemProperty("kotest.framework.config.fqn", "com.rex.api.internal.skeleton.config.KotestIntegrationProjectConfig")
> systemProperty("kotest.framework.config.fqn", "com.rex.api.internal.skeleton.config.KotestE2eProjectConfig")
> ```

base package 를 바꿀 때는 문자열 안의 FQN 도 함께 고친다 — `META-INF/spring.factories`, `RedisCacheSerializerFactory` 의 `allowIfSubType`, MapStruct `expression = "java(…)"`, 규약 테스트의 `BASE` 상수.

### 3. 데이터소스를 고른다

업무 스키마 **2개**(`main` @Primary · `sub`)를 각각 read/write 로 나눠 갖는다. 스키마 이름은 `skeleton_main` · `skeleton_sub` 이다 — `application.yml` 의 URL 과 `docker/mysql/init/01-schemas-users.sql` 을 함께 바꾼다. 하나만 쓰면 `sub` 를 지운다(「다중 데이터소스」 참고).

### 4. 비밀값 출처를 고른다

애플리케이션은 비밀값을 **논리 키**(`${db-writer-password}` 같은 kebab-case 이름)로만 참조한다. 어디서 채울지는 프로필이 고른다 — 「설정과 비밀값」 참고. 쓰지 않는 클라우드의 의존성 줄과 프로필 yml 은 지운다.

### 5. 지워도 되는 샘플

| 지울 것 | 함께 지워야 하는 테스트 |
|---|---|
| `Product*` (main 스키마 예제), `Note*` (sub 스키마 예제) | 같은 이름의 단위·통합·e2e 테스트, `context/product/ProductBuilder.kt` |
| `persistence/jpa/entity/main/SoftDeletableExampleEntity.kt` | **`AuditingIntegrationTest` · `AuditorChainIntegrationTest` · `AuditorHeaderDistrustIntegrationTest` · `support/AuditProbeConfiguration.kt`** |
| `service/example/`, `client/example/` (비동기 프록시·WebClient 예제) | `service/example/`, `client/example/` 아래 테스트 |

**샘플만 지우면 빌드가 깨진다.** `SoftDeletableExampleEntity` 는 감사 계약을 검증하는 통합 테스트 셋이 저장 대상으로 쓴다. 지울 거라면 그 테스트들의 저장 대상을 **프로젝트의 실제 감사 엔티티로 바꿔라** — 통째로 지우면 감사 계약을 아무도 검증하지 않게 된다. 예제를 지우고 나면 `db/manual/create-*.sql` 의 테이블도 정리한다.

### 6. 뜨는지 확인한다

```bash
docker compose up -d                 # MySQL·Redis — 스키마·계정·업무 테이블까지 만든다
./gradlew ktlintFormat && ./gradlew build
./gradlew bootRun                    # SPRING_PROFILES_ACTIVE 가 없으면 local 로 뜬다
curl localhost:8888/actuator/health
```

`docker/mysql/init` 과 `db/manual/*.sql` 은 **볼륨을 처음 만들 때만** 실행된다. 바꾼 뒤에는 `docker compose down -v` 로 다시 만든다.

## 아키텍처

**계층을 건너뛰지 않는다.** Controller 가 EntityService 를 직접 부르지 않는다.

```
Controller          HTTP 입출력. Model 만 쓴다. @Valid 로 입력 검증
    │               (DTO·Entity 를 직접 쓰지 않는다)
    ▼
MappingService      DTO ↔ Model 변환(MapStruct). 여러 BizService 를 조합해 응답을 만든다
    │               위치: service/mapping/
    ▼
BizService          비즈니스 로직. 트랜잭션 경계. DTO 를 쓴다
    │               위치: service/biz/
    ▼
EntityService       단일 Entity CRUD. Entity ↔ DTO 변환. 캐싱
    │               위치: persistence/jpa/service/, persistence/jdbc/service/
    ▼
Repository          순수 데이터 접근만
                    위치: persistence/jpa/repository/, persistence/jdbc/repository/
```

Controller 는 **MappingService 만** 주입받는다. BizService 를 직접 주입하지 않는다.

### 감사 주체 — 이 레포에는 인증이 없다

`BaseAuditedEntity` 를 상속한 엔티티는 `created_by` · `updated_by` 가 자동으로 채워진다. **그 값이 어디서 오는지가 보안 계약이다.**

| 순위 | 출처 | 조건 |
|---|---|---|
| 1 | `X-User-Id` 헤더 | **`app.audit.trust-user-header` 가 켜진 경우에만** (기본 `false`) |
| 2 | `Constant.SYSTEM_USER_ID` | 그 외 전부 |

**`HeaderAuditorContextFilter` 라는 이름이 계약을 말한다** — 인증 결과가 아니라 헤더에서 온다. 이 레포에는 인증 프레임워크가 없어 인증 주체를 도출할 수 없다. **MDC 는 로깅 전용이고 감사에 관여하지 않는다.**

> **플래그를 켜는 것은 이 코드가 강제하지 않는 배포 계약을 단언하는 행위다** — 게이트웨이를 통해서만 도달 가능하고, 게이트웨이가 자기 인증 결과로 `X-User-Id` 를 덮어써 호출자가 위조할 수 없어야 한다. **그 경계 없이 켜지 않는다.**

헤더가 비었거나 숫자가 아니거나 `Int` 범위를 벗어나면 `SYSTEM_USER_ID` 로 떨어진다.

<!-- 아래 「코딩 규약」 절은 3레포 공통 정본이다. 한 레포만 고치지 않는다. -->

## 코딩 규약

**이 절이 세 스켈레톤 공통 규약의 정본이다.** `CLAUDE.md` 를 포함한 다른 문서는 여기를 가리킨다. 세 레포에서 이 절은 **글자 단위로 동일**해야 한다.

| 항목 | 규칙 | 올바른 예 | 잘못된 예 |
|---|---|---|---|
| Enum 클래스 | 접미사 없이 명명 | `ProductStatus`, `OrderType` | `ProductStatusEnum` |
| Configuration 클래스 | `Configuration` 접미사 | `SecurityConfiguration` | `SecurityConfig` |
| DataSource 파라미터 | 명시적 이름 | `mainDataSource`, `subDataSource` | `dataSource` |
| enum 패키지 | `enums/` | `...enums.SampleStatus` | `enum/` — Java 예약어라 MapStruct 빌드가 깨진다 |
| 역할 코드·Enum 값·상수 | 풀네임 | `ADMINISTRATOR`, `TEAM_LEADER` | `ADMIN`, `TL` |

클래스 접미사는 그 클래스가 **어느 계층에 속하는지**를 이름만으로 알 수 있게 한다.

| 접미사 | 계층 | 예 |
|---|---|---|
| `~Model` | Controller 입출력 | `SampleModel` |
| `~Dto` | Service 간 전송 | `SampleDto` |
| `~JpaEntity` · `~JdbcEntity` | DB 매핑 | `SampleJpaEntity` |
| `~JpaRepository` · `~JdbcRepository` | 데이터 접근 | `SampleJpaRepository` |
| `~JpaEntityService` · `~JdbcEntityService` | 단일 Entity CRUD | `SampleJpaEntityService` |
| `~BizService` | 비즈니스 로직 | `SampleBizService` |
| `~MappingService` | Controller 입출력 변환 · BizService 호출 (응답은 `~Model`) | `SampleMappingService` |
| `~Client` | 외부 API 어댑터 (`client/` 패키지, `@Component`) | `SampleApiClient` |
| `~Mapper` | MapStruct 변환 | `SampleMapper` |
| `~Configuration` | 설정 | `RedisConfiguration` |

**응답은 `~Model`, 요청 본문은 `~Dto` 를 허용한다.** Controller 는 MappingService 만 주입받고 Model 을 응답한다(허용 목록: MappingService 와 `util/`·`config/` 의 요청 경계 보조 컴포넌트 — `~Resolver`·`~Authorizer`·`~Properties` — 만; 그 외 BizService·EntityService·Repository·Client·임의 서비스는 전부 위반). MappingService 는 BizService · Mapper · Properties 만 주입받는다. `~EntityService` 는 Entity 를 위로 올리지 않는다 — DTO 를 돌려주거나 의도 메서드(`revoke…` · `markUsed…`)로 쓴다. 검출: `ServiceNamingConventionTest` · `LayeringConventionTest`.

**1 Class per 1 File.** 최상위 `class`·`interface`·`object`(enum·data·annotation 포함)는 파일에 하나만 둔다. 예외는 Kotlin extension(최상위 함수·프로퍼티만 있는 파일)과 `sealed` 계층(sealed 타입과 그 직접 하위 타입) 뿐이며 `typealias`·`fun main` 은 세지 않는다. 관련 파일은 하위 패키지로 묶는다. 검출: `OneClassPerFileConventionTest`(파일 이름 = 클래스 이름은 ktlint `standard:filename`).

**Model / DTO / Entity 를 섞지 않는다.** Entity 는 상위 계층에 노출하지 않는다. 변환은 MapStruct 가 한다 — 손으로 매핑 코드를 쓰지 않는다. 선언 형태(Model·DTO·JdbcEntity 는 data class + val, JPA Entity 는 class + var)·nullable·MapStruct 정책의 정본은 `.claude/rules/conventions.md` 「타입 선언」이다.

**트랜잭션은 클래스 레벨에 `@Transactional(readOnly = true)` 를 기본으로 두고, 변경 메서드에만 `@Transactional` 을 붙인다.** 메서드 레벨이 클래스 레벨을 덮는다.

**모든 `@Transactional` 은 `transactionManager` 를 명시한다(primary 포함)** — 데이터소스가 여럿이라 묵시는 primary 로 간다. 정본·근거·검출은 `.claude/rules/conventions.md` 「데이터베이스」 3.

**`@Async` 도 같은 프록시 규칙이다** — 같은 클래스 안 호출은 동기로 돈다. 비동기는 별도 빈으로 나눈다(`.claude/rules/conventions.md` 「데이터베이스」 3).

**문자열 값을 갖는 Enum 은 `StringEnum` 인터페이스를 구현한다.** JPA 영속화는 `AbstractStringEnumConverter`, 역변환은 `StringEnumUtil.fromValue()` 를 쓴다. `name()` 을 DB 값으로 쓰지 않는다 — 이름을 바꾸면 데이터가 깨진다.

**업무 예외는 `CodedException` 을 상속하고, HTTP 상태와 오류 코드는 `ErrorCode` 하나가 정한다.** 새 오류는 `ErrorCode` 에 항목을 추가하고 `throw CodedException(ErrorCode.X)` 로 던진다. `GlobalExceptionHandler` 는 `CodedException` 하나로 받으며, 응답 본문 모양이나 노출 정책이 다른 예외만 전용 핸들러를 둔다. 오류 응답의 문구 필드에는 사용자에게 보여도 되는 문장만 싣는다 — 업무 예외 4xx 는 예외 메시지(하위 예외·외부 응답 문구를 합치지 않는다), 검증 실패는 필드별 검증 메시지, 그 밖은 고정 문장이고 내부 상세는 로그에 남긴다. 예외를 삼키지 않는다.

**비밀값은 논리 키로만 참조한다.** yml 에는 `${db-writer-password}` 같은 kebab-case 논리 키만 적고 평문을 어디에도 적지 않는다. 값은 환경변수 또는 비밀값 출처 프로필(`aws`·`azure`·`gcp`)이 채운다 — 정본은 `.claude/rules/conventions.md` 「설정값·비밀값 규약」.

**ktlint 는 강제다.** 커밋 전에 `./gradlew ktlintFormat` 을 돌린다.

## 테스트

소스셋이 셋이고 **성격이 다르다.** 새 테스트를 어디에 둘지는 「무엇에 의존하는가」로 정한다.

| 소스셋 | Gradle task | 의존 | 도구 |
|---|---|---|---|
| `src/test/unit` | `test` | 없음 — Spring Context 를 띄우지 않는다 | Kotest · MockK · FixtureMonkey |
| `src/test/integration` | `integrationTest` | **Docker** — Testcontainers MySQL·Redis·LocalStack | `@SpringBootTest` |
| `src/test/e2e` | `e2eTest` | **docker compose** 로 띄운 MySQL·Redis | TestRestTemplate |

```bash
./gradlew test                                    # 유닛만 — 가장 빠르다
./gradlew integrationTest                         # Docker 필요
./gradlew check                                   # 유닛 + 통합 (+ ktlint)
docker compose up -d && ./gradlew e2eTest         # e2e 는 check 에 묶지 않는다

./gradlew test --tests "*ProductBizServiceUnitTest"   # 특정 클래스만
```

**커밋 전 전체 확인은 `check` 다.** `./gradlew test` 만 돌리고 넘어가면 통합 테스트를 보지 않은 것이다. e2e 는 로컬 인프라가 필요해 `check` 에서 뺐다 — 필요할 때 따로 돌린다.

### 테스트 지원 클래스

| 클래스 | 위치 | 용도 |
|---|---|---|
| `FixtureMonkeyFactory` | `src/test/unit/.../context/` | 테스트 픽스처 생성 |
| `ProductBuilder` | `src/test/unit/.../context/product/` | 도메인 빌더 예시 |
| `IntegrationTestConfiguration` | `src/test/integration/.../config/` | Redis·WebClient 를 Mock 으로 대체 |
| `TestcontainersConfiguration` | `src/test/integration/.../config/` | Testcontainers MySQL(main·sub)·Redis 기동 |

## 설정과 비밀값

### 프로필 — 비밀값을 어디서 읽을지 고른다

환경(dev·staging·prod)별 프로필은 없다. 환경 간 차이(호스트, 풀 크기, 로그 레벨)는 배포할 때 환경변수로 준다. 프로필은 **비밀값 출처**만 고른다.

| 프로필 | 비밀값 출처 | 필요한 것 |
|---|---|---|
| (없음) | 환경변수 (컨테이너, k8s Secret) | 논리 키를 환경변수로 (`.env.example`) |
| `local` | `application-local.yml` 의 docker compose 값 | `docker compose up -d` |
| `aws` | AWS Secrets Manager — JSON 비밀 하나의 키 | `AWS_SECRET_ID`, AWS 기본 자격증명 체인 |
| `azure` | Azure Key Vault — 논리 키와 같은 이름의 비밀 | `AZURE_KEYVAULT_ENDPOINT`, DefaultAzureCredential |
| `gcp` | GCP Secret Manager — 논리 키와 같은 이름의 비밀 | `GCP_PROJECT_ID`, Application Default Credentials |
| `loki` (조합) | — | Loki 로그 전송. `LOKI_URL` (예: `SPRING_PROFILES_ACTIVE=aws,loki`) |

빠진 논리 키가 있으면 `UnresolvedPlaceholderGuard` 가 **기동을 막고 빠진 키를 나열한다** — 해석되지 않은 `${…}` 가 문자열 그대로 설정값이 되는 일을 막는다.

### 논리 키

| 논리 키 | 환경변수 | 비밀 | 설명 |
|---|---|---|---|
| `db-host` | `DB_HOST` | | MySQL 쓰기 호스트 |
| `db-port` | `DB_PORT` | | 기본 3306 |
| `db-read-host` | `DB_READ_HOST` | | 읽기 복제본 호스트 (기본 `db-host`) |
| `db-reader-username` · `db-reader-password` | `DB_READER_USERNAME` · `DB_READER_PASSWORD` | ✔ | 읽기 계정 |
| `db-writer-username` · `db-writer-password` | `DB_WRITER_USERNAME` · `DB_WRITER_PASSWORD` | ✔ | 쓰기 계정 |
| `redis-host` · `redis-port` | `REDIS_HOST` · `REDIS_PORT` | | |
| `redis-password` | `REDIS_PASSWORD` | ✔ | 기본 빈 값 |
| `redis-ssl-enabled` | `REDIS_SSL_ENABLED` | | 기본 `false` |
| `redis-managed-cluster` | `REDIS_MANAGED_CLUSTER` | | 관리형 Redis 클러스터(단일 엔드포인트)면 `true` — `RedisConfiguration` |
| `core-api-url` | `CORE_API_URL` | | `CoreApi` 예제 클라이언트의 대상 |

Spring 은 환경변수 `DB_WRITER_PASSWORD` 를 논리 키 `db-writer-password` 로 자동 대응시킨다. 비밀 저장소에는 **논리 키 이름(kebab-case) 그대로** 비밀을 만든다 — Key Vault 이름 제약(영숫자·하이픈)을 만족하는 형식이다. 키를 추가하면 `.env.example` 과 `application-gcp.yml` 에도 추가한다(`SecretKeyCoverageTest` 가 누락을 잡는다).

### 클라우드별 비밀 등록 예

```bash
# AWS — JSON 비밀 하나에 키를 모은다
aws secretsmanager create-secret --name internal-api-skeleton/prod \
  --secret-string '{"db-writer-username":"app","db-writer-password":"…","db-reader-username":"ro","db-reader-password":"…"}'
SPRING_PROFILES_ACTIVE=aws AWS_SECRET_ID=internal-api-skeleton/prod DB_HOST=… java -jar app.war

# Azure — 비밀 이름 = 논리 키
az keyvault secret set --vault-name <vault> --name db-writer-password --value '…'
SPRING_PROFILES_ACTIVE=azure AZURE_KEYVAULT_ENDPOINT=https://<vault>.vault.azure.net/ DB_HOST=… java -jar app.war

# GCP — 비밀 이름 = 논리 키 (application-gcp.yml 이 키마다 ${sm@…} 로 대응시킨다)
printf '…' | gcloud secrets create db-writer-password --data-file=-
SPRING_PROFILES_ACTIVE=gcp GCP_PROJECT_ID=<project> DB_HOST=… java -jar app.war
```

### 쓰지 않는 클라우드 지우기

세 클라우드 starter 는 모두 `runtimeOnly` 로 들어 있고 기본 `application.yml` 에서 꺼져 있다. 하나만 쓴다면 나머지를 지운다.

| 클라우드 | `build.gradle.kts` | 리소스 |
|---|---|---|
| AWS | `spring-cloud-aws-dependencies`, `spring-cloud-aws-starter-secrets-manager`, `testcontainers-localstack`, `awssdk:secretsmanager` | `application-aws.yml`, `AwsSecretsManagerProfileIntegrationTest` |
| Azure | `spring-cloud-azure-dependencies`, `spring-cloud-azure-starter-keyvault`, `lowkey-vault-testcontainers` | `application-azure.yml`, `AzureKeyVaultProfileIntegrationTest` |
| GCP | `spring-cloud-gcp-dependencies`, `spring-cloud-gcp-starter-secretmanager` | `application-gcp.yml`, `SecretKeyCoverageTest` 의 GCP 검사 |

기본 `application.yml` 의 `spring.cloud.*` 비활성 블록에서도 해당 줄을 지운다.

| 항목 | 값 |
|---|---|
| 로깅 | `logback-spring.xml` 하나 — `local` 은 컬러 콘솔, 그 외는 콘솔 + 파일(`./logs`), `loki` 를 조합하면 Loki |
| 의존성 | `build.gradle.kts` 에 직접 정의 — version catalog 를 쓰지 않는다 |

## 빌드·실행

```bash
./gradlew ktlintFormat        # 커밋 전 필수
./gradlew ktlintCheck         # 검사만

./gradlew build               # 빌드 + 유닛 테스트
./gradlew build -x test       # 테스트 없이
./gradlew clean build

docker compose up -d
./gradlew bootRun             # local 프로필
```

---

# 이 레포에만 있는 것

## 다중 데이터소스

업무 스키마 **2개**를 각각 read/write 로 나눠 갖는다. 쓰기는 `db-host`, 읽기는 `db-read-host`(없으면 `db-host`)로 가고, `LazyConnectionDataSourceProxy` 가 `readOnly` 트랜잭션을 읽기 풀로 보낸다.

| 스키마 | 설정 클래스 | 트랜잭션 매니저 | 예제 |
|---|---|---|---|
| `skeleton_main` (@Primary) | `config/persistence/Main{DataSource,Jpa,Jdbc}Configuration.kt` | `mainTransactionManager` | `Product`, `Test`, `SoftDeletableExampleEntity` |
| `skeleton_sub` | `config/persistence/Sub{DataSource,Jpa,Jdbc}Configuration.kt` | `subTransactionManager` | `Note` |

**DataSource 빈은 반드시 명시적 이름을 갖는다**(`mainDataSource`·`subDataSource`). 이름 없는 `dataSource` 를 만들면 어느 스키마인지 알 수 없고 주입이 모호해진다.

**primary 가 아닌 스키마의 JDBC 접근은 한정자를 붙인다.** `NamedParameterJdbcOperations` 를 한정자 없이 주입하면 @Primary(main) 가 들어온다 — `NoteJdbcRepository` 의 `@Qualifier("subNamedParameterJdbcOperations")` 가 예다. Spring Data JDBC 리포지토리는 `SubJdbcConfiguration` 의 `jdbcOperationsRef`·`dataAccessStrategyRef` 가 같은 일을 한다.

업무 테이블은 Flyway 로 관리하지 않는다. 정본은 `src/main/resources/db/manual/create-{main,sub}.sql` 이고, docker compose 초기화와 Testcontainers 가 같은 파일을 쓴다. 운영에서는 사람이 실행한다(재실행해도 기존 행을 지우지 않는다 — `ManualBusinessSchemaIntegrationTest`). 문자셋·콜레이션은 `utf8mb4` / `utf8mb4_0900_ai_ci` 다.

| 계정 | 권한 | 쓰는 곳 |
|---|---|---|
| `skeleton_reader` | `SELECT` | read 풀 |
| `skeleton_writer` | `SELECT, INSERT, UPDATE, DELETE` | write 풀 |

## 외부 API 클라이언트 예제

`client/core/CoreApi` 는 `@HttpExchange` 선언형 클라이언트 예제다 — `GET {core-api-url}/products/{id}` 를 호출하고 `CoreApiBizService` → `CoreApiMappingService` 를 거쳐 `/products/{id}/core` 로 노출된다. 요청에는 `TraceIdPropagationFilter` 가 `X-Trace-Id` 를 실어 보낸다.

## 유틸리티

### `WebClientUtil` — 재시도가 붙은 HTTP 클라이언트

```kotlin
webClientUtil.postAsync(
    baseUrl = "https://api.external.com",
    uri = "/notifications",
    headers = HttpHeaders(),
    requestBody = request,
    responseType = NotificationResponse::class.java,
    maxRetryCount = 3,
    waitRetryTime = 2,
).doOnSuccess { … }.doOnError { … }.subscribe()

webClientUtil.get(
    baseUrl = "https://api.external.com",
    uri = "/users/$userId",
    responseType = UserInfo::class.java,
    timeout = 10,
)
```

재시도 분기가 두 예외로 갈린다 — `RetryableException`(5xx) 은 자동 재시도, `NonRetryableException` 은 즉시 실패.

### `@CacheableNullSafe`

`@Cacheable` 과 같지만 **결과가 `null` 이면 캐싱하지 않는다.** null 을 캐싱하면 뒤늦게 생긴 데이터를 TTL 동안 못 본다.

### 그 밖의 헬퍼

| 클래스 | 하는 일 |
|---|---|
| `DateExtensions` | `today.inPeriod(start, end)` — 시작·종료일 포함 |
| `LoggerExtension` | `private val log = logger()` |

## 관련 문서

- [`CLAUDE.md`](CLAUDE.md) — Claude Code 작업 지침
- [`AGENTS.md`](AGENTS.md) — 에이전트 작업 진입점

## 라이선스

MIT — [LICENSE](LICENSE). 이 템플릿으로 만든 프로젝트는 `LICENSE` 를 지우거나 그 프로젝트의 라이선스로 바꾼다.

## 에이전트 지침

에이전트 작업의 진입점은 [AGENTS.md](AGENTS.md)다. 프로젝트 규칙은 `CLAUDE.md`와 README 「코딩 규약」 원본을 공유한다. `.claude/rules/**/*.md`가 있으면 `AGENTS.md`의 탐색 절차에 따라 직접 읽으며, `paths`가 없는 규칙은 항상 적용하고 있는 규칙은 작업 경로에 맞춰 적용한다. 규칙 본문을 별도 사본으로 복제하지 않는다.
