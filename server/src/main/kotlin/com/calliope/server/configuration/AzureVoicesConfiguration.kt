package com.calliope.server.configuration

import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.google.GoogleInternalVoice
import com.calliope.server.model.domain.yandex.YandexKey
import com.calliope.server.service.ai.AwsService
import com.calliope.server.service.ai.GoogleService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import software.amazon.awssdk.services.polly.model.Voice
import java.io.IOException

@Configuration
class AzureVoicesConfiguration {
    private val logger = KotlinLogging.logger {}

    @Value("\${yandex.key}") var yandexKeyStr: String? = null

    @Bean
    @Throws(IOException::class)
    fun azureVoices(mapper: ObjectMapper): List<AzureInternalVoice> {
        val classPathResource = ClassPathResource(AZURE_VOICES_FILENAME)
        val jsonArray = IOUtils.toString(classPathResource.inputStream)

        return mapper.readValue<List<AzureInternalVoice>>(
            jsonArray,
            object : TypeReference<List<AzureInternalVoice>>() {
            }
        )
    }

    @Bean
    @Throws(Exception::class)
    fun googleVoices(googleService: GoogleService): List<GoogleInternalVoice>? {
        val voices = googleService.initVoices()
        logger.info("Google voices {}", voices?.size)
        return voices
    }

    @Bean
    @Throws(Exception::class)
    fun awsVoices(awsService: AwsService): List<Voice> {
        val voices = awsService.initVoices()
        logger.info("AWS voices {}", voices.size)
        return voices
    }

    @Bean
    @Throws(IOException::class)
    fun languageSpeeds(mapper: ObjectMapper): Map<String, LanguageSpeed> {
        val classPathResource = ClassPathResource(LANGUAGE_SPEED_FILENAME)
        val jsonArray = IOUtils.toString(classPathResource.inputStream)

        return mapper.readValue<List<LanguageSpeed>>(
            jsonArray,
            object : TypeReference<List<LanguageSpeed>>() {
            }
        )
            .map { it.code!! to it }
            .toMap()
    }

    @Bean
    @Throws(IOException::class)
    fun languageCode2Name(mapper: ObjectMapper): Map<String, String> {
        val classPathResource = ClassPathResource(LANGUAGE_SPEED_FILENAME)
        val jsonArray = IOUtils.toString(classPathResource.inputStream)

        return mapper.readValue<List<LanguageSpeed>>(
            jsonArray,
            object : TypeReference<List<LanguageSpeed>>() {
            }
        )
            .map { it.code!! to it.language!! }
            .toMap()
    }

    @Bean
    @Throws(IOException::class)
    fun yandexKey(mapper: ObjectMapper): YandexKey {
        logger.info("yandexKye ######################################## {}", yandexKeyStr)
        return mapper.readValue(
            yandexKeyStr,
            object : TypeReference<YandexKey>() {
            }
        )
    }

    companion object {
        private const val AZURE_VOICES_FILENAME = "azure_voice.json"
        private const val LANGUAGE_SPEED_FILENAME = "world_languages_speed.json"
    }
}
