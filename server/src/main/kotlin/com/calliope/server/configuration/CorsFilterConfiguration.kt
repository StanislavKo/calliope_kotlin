package com.calliope.server.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.function.Consumer
import mu.KotlinLogging

@Configuration
class CorsFilterConfiguration {
    private val logger = KotlinLogging.logger {}

    @Value("#{'\${CORS_ALLOWED_METHODS:*}'.split('\\s*,\\s*')}")
    private val allowedMethods: Set<String> = HashSet(setOf("*"))

    @Value("#{'\${CORS_ALLOWED_HEADERS:*}'.split('\\s*,\\s*')}")
    private val allowedHeaders: Set<String> = HashSet(setOf("*"))

    @Value("\${CORS_ALLOW_CREDENTIALS:true}")
    private val allowCredentials = true

    @Value("\${CORS_PATH:/**}")
    private val path = "/**"

    @Bean
    fun corsFilter(): CorsFilter {
        val allowedOrigins: MutableSet<String> = HashSet()
        allowedOrigins.add("http://localhost:3000")
        allowedOrigins.add("http://localhost:4200")

        val source = UrlBasedCorsConfigurationSource()

        val config = CorsConfiguration()
        config.allowCredentials = allowCredentials
        allowedOrigins.forEach(Consumer { origin: String? -> config.addAllowedOrigin(origin) })
        allowedMethods.forEach(Consumer { method: String? -> config.addAllowedMethod(method) })
        allowedHeaders.forEach(Consumer { allowedHeader: String? -> config.addAllowedHeader(allowedHeader) })
        logger.info("CORS config: {}", toString(config))

        source.registerCorsConfiguration(path, config)
        return CorsFilter(source)
    }

    private fun toString(config: CorsConfiguration): String {
        return "CorsConfiguration(" +
                "allowCredentials=" + config.allowCredentials +
                ", allowedOrigins=" + config.allowedOrigins +
                ", allowedMethods=" + config.allowedMethods +
                ", allowedHeaders=" + config.allowedHeaders +
                ")"
    }

    companion object {
        fun isValidURL(url: String): Boolean {
            try {
                URL(url).toURI()
                return true
            } catch (e: MalformedURLException) {
                return false
            } catch (e: URISyntaxException) {
                return false
            }
        }
    }
}
