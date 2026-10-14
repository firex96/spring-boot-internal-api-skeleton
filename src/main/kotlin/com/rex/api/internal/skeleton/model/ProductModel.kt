package com.rex.api.internal.skeleton.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.rex.api.internal.skeleton.enums.ProductStatus

data class ProductModel(
    @field:JsonProperty("id")
    val id: Long,
    @field:JsonProperty("status")
    val status: ProductStatus,
    @field:JsonProperty("name")
    val name: String,
    @field:JsonProperty("description")
    val description: String?,
    @field:JsonProperty("price")
    val price: Long,
    @field:JsonProperty("categoryId")
    val categoryId: Long?,
)