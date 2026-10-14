package com.rex.api.internal.skeleton.filter

import com.rex.api.internal.skeleton.config.audit.AuditProperties
import com.rex.api.internal.skeleton.constant.Constant
import com.rex.api.internal.skeleton.context.AuditorContext
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 인바운드 `X-User-Id` 헤더를 감사 주체로 옮긴다.
 *
 * **이 레포에는 인증 프레임워크가 없다.** 그래서 감사 주체를 인증 결과에서 도출할 수
 * 없고, 게이트웨이가 넣어 주는 헤더에 의존한다. 그 사실이 클래스 이름에 드러나 있다 —
 * 인증에서 오는 것이 아니라 **헤더에서** 온다.
 *
 * `app.audit.trust-user-header` 가 켜졌을 때만 읽는다(기본 꺼짐). 헤더는 외부
 * 입력이라 그대로 신뢰하면 호출자가 다른 사용자의 ID 로 감사 기록을 위조할 수 있다.
 *
 * 파싱은 [String.toLongOrNull] 하나로 끝난다 — 빈 값·비숫자·`Long` 범위 초과가 모두
 * `null` 이다. **자르지 않는다.** 자르면 다른 사용자의 ID 로 기록된다.
 *
 * [TraceIdFilter] 다음에 온다([Ordered.HIGHEST_PRECEDENCE] + 1) — traceId 가 먼저
 * 잡혀야 이 필터가 남기는 로그도 상관관계를 갖는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class HeaderAuditorContextFilter(
    private val auditProperties: AuditProperties,
) : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        try {
            AuditorContext.set(resolveUserId(request))
            chain.doFilter(request, response)
        } finally {
            // 안 지우면 스레드 재사용 시 다음 요청이 남의 ID 로 감사되고, 그것은 DB 에 남는다.
            AuditorContext.clear()
        }
    }

    private fun resolveUserId(request: ServletRequest): Long? {
        if (!auditProperties.trustUserHeader) return null
        val httpRequest = request as? HttpServletRequest ?: return null
        return httpRequest.getHeader(Constant.HEADER_USER_ID)?.toLongOrNull()
    }
}