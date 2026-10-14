package com.rex.api.internal.skeleton.controller.product

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeKotlinBuilder
import com.ninjasquad.springmockk.MockkBean
import com.rex.api.internal.skeleton.client.core.CoreApi
import com.rex.api.internal.skeleton.client.core.response.ProductResponse
import com.rex.api.internal.skeleton.config.IntegrationTestConfiguration
import com.rex.api.internal.skeleton.config.TestcontainersConfiguration
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@ContextConfiguration(initializers = [TestcontainersConfiguration.Initializer::class])
@ActiveProfiles("testcontainers")
@Import(IntegrationTestConfiguration::class)
@AutoConfigureMockMvc
class ProductRestControllerTest(
    private val mockMvc: MockMvc,
) : BehaviorSpec() {
    @MockkBean
    lateinit var coreApi: CoreApi

    init {
        Given("Product REST Controller") {
            val fixtureMonkey = FixtureMonkey.builder().plugin(KotlinPlugin()).build()
            val productResponse = fixtureMonkey.giveMeKotlinBuilder<ProductResponse>().set(ProductResponse::id, 2).sample()

            every { coreApi.getProduct(any()) } returns productResponse
            When("Core API is called to get product details") {
                Then("Product details should be returned from Core API") {
                    mockMvc.get("/products/2/core") {
                        accept = MediaType.APPLICATION_JSON
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.id") { value(2) }
                    }
                }
            }
        }
    }
}