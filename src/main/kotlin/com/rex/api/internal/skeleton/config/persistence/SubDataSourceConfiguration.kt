package com.rex.api.internal.skeleton.config.persistence

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

@Configuration
class SubDataSourceConfiguration {
    @Bean("subWriteDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.sub.write")
    fun subWriteDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean("subReadDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.sub.read")
    fun subReadDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean("subWriteDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.sub.write.hikari")
    fun subWriteDataSource(
        @Qualifier("subWriteDataSourceProperties") properties: DataSourceProperties,
    ): DataSource = properties.initializeDataSourceBuilder().build()

    @Bean("subReadDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.sub.read.hikari")
    fun subReadDataSource(
        @Qualifier("subReadDataSourceProperties") properties: DataSourceProperties,
    ): DataSource = properties.initializeDataSourceBuilder().build()

    @Bean("subDataSource")
    fun subDataSource(
        @Qualifier("subWriteDataSource") writeDataSource: DataSource,
        @Qualifier("subReadDataSource") readDataSource: DataSource,
    ): DataSource =
        LazyConnectionDataSourceProxy(writeDataSource).apply {
            setReadOnlyDataSource(readDataSource)
        }
}