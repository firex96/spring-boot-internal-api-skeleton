package com.rex.api.internal.skeleton.service.mapping

import com.rex.api.internal.skeleton.enums.ErrorCode
import com.rex.api.internal.skeleton.exception.ItemNotFoundException
import com.rex.api.internal.skeleton.mapper.ProductMapper
import com.rex.api.internal.skeleton.model.ProductModel
import com.rex.api.internal.skeleton.service.biz.ProductBizService
import org.springframework.stereotype.Service

@Service
class ProductMappingService(
    private val bizService: ProductBizService,
    private val mapper: ProductMapper,
) {
    fun getProduct(id: Long): ProductModel =
        bizService.find(id)?.let {
            mapper.toModel(it)
        } ?: throw ItemNotFoundException(ErrorCode.NOT_FOUND_ERROR.divisionCode, "Product with id $id not found")
}