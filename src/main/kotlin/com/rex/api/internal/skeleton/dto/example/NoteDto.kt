package com.rex.api.internal.skeleton.dto.example

import java.time.LocalDateTime

data class NoteDto(
    val id: Long,
    val title: String,
    val content: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)