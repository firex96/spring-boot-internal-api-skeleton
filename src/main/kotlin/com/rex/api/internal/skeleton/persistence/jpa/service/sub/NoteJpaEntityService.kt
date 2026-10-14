package com.rex.api.internal.skeleton.persistence.jpa.service.sub

import com.rex.api.internal.skeleton.annotation.CacheableNullSafe
import com.rex.api.internal.skeleton.dto.example.NoteDto
import com.rex.api.internal.skeleton.mapper.NoteMapper
import com.rex.api.internal.skeleton.persistence.jpa.repository.sub.NoteJpaRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true, transactionManager = "subTransactionManager")
@Service
class NoteJpaEntityService(
    private val noteJpaRepository: NoteJpaRepository,
    private val mapper: NoteMapper,
) {
    @CacheableNullSafe(value = ["NoteDto"], key = "#id")
    fun get(id: Long): NoteDto =
        noteJpaRepository.findById(id).orElseThrow {
            NoSuchElementException("Note with id $id not found")
        }.let {
            mapper.toDto(it)
        }

    fun getAll(): List<NoteDto> =
        noteJpaRepository.findAll().let {
            mapper.toDtoList(it)
        }

    @Transactional(transactionManager = "subTransactionManager")
    fun create(dto: NoteDto): NoteDto {
        if (dto.id != 0L) {
            throw IllegalArgumentException("ID must be zero for new Note creation")
        }
        val entity = mapper.toEntity(dto)
        return noteJpaRepository.save(entity).let {
            mapper.toDto(it)
        }
    }

    @CachePut(value = ["NoteDto"], key = "#dto.id")
    @Transactional(transactionManager = "subTransactionManager")
    fun modify(dto: NoteDto): NoteDto {
        val id = dto.id
        val existingEntity = noteJpaRepository.findById(id).orElseThrow {
            NoSuchElementException("Note with id $id not found")
        }

        // 기존 엔티티의 필드를 업데이트
        existingEntity.title = dto.title
        existingEntity.content = dto.content
        existingEntity.updatedAt = dto.updatedAt

        return noteJpaRepository.save(existingEntity).let {
            mapper.toDto(it)
        }
    }

    @CacheEvict(value = ["NoteDto"], key = "#id")
    @Transactional(transactionManager = "subTransactionManager")
    fun delete(id: Long) {
        noteJpaRepository.deleteById(id)
    }
}