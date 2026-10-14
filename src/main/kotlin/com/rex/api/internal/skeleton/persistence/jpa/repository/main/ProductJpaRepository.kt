package com.rex.api.internal.skeleton.persistence.jpa.repository.main

import com.rex.api.internal.skeleton.persistence.jpa.entity.main.ProductJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductJpaRepository : JpaRepository<ProductJpaEntity, Long> {
    fun getAllByCategoryId(categoryId: Long): List<ProductJpaEntity>
}