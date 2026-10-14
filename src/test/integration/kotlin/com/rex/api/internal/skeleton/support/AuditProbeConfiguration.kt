package com.rex.api.internal.skeleton.support

import com.rex.api.internal.skeleton.context.AuditorContext
import com.rex.api.internal.skeleton.persistence.jpa.entity.main.SoftDeletableExampleEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 감사 주체를 **실제 요청 안에서** 관측하기 위한 테스트 전용 엔드포인트.
 *
 * ## 왜 필요한가
 *
 * 필터를 직접 호출하면 **서블릿 등록이 빠져도 통과한다** — 테스트 코드가 부른 것을
 * 검증할 뿐이다. 실제 요청을 지나야 등록·순서·요청 생명주기까지 함께 검증된다.
 *
 * `/demo` 하위를 쓴다. batch 와 달리 이 레포에는 경로 인터셉터가 없지만,
 * **형제 스켈레톤이 같은 경로를 쓰도록** 맞춘다.
 * **운영 코드에 테스트용 엔드포인트를 만들지 않는다.**
 *
 * ## 응답 형식
 *
 * `<요청 안에서 관측한 감사 주체>:<저장된 행의 created_by>` — 예: `42:42`, `null:0`.
 *
 * 둘을 함께 돌려주는 이유가 있다. `created_by` 만 보면 `SYSTEM_USER_ID` 가 0 이고
 * Kotlin 기본값도 0 이라 **감사 리스너가 아예 안 붙어도** 기대값과 같아진다.
 */
@TestConfiguration
class AuditProbeConfiguration {
    @RestController
    @RequestMapping(PATH)
    class AuditProbeController(
        @Qualifier("mainEntityManagerFactory") private val entityManagerFactory: EntityManagerFactory,
    ) {
        @PostMapping
        fun save(): String {
            val observed = AuditorContext.get()
            val createdBy = inRolledBackTransaction { it.persistAndReadCreatedBy() }
            return "$observed:$createdBy"
        }

        /**
         * `flush → clear → find` 로 DB 왕복을 강제한다. 메모리 위의 객체를 그대로 보면
         * 감사 리스너가 동작하지 않아도 통과할 수 있다.
         */
        private fun EntityManager.persistAndReadCreatedBy(): Long {
            val entity = SoftDeletableExampleEntity(name = "audit-probe")
            persist(entity)
            flush()
            clear()
            return find(SoftDeletableExampleEntity::class.java, entity.id).createdBy
        }

        /** 커밋하지 않는다 — 검증이 끝나면 롤백해 다음 테스트에 행을 남기지 않는다. */
        private fun <T> inRolledBackTransaction(block: (EntityManager) -> T): T {
            val entityManager = entityManagerFactory.createEntityManager()
            return try {
                entityManager.transaction.begin()
                block(entityManager)
            } finally {
                if (entityManager.transaction.isActive) {
                    entityManager.transaction.rollback()
                }
                entityManager.close()
            }
        }
    }

    companion object {
        const val PATH = "/demo/audit-probe"

        /** 프로브 응답을 조립한다. 기대값을 문자열 리터럴로 흩뿌리지 않는다. */
        fun body(
            observed: Long?,
            createdBy: Long,
        ): String = "$observed:$createdBy"
    }
}