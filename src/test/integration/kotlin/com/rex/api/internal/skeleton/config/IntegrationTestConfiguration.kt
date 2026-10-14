package com.rex.api.internal.skeleton.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * 통합 테스트용 설정.
 *
 * 테스트 환경에서 필요한 공통 빈들을 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @SpringBootTest
 * @Import(IntegrationTestConfiguration::class)
 * class MyIntegrationTest {
 *     // ...
 * }
 * ```
 */
@TestConfiguration
class IntegrationTestConfiguration {
    /**
     * 테스트용 인메모리 캐시 매니저.
     *
     * Redis 대신 ConcurrentMapCache를 사용하여 외부 의존성 없이 테스트합니다.
     */
    @Bean
    @Primary
    fun testCacheManager(): CacheManager = ConcurrentMapCacheManager()
}