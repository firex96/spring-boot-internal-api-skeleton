package com.rex.api.internal.skeleton.mapper

import com.rex.api.internal.skeleton.config.MapstructConfiguration
import com.rex.api.internal.skeleton.dto.example.NoteDto
import com.rex.api.internal.skeleton.persistence.jpa.entity.sub.NoteJpaEntity
import org.mapstruct.Mapper

@Mapper(config = MapstructConfiguration::class)
interface NoteMapper {
    fun toDto(entity: NoteJpaEntity): NoteDto

    fun toDtoList(entityList: List<NoteJpaEntity>): List<NoteDto>

    fun toEntity(dto: NoteDto): NoteJpaEntity
}