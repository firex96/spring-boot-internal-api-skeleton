package com.rex.api.internal.skeleton.config.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.client.destinations")
class ClientDestinations(
    val core: ClientInfo,
) {
    class ClientInfo(
        val url: String,
    )
}