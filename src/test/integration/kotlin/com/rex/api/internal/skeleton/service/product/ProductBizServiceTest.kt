package com.rex.api.internal.skeleton.service.product

import com.ninjasquad.springmockk.MockkBean
import com.rex.api.internal.skeleton.config.IntegrationTestConfiguration
import com.rex.api.internal.skeleton.config.TestcontainersConfiguration
import com.rex.api.internal.skeleton.context.product.ProductBuilder
import com.rex.api.internal.skeleton.dto.ProductDto
import com.rex.api.internal.skeleton.persistence.jdbc.service.main.ProductJdbcEntityService
import com.rex.api.internal.skeleton.service.biz.ProductBizService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ContextConfiguration(initializers = [TestcontainersConfiguration.Initializer::class])
@ActiveProfiles("testcontainers")
@Import(IntegrationTestConfiguration::class)
@Transactional
class ProductBizServiceTest(
    private val service: ProductBizService,
) : BehaviorSpec() {
    @MockkBean
    lateinit var productJdbcEntityService: ProductJdbcEntityService

    init {
        Context("Product CRUD") {
            val product = ProductBuilder.productDtoBuilder().set(ProductDto::id, null).sample()

            Given("New Product") {
                When("Product Created") {
                    val createdProduct = service.add(product)
                    Then("Product should be created successfully with all fields") {
                        createdProduct.shouldBeEqualToIgnoringFields(
                            product,
                            ProductDto::id,
                            ProductDto::createdAt,
                            ProductDto::createdBy,
                            ProductDto::updatedAt,
                            ProductDto::updatedBy,
                        )
                    }
                }
            }

            Given("Existing Product") {
                val existingProduct = service.add(product)

                When("Product Updated") {
                    val updatedDto =
                        existingProduct.copy(
                            description = "Updated",
                            name = "Updated name",
                            price = 20_000L,
                        )
                    val updatedProduct = service.modify(updatedDto)
                    Then("Product should be updated successfully") {
                        updatedProduct.description shouldBe updatedDto.description
                        updatedProduct.name shouldBe updatedDto.name
                        updatedProduct.price shouldBe updatedDto.price
                    }
                }

                When("Product Deleted") {
                    service.remove(existingProduct.id)
                    Then("Product should not be found after deletion") {
                        service.find(existingProduct.id) shouldBe null
                    }
                }
            }
        }

        Context("Product by Category") {
            Given("Category with active products") {
                val categoryId = 100L
                val expectedProductIds = listOf(1L, 2L, 3L, 4L, 5L)

                every { productJdbcEntityService.getAllProductIdByCategoryId(categoryId) } returns expectedProductIds

                When("Finding all product IDs by category ID") {
                    val result = service.findAllProductIdByCategoryId(categoryId)

                    Then("Should return all active product IDs in the category") {
                        result shouldContainExactly expectedProductIds
                    }
                }
            }

            Given("Category with no products") {
                val categoryId = 999L

                every { productJdbcEntityService.getAllProductIdByCategoryId(categoryId) } returns emptyList()

                When("Finding all product IDs by category ID") {
                    val result = service.findAllProductIdByCategoryId(categoryId)

                    Then("Should return empty list") {
                        result.shouldBeEmpty()
                    }
                }
            }
        }
    }
}