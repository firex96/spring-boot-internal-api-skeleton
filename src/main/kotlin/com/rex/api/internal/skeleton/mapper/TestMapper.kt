package com.rex.api.internal.skeleton.mapper

import com.rex.api.internal.skeleton.config.MapstructConfiguration
import com.rex.api.internal.skeleton.dto.TestDto
import com.rex.api.internal.skeleton.persistence.jpa.entity.main.TestJpaEntity
import org.mapstruct.Mapper

@Mapper(config = MapstructConfiguration::class)
interface TestMapper {
    fun toDto(entity: TestJpaEntity): TestDto

    fun toDtoList(entityList: List<TestJpaEntity>): List<TestDto>

    fun toEntity(dto: TestDto): TestJpaEntity
}