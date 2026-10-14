package com.rex.api.internal.skeleton.persistence.jpa.service.sub

import com.rex.api.internal.skeleton.config.IntegrationTestConfiguration
import com.rex.api.internal.skeleton.config.TestcontainersConfiguration
import com.rex.api.internal.skeleton.dto.example.NoteDto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * NoteJpaEntityService 통합 테스트.
 *
 * Testcontainers를 통해 실제 MySQL 데이터베이스와 연동하여 테스트합니다.
 * - 실제 JPA Repository 사용
 * - 실제 MapStruct Mapper 사용
 * - 트랜잭션 롤백으로 테스트 격리
 */
@SpringBootTest
@ContextConfiguration(initializers = [TestcontainersConfiguration.Initializer::class])
@ActiveProfiles("testcontainers")
@Import(IntegrationTestConfiguration::class)
@Transactional
class NoteJpaEntityServiceIntegrationTest(
    private val service: NoteJpaEntityService,
) : BehaviorSpec({

    Given("Note 생성을 위한 DTO") {
        val now = LocalDateTime.now()
        val dto = NoteDto(
            id = 0L,
            title = "Test Title",
            content = "Test Content",
            createdAt = now,
            updatedAt = now,
        )

        When("create 메서드를 호출하면") {
            val created = service.create(dto)

            Then("ID가 생성된 NoteDto가 반환된다") {
                created.shouldNotBeNull()
                created.id shouldBeGreaterThan 0L
                created.title shouldBe "Test Title"
                created.content shouldBe "Test Content"
            }
        }
    }

    Given("ID가 0이 아닌 NoteDto") {
        val dto = NoteDto(
            id = 123L,
            title = "Test",
            content = "Content",
            createdAt = null,
            updatedAt = null,
        )

        When("create 메서드를 호출하면") {
            Then("IllegalArgumentException이 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    service.create(dto)
                }
                exception.message shouldBe "ID must be zero for new Note creation"
            }
        }
    }

    Given("저장된 Note") {
        val now = LocalDateTime.now()
        val dto = NoteDto(
            id = 0L,
            title = "Test Title",
            content = "Test Content",
            createdAt = now,
            updatedAt = now,
        )
        val saved = service.create(dto)

        When("get 메서드로 ID로 조회하면") {
            val found = service.get(saved.id)

            Then("저장된 Note이 조회된다") {
                found.shouldNotBeNull()
                found.id shouldBe saved.id
                found.title shouldBe "Test Title"
                found.content shouldBe "Test Content"
            }
        }

        When("존재하지 않는 ID로 조회하면") {
            Then("NoSuchElementException이 발생한다") {
                val exception = shouldThrow<NoSuchElementException> {
                    service.get(999999L)
                }
                exception.message shouldBe "Note with id 999999 not found"
            }
        }
    }

    Given("수정할 Note") {
        val now = LocalDateTime.now()
        val dto = NoteDto(
            id = 0L,
            title = "Original Title",
            content = "Original Content",
            createdAt = now,
            updatedAt = now,
        )
        val saved = service.create(dto)

        When("modify 메서드로 수정하면") {
            val updatedDto = saved.copy(
                title = "Updated Title",
                content = "Updated Content",
            )
            val result = service.modify(updatedDto)

            Then("수정된 Note이 반환된다") {
                result.shouldNotBeNull()
                result.id shouldBe saved.id
                result.title shouldBe "Updated Title"
                result.content shouldBe "Updated Content"
            }

            And("조회 시 수정된 값이 확인된다") {
                val found = service.get(saved.id)
                found.shouldNotBeNull()
                found.title shouldBe "Updated Title"
                found.content shouldBe "Updated Content"
            }
        }
    }

    Given("존재하지 않는 Note ID로 modify 시도") {
        val nonExistentDto = NoteDto(
            id = 999999L,
            title = "Title",
            content = "Content",
            createdAt = null,
            updatedAt = null,
        )

        When("modify 메서드를 호출하면") {
            Then("NoSuchElementException이 발생한다") {
                val exception = shouldThrow<NoSuchElementException> {
                    service.modify(nonExistentDto)
                }
                exception.message shouldBe "Note with id ${nonExistentDto.id} not found"
            }
        }
    }

    Given("삭제할 Note") {
        val now = LocalDateTime.now()
        val dto = NoteDto(
            id = 0L,
            title = "To Be Deleted",
            content = "Delete Me",
            createdAt = now,
            updatedAt = now,
        )
        val saved = service.create(dto)

        When("delete 메서드로 삭제하면") {
            service.delete(saved.id)

            Then("조회 시 NoSuchElementException이 발생한다") {
                shouldThrow<NoSuchElementException> {
                    service.get(saved.id)
                }
            }
        }
    }

    Given("getAll 메서드 테스트를 위한 여러 Note") {
        val now = LocalDateTime.now()
        val countBefore = service.getAll().size

        val dto1 = NoteDto(id = 0L, title = "Title 1", content = "Content 1", createdAt = now, updatedAt = now)
        val dto2 = NoteDto(id = 0L, title = "Title 2", content = "Content 2", createdAt = now, updatedAt = now)
        val dto3 = NoteDto(id = 0L, title = "Title 3", content = "Content 3", createdAt = now, updatedAt = now)

        service.create(dto1)
        service.create(dto2)
        service.create(dto3)

        When("getAll 메서드를 호출하면") {
            val allItems = service.getAll()

            Then("생성한 Note이 추가로 조회된다") {
                allItems.size shouldBe countBefore + 3
            }
        }
    }
})