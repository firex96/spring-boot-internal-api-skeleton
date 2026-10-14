package com.rex.api.internal.skeleton.persistence.jpa.entity.sub

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "note")
class NoteJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "content", nullable = true, columnDefinition = "TEXT")
    var content: String?,

    @Column(name = "created_at", nullable = true)
    var createdAt: LocalDateTime?,

    @Column(name = "updated_at", nullable = true)
    var updatedAt: LocalDateTime?,
)