package com.rex.api.internal.skeleton.converter

import com.rex.api.internal.skeleton.enums.ProductStatus
import com.rex.api.internal.skeleton.enums.StringEnumUtil

class ProductStatusConverter : AbstractStringEnumConverter<ProductStatus>(nullable = false, fromValue = StringEnumUtil::fromValue)