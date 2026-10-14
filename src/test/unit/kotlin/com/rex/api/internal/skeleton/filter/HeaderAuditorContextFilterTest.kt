package com.rex.api.internal.skeleton.filter

import com.rex.api.internal.skeleton.config.audit.AuditProperties
import com.rex.api.internal.skeleton.constant.Constant
import com.rex.api.internal.skeleton.context.AuditorContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * 헤더에서 감사 주체를 옮기는 필터의 계약.
 *
 * 이 레포에는 인증 프레임워크가 없어 헤더가 유일한 식별원이다. 그래서
 * **플래그가 켜졌을 때만** 읽고, 파싱이 애매하면 비운다 — 자르면 다른 사용자의
 * ID 가 된다.
 */
class HeaderAuditorContextFilterTest : BehaviorSpec({

    afterTest { AuditorContext.clear() }

    fun run(
        userHeader: String?,
        trustUserHeader: Boolean = true,
        chain: FilterChain,
    ) {
        val request = MockHttpServletRequest()
        userHeader?.let { request.addHeader(Constant.HEADER_USER_ID, it) }
        HeaderAuditorContextFilter(AuditProperties(trustUserHeader = trustUserHeader))
            .doFilter(request, MockHttpServletResponse(), chain)
    }

    Given("플래그가 켜지고 X-User-Id 가 42 일 때") {
        Then("H-1 체인 안에서 42 가 보이고, 끝난 뒤에는 지워진다") {
            var seen: Long? = null
            run("42") { _, _ -> seen = AuditorContext.get() }

            seen shouldBe 42L
            AuditorContext.get().shouldBeNull()
        }
    }

    Given("플래그가 켜지고 헤더가 없을 때") {
        Then("H-2 체인 안에서도 비어 있다") {
            var seen: Long? = 999
            run(null) { _, _ -> seen = AuditorContext.get() }

            seen.shouldBeNull()
        }
    }

    Given("플래그가 꺼진 상태에서 헤더가 왔을 때") {
        // 헤더는 외부 입력이다 — 켜지 않았으면 감사 주체로 들어오면 안 된다.
        Then("H-3 무시한다") {
            var seen: Long? = 999
            run("42", trustUserHeader = false) { _, _ -> seen = AuditorContext.get() }

            seen.shouldBeNull()
        }
    }

    Given("체인이 예외를 던질 때") {
        Then("H-4 예외가 전파되면서도 반드시 정리된다") {
            shouldThrow<IllegalStateException> {
                run("42") { _, _ -> error("체인 실패") }
            }

            // 남으면 스레드 재사용 시 다음 요청이 남의 ID 로 감사되고, DB 에 남는다.
            AuditorContext.get().shouldBeNull()
        }
    }

    Given("플래그가 켜지고 헤더가 빈 문자열일 때") {
        Then("H-5 비운다") {
            var seen: Long? = 999
            run("") { _, _ -> seen = AuditorContext.get() }

            seen.shouldBeNull()
        }
    }

    Given("플래그가 켜지고 헤더가 숫자가 아닐 때") {
        Then("H-6 비운다") {
            var seen: Long? = 999
            run("abc") { _, _ -> seen = AuditorContext.get() }

            seen.shouldBeNull()
        }
    }

    Given("플래그가 켜지고 헤더가 Int 범위를 넘을 때") {
        Then("H-7 Long 으로 그대로 받는다 — 식별자 사슬 전체가 Long 이라 범위 변환이 없다") {
            var seen: Long? = null
            run("2147483648") { _, _ -> seen = AuditorContext.get() }

            seen shouldBe 2147483648L
        }
    }

    Given("플래그가 켜지고 헤더가 Long 범위를 넘을 때") {
        Then("H-8 비운다 — 파싱 실패는 잘라 쓰지 않는다") {
            var seen: Long? = 999
            run("99999999999999999999") { _, _ -> seen = AuditorContext.get() }

            seen.shouldBeNull()
        }
    }
})