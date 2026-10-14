package com.rex.api.internal.skeleton.config.persistence

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = [
        "com.rex.api.internal.skeleton.persistence.jpa.repository.sub",
    ],
    entityManagerFactoryRef = "subEntityManagerFactory",
    transactionManagerRef = "subTransactionManager",
)
class SubJpaConfiguration {
    @Bean
    fun subEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("subDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(dataSource)
            .packages("com.rex.api.internal.skeleton.persistence.jpa.entity.sub")
            .persistenceUnit("sub")
            .properties(mapOf("hibernate.hbm2ddl.auto" to "none"))
            .build()

    @Bean
    fun subTransactionManager(
        @Qualifier("subEntityManagerFactory") entityManagerFactory: EntityManagerFactory?,
    ): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory
        return transactionManager
    }
}