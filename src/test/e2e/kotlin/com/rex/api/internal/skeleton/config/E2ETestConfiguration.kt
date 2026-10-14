package com.rex.api.internal.skeleton.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * E2E 테스트용 설정.
 *
 * docker compose 로 띄운 로컬 인프라(MySQL·Redis)를 사용하여 E2E 테스트를 실행합니다.
 * e2eTest 태스크가 local,e2e 프로필을 켠다 — 설정은 application-local.yml 과 src/test/e2e/resources/application-e2e.yml.
 *
 * 실행 전 요구사항: docker compose up -d
 *
 * 사용 예시:
 * ```kotlin
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * @Import(E2ETestConfiguration::class)
 * class MyE2ETest {
 *     // ...
 * }
 * ```
 */
@TestConfiguration
class E2ETestConfiguration {
    /**
     * 테스트용 인메모리 캐시 매니저.
     *
     * Redis 대신 ConcurrentMapCache를 사용하여
     * 테스트 간 캐시 격리를 보장합니다.
     */
    @Bean
    @Primary
    fun testCacheManager(): CacheManager = ConcurrentMapCacheManager()
}