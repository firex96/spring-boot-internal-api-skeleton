package com.rex.api.internal.skeleton.converter

import com.rex.api.internal.skeleton.enums.ProductStatus
import com.rex.api.internal.skeleton.enums.StringEnumUtil

class ProductStatusReadingConverter : AbstractStringEnumReadingConverter<ProductStatus>(StringEnumUtil::fromValue)