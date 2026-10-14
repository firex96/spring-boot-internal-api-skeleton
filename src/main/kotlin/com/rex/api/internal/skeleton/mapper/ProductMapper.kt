package com.rex.api.internal.skeleton.mapper

import com.rex.api.internal.skeleton.client.core.response.ProductResponse
import com.rex.api.internal.skeleton.config.MapstructConfiguration
import com.rex.api.internal.skeleton.dto.ProductDto
import com.rex.api.internal.skeleton.model.ProductModel
import com.rex.api.internal.skeleton.persistence.jdbc.entity.main.ProductJdbcEntity
import com.rex.api.internal.skeleton.persistence.jpa.entity.main.ProductJpaEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = MapstructConfiguration::class)
interface ProductMapper {
    fun toDto(entity: ProductJpaEntity): ProductDto

    fun toDto(entity: ProductJdbcEntity): ProductDto

    fun toModel(dto: ProductDto): ProductModel

    // ProductModel 에는 감사 타임스탬프가 없다 — DTO 는 non-null(NOT NULL 컬럼)이라 자리표시자를 넣고 persist 시 감사 리스너가 덮는다
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    // 감사 주체도 Model 에 없다 — non-null Long 이라 ignore 대신 시스템 사용자 자리표시자(persist 시 감사 리스너가 덮는다)
    @Mapping(target = "createdBy", expression = "java(com.rex.api.internal.skeleton.constant.Constant.SYSTEM_USER_ID)")
    @Mapping(target = "updatedBy", expression = "java(com.rex.api.internal.skeleton.constant.Constant.SYSTEM_USER_ID)")
    fun toDto(model: ProductModel): ProductDto

    fun toEntity(dto: ProductDto): ProductJpaEntity

    fun toDtoList(entityList: List<ProductJpaEntity>): List<ProductDto>

    fun toModelList(dtoList: List<ProductDto>): List<ProductModel>

    @Mapping(source = "productName", target = "name")
    fun toModel(response: ProductResponse): ProductModel
}