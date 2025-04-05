package com.calliope.server.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.polly.PollyClient
import mu.KotlinLogging

@Configuration
class PollyConfig {
    private val logger = KotlinLogging.logger {}

    @Value("\${aws.region}") var region: String? = null

    @Value("\${aws.access-key}") var accessKey: String? = null

    @Value("\${aws.secret-key}") var secretKey: String? = null

    @Bean
    fun amazonPollyclient(): PollyClient {
        val client = PollyClient.builder().region(Region.of(region)).credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    accessKey,
                    secretKey
                )
            )
        ).build()
        println("#################")
        println("PollyClient client = $client")
        return client
    }
}
