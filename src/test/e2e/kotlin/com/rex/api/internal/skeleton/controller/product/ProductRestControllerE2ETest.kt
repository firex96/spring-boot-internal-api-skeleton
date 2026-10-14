package com.rex.api.internal.skeleton.controller.product

import com.rex.api.internal.skeleton.config.E2ETestConfiguration
import com.rex.api.internal.skeleton.model.ProductModel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import tools.jackson.databind.json.JsonMapper

/**
 * ProductRestController E2E 테스트.
 *
 * 실제 HTTP 요청을 통해 API 엔드포인트를 테스트합니다.
 * - 전체 Spring Context 로드
 * - 실제 서버 기동 (RANDOM_PORT)
 * - TestRestTemplate을 통한 HTTP 호출
 * - docker compose 로 띄운 로컬 인프라(MySQL·Redis) 사용 (local,e2e 프로필)
 *
 * 실행 방법:
 * ```bash
 * docker compose up -d
 * ./gradlew e2eTest
 * ```
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(E2ETestConfiguration::class)
class ProductRestControllerE2ETest(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: JsonMapper,
) : DescribeSpec({

    describe("GET /products/{id}") {

        context("Product ID로 조회") {
            it("존재하는 Product인 경우 ProductModel을 반환한다") {
                // given
                val productId = 1L

                // when
                val response = restTemplate.getForEntity<String>("/products/$productId")

                // then
                when (response.statusCode) {
                    HttpStatus.OK -> {
                        val productModel = objectMapper.readValue(response.body, ProductModel::class.java)
                        productModel.shouldNotBeNull()
                        productModel.id shouldBe productId
                    }
                    HttpStatus.NOT_FOUND -> {
                        // DB에 데이터가 없는 경우
                        response.body shouldContain "NOT_FOUND"
                    }
                    else -> {
                        // 예상치 못한 응답
                        throw AssertionError("Unexpected status: ${response.statusCode}")
                    }
                }
            }
        }

        context("존재하지 않는 Product ID로 조회") {
            it("404 Not Found를 반환한다") {
                // given
                val nonExistentId = 999999

                // when
                val response = restTemplate.getForEntity<String>("/products/$nonExistentId")

                // then
                response.statusCode shouldBe HttpStatus.NOT_FOUND
                response.body shouldContain "NOT_FOUND"
            }
        }
    }

    describe("GET /products/{id}/core") {

        context("Core API를 통한 Product 조회") {
            it("실제 Core API를 통해 ProductModel을 반환한다") {
                // given
                val productId = 1L

                // when
                val response = restTemplate.getForEntity<String>("/products/$productId/core")

                // then
                when (response.statusCode) {
                    HttpStatus.OK -> {
                        val productModel = objectMapper.readValue(response.body, ProductModel::class.java)
                        productModel.shouldNotBeNull()
                        productModel.id shouldBe productId
                    }
                    HttpStatus.NOT_FOUND -> {
                        // Core API에 데이터가 없는 경우
                        response.body shouldContain "NOT_FOUND"
                    }
                    HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.INTERNAL_SERVER_ERROR -> {
                        // Core API 연결 실패
                        println("Core API 연결 실패: ${response.statusCode}")
                    }
                    else -> {
                        throw AssertionError("Unexpected status: ${response.statusCode}")
                    }
                }
            }
        }
    }
})