package com.rex.api.internal.skeleton.persistence

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.containers.MySQLContainer

/**
 * 업무 스키마 수동 DDL(`db/manual/create-*.sql`)은 운영에서 사람이 다시 실행할 수 있다 — 재실행이 기존 행을 지우거나
 * 다른 테이블을 만들지 않는지 확인한다.
 */
class ManualBusinessSchemaIntegrationTest : StringSpec({
    fun rerunPreservesRows(
        script: String,
        insertSql: String,
        selectSql: String,
        expectedTableCount: Long,
    ) {
        val mysql = MySQLContainer("mysql:8.0").withDatabaseName("skeleton").withInitScript(script)
        mysql.start()
        try {
            val dataSource = DriverManagerDataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            val jdbc = JdbcTemplate(dataSource)
            jdbc.update(insertSql)
            dataSource.connection.use {
                ScriptUtils.executeSqlScript(it, ClassPathResource(script))
            }
            jdbc.queryForObject(selectSql, String::class.java) shouldBe "existing"
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",
                Long::class.java,
            ) shouldBe expectedTableCount
        } finally {
            mysql.stop()
        }
    }

    "수동 main 생성 SQL 재실행은 기존 행을 보존하고 다른 테이블을 만들지 않는다" {
        rerunPreservesRows(
            script = "db/manual/create-main.sql",
            insertSql =
            "INSERT INTO product(status, name, description, price, created_by, created_at, updated_by, updated_at) " +
                "VALUES ('OnSale', 'preserved', 'existing', 1000, 0, NOW(6), 0, NOW(6))",
            selectSql = "SELECT description FROM product WHERE name='preserved'",
            expectedTableCount = 3L,
        )
    }

    "수동 sub 생성 SQL 재실행은 기존 행을 보존하고 다른 테이블을 만들지 않는다" {
        rerunPreservesRows(
            script = "db/manual/create-sub.sql",
            insertSql = "INSERT INTO note(title, content) VALUES ('preserved', 'existing')",
            selectSql = "SELECT content FROM note WHERE title='preserved'",
            expectedTableCount = 1L,
        )
    }
})