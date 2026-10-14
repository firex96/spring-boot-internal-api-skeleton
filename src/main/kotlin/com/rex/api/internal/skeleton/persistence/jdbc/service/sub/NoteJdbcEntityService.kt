package com.rex.api.internal.skeleton.persistence.jdbc.service.sub

import com.rex.api.internal.skeleton.annotation.CacheableNullSafe
import com.rex.api.internal.skeleton.persistence.jdbc.repository.sub.NoteJdbcRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true, transactionManager = "subTransactionManager")
@Service
class NoteJdbcEntityService(
    private val noteJdbcRepository: NoteJdbcRepository,
) {
    @CacheableNullSafe(value = ["NoteIdList"], key = "#title")
    fun findAllIdsByTitle(title: String): List<Long> =
        noteJdbcRepository.findAllIdsByTitle(title)

    fun countByTitle(title: String): Int =
        noteJdbcRepository.countByTitle(title)
}