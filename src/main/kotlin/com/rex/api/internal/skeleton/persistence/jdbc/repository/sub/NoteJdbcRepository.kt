package com.rex.api.internal.skeleton.persistence.jdbc.repository.sub

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository

@Repository
class NoteJdbcRepository(
    @param:Qualifier("subNamedParameterJdbcOperations")
    private val namedParameterJdbcOperations: NamedParameterJdbcOperations,
) {
    fun findAllIdsByTitle(title: String): List<Long> {
        val namedParameters: SqlParameterSource = MapSqlParameterSource(
            "TITLE",
            title,
        )

        return namedParameterJdbcOperations.queryForList(
            """
SELECT  id
FROM    note
WHERE   title = :TITLE
;
            """.trimIndent(),
            namedParameters,
            Long::class.javaObjectType,
        ).filterNotNull()
    }

    fun countByTitle(title: String): Int {
        val namedParameters: SqlParameterSource = MapSqlParameterSource(
            "TITLE",
            title,
        )

        return namedParameterJdbcOperations.queryForObject(
            """
SELECT  COUNT(*)
FROM    note
WHERE   title = :TITLE
;
            """.trimIndent(),
            namedParameters,
            Int::class.javaObjectType,
        ) ?: 0
    }
}