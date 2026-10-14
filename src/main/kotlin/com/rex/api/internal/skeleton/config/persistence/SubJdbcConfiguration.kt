package com.rex.api.internal.skeleton.config.persistence

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.convert.DataAccessStrategy
import org.springframework.data.jdbc.core.convert.DataAccessStrategyFactory
import org.springframework.data.jdbc.core.convert.JdbcConverter
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.relational.core.dialect.MySqlDialect
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

/**
 * sub 스키마 JDBC 설정.
 *
 * primary 가 아닌 데이터소스라 `jdbcOperationsRef`·`dataAccessStrategyRef` 를 명시한다 — 빠지면 Spring Data JDBC
 * 리포지토리가 @Primary(main) 연산자에 붙어 main 스키마를 조회한다. 직접 주입할 때도 한정자를 붙인다.
 */
@Configuration
@EnableJdbcRepositories(
    basePackages = [
        "com.rex.api.internal.skeleton.persistence.jdbc.repository.sub",
    ],
    jdbcOperationsRef = "subNamedParameterJdbcOperations",
    dataAccessStrategyRef = "subDataAccessStrategy",
    transactionManagerRef = "subTransactionManager",
)
class SubJdbcConfiguration {
    @Bean(defaultCandidate = false)
    fun subDataAccessStrategy(
        jdbcConverter: JdbcConverter,
        @Qualifier("subNamedParameterJdbcOperations") operations: NamedParameterJdbcOperations,
    ): DataAccessStrategy = DataAccessStrategyFactory(
        jdbcConverter,
        operations,
        MySqlDialect.INSTANCE,
        QueryMappingConfiguration.EMPTY,
    ).create()

    @Bean
    fun subJdbcTemplate(
        @Qualifier("subDataSource") dataSource: DataSource,
    ): JdbcTemplate = JdbcTemplate(dataSource)

    @Bean
    fun subNamedParameterJdbcOperations(
        @Qualifier("subDataSource") dataSource: DataSource,
    ): NamedParameterJdbcOperations = NamedParameterJdbcTemplate(dataSource)
}