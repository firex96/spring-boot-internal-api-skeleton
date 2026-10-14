package com.rex.api.internal.skeleton

import com.rex.api.internal.skeleton.config.audit.AuditProperties
import com.rex.api.internal.skeleton.config.client.ClientDestinations
import com.rex.api.internal.skeleton.config.client.ClientProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    ClientDestinations::class,
    ClientProperties::class,
    // 자동 스캔이 아니라 명시 목록이다 — 빠뜨리면 빈이 생성되지 않아
    // TraceIdFilter 주입이 실패하고 컨텍스트 로딩부터 깨진다.
    AuditProperties::class,
)
class InternalApiSkeletonApplication

fun main(args: Array<String>) {
    runApplication<InternalApiSkeletonApplication>(*args)
}