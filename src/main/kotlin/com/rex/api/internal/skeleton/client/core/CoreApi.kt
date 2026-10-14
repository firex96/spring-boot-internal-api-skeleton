package com.rex.api.internal.skeleton.client.core

import com.rex.api.internal.skeleton.client.core.response.ProductResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface CoreApi {
    @GetExchange("/products/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ProductResponse
}