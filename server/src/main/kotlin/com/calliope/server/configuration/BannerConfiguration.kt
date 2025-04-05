package com.calliope.server.configuration

import com.calliope.server.model.domain.yandex.AspectRatio
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.io.IOUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.IOException

@Configuration
class BannerConfiguration {
    @Bean
    @Throws(IOException::class)
    fun aspectRatios(mapper: ObjectMapper): List<AspectRatio> {
        val classPathResource = ClassPathResource(ASPECT_RATIO_FILENAME)
        val jsonArray = IOUtils.toString(classPathResource.inputStream)

        return mapper.readValue<List<AspectRatio>>(
            jsonArray,
            object : TypeReference<List<AspectRatio>>() {
            }
        )
    }

    companion object {
        private const val ASPECT_RATIO_FILENAME = "aspect_ratios.json"
    }
}
