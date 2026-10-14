package com.rex.api.internal.skeleton.persistence.jpa.service.sub

import com.rex.api.internal.skeleton.dto.example.NoteDto
import com.rex.api.internal.skeleton.mapper.NoteMapper
import com.rex.api.internal.skeleton.persistence.jpa.entity.sub.NoteJpaEntity
import com.rex.api.internal.skeleton.persistence.jpa.repository.sub.NoteJpaRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

class NoteJpaEntityServiceUnitTest : DescribeSpec({

    val noteJpaRepository = mockk<NoteJpaRepository>()
    val mapper = mockk<NoteMapper>()
    val service = NoteJpaEntityService(noteJpaRepository, mapper)

    beforeTest {
        clearMocks(noteJpaRepository, mapper)
    }

    describe("get") {

        it("should return NoteDto when entity exists") {
            // given
            val id = 1L
            val now = LocalDateTime.now()
            val entity = mockk<NoteJpaEntity>()
            val expectedDto = NoteDto(
                id = id,
                title = "Test Title",
                content = "Test Content",
                createdAt = now,
                updatedAt = now,
            )

            every { noteJpaRepository.findById(id) } returns Optional.of(entity)
            every { mapper.toDto(entity) } returns expectedDto

            // when
            val result = service.get(id)

            // then
            result shouldBe expectedDto
            verify(exactly = 1) { noteJpaRepository.findById(id) }
            verify(exactly = 1) { mapper.toDto(entity) }
        }

        it("should throw NoSuchElementException when entity does not exist") {
            // given
            val id = 999L

            every { noteJpaRepository.findById(id) } returns Optional.empty()

            // when & then
            val exception = shouldThrow<NoSuchElementException> {
                service.get(id)
            }
            exception.message shouldBe "Note with id $id not found"
        }
    }

    describe("getAll") {

        it("should return list of NoteDto") {
            // given
            val now = LocalDateTime.now()
            val entity1 = mockk<NoteJpaEntity>()
            val entity2 = mockk<NoteJpaEntity>()
            val entityList = listOf(entity1, entity2)
            val expectedDtoList = listOf(
                NoteDto(id = 1L, title = "Title1", content = "Content1", createdAt = now, updatedAt = now),
                NoteDto(id = 2L, title = "Title2", content = "Content2", createdAt = now, updatedAt = now),
            )

            every { noteJpaRepository.findAll() } returns entityList
            every { mapper.toDtoList(entityList) } returns expectedDtoList

            // when
            val result = service.getAll()

            // then
            result shouldBe expectedDtoList
            result.size shouldBe 2
            verify(exactly = 1) { noteJpaRepository.findAll() }
            verify(exactly = 1) { mapper.toDtoList(entityList) }
        }

        it("should return empty list when no entities exist") {
            // given
            every { noteJpaRepository.findAll() } returns emptyList()
            every { mapper.toDtoList(emptyList()) } returns emptyList()

            // when
            val result = service.getAll()

            // then
            result shouldBe emptyList()
        }
    }

    describe("create") {

        it("should create and return NoteDto when id is 0") {
            // given
            val now = LocalDateTime.now()
            val inputDto = NoteDto(
                id = 0L,
                title = "New Title",
                content = "New Content",
                createdAt = now,
                updatedAt = now,
            )
            val entity = mockk<NoteJpaEntity>()
            val savedEntity = mockk<NoteJpaEntity>()
            val expectedDto = inputDto.copy(id = 1L)

            every { mapper.toEntity(inputDto) } returns entity
            every { noteJpaRepository.save(entity) } returns savedEntity
            every { mapper.toDto(savedEntity) } returns expectedDto

            // when
            val result = service.create(inputDto)

            // then
            result shouldBe expectedDto
            verify(exactly = 1) { noteJpaRepository.save(entity) }
        }

        it("should throw IllegalArgumentException when id is not 0") {
            // given
            val inputDto = NoteDto(
                id = 123L,
                title = "Title",
                content = "Content",
                createdAt = null,
                updatedAt = null,
            )

            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                service.create(inputDto)
            }
            exception.message shouldBe "ID must be zero for new Note creation"
        }
    }

    describe("modify") {

        it("should modify and return NoteDto when entity exists") {
            // given
            val now = LocalDateTime.now()
            val inputDto = NoteDto(
                id = 1L,
                title = "Updated Title",
                content = "Updated Content",
                createdAt = now,
                updatedAt = now,
            )
            val existingEntity = mockk<NoteJpaEntity>(relaxed = true)
            val savedEntity = mockk<NoteJpaEntity>()

            every { noteJpaRepository.findById(inputDto.id) } returns Optional.of(existingEntity)
            every { noteJpaRepository.save(existingEntity) } returns savedEntity
            every { mapper.toDto(savedEntity) } returns inputDto

            // when
            val result = service.modify(inputDto)

            // then
            result shouldBe inputDto
            verify(exactly = 1) { noteJpaRepository.findById(inputDto.id) }
            verify(exactly = 1) { existingEntity.title = inputDto.title }
            verify(exactly = 1) { existingEntity.content = inputDto.content }
            verify(exactly = 1) { existingEntity.updatedAt = inputDto.updatedAt }
            verify(exactly = 1) { noteJpaRepository.save(existingEntity) }
        }

        it("should throw NoSuchElementException when entity does not exist") {
            // given
            val inputDto = NoteDto(
                id = 999L,
                title = "Title",
                content = "Content",
                createdAt = null,
                updatedAt = null,
            )

            every { noteJpaRepository.findById(inputDto.id) } returns Optional.empty()

            // when & then
            val exception = shouldThrow<NoSuchElementException> {
                service.modify(inputDto)
            }
            exception.message shouldBe "Note with id ${inputDto.id} not found"
        }
    }

    describe("delete") {

        it("should delete entity by id") {
            // given
            val id = 1L

            every { noteJpaRepository.deleteById(id) } just runs

            // when
            service.delete(id)

            // then
            verify(exactly = 1) { noteJpaRepository.deleteById(id) }
        }
    }
})