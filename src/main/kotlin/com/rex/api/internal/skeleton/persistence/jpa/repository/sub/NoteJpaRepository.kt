package com.rex.api.internal.skeleton.persistence.jpa.repository.sub

import com.rex.api.internal.skeleton.persistence.jpa.entity.sub.NoteJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteJpaRepository : JpaRepository<NoteJpaEntity, Long>