package com.calliope.server.service.ai

import com.calliope.server.model.domain.openai.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.function.Consumer
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value

@Service
class OpenaiService(
    private val mapper: ObjectMapper,
    private val restOperations: RestOperations
) {
    private val logger = KotlinLogging.logger {}

    @Value("\${openai.lambda-url}") var assistantMyUrl: String? = null

    fun generateAdText(
        description: String?,
        locale: String?,
        language: String?,
        characters: Int
    ): String? {
        var hint: String? = null
        //        alpha-numeric
        hint = if (language != null) {
            "Please provide the text of exactly $characters $language characters. Resulting text should be in $locale language. It's strict constraint to create requested length of the text."
        } else {
            "Please provide the text of exactly $characters characters. Resulting text should be in $locale language. It's strict constraint to create requested length of the text."
        }
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_AD_TEXT_ID,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            hint
        )
        try {
            logger.debug(
                "OPENAI_INVOKE generateAdText(), answer = {}, description = {}",
                answer,
                description
            )
            val adText = ObjectMapper().readValue(answer, OpenaiAnswerAdText::class.java)

            return adText.advertisementText
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI Ad text assistant answer", e)
        }
        return null
    }

    fun generateImageDescription(
        description: String?
    ): String? {
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_IMAGE_DESCRIPTION_ID,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            null
        )
        try {
            logger.debug(
                "OPENAI_INVOKE generateImageDescription(), answer = {}, description = {}",
                answer,
                description
            )
            val imageDesc = ObjectMapper().readValue(
                answer,
                OpenaiAnswerImageDescription::class.java
            )

            return imageDesc.entitiesAndCharacteristics
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI Image description assistant answer", e)
        }
        return null
    }

    fun generateMusicGenTask(
        description: String?
    ): OpenaiMusicGenTask? {
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_MUSICGEN_TASK_ID,  //                ASSISTANT_BEARER,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            null
        )
        try {
            logger.debug(
                "OPENAI_INVOKE generateMusicGenTask(), answer = {}, description = {}",
                answer,
                description
            )
            val task = ObjectMapper().readValue(answer, OpenaiMusicGenTask::class.java)

            return task
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI MusicGen task assistant answer", e)
        }
        return null
    }

    fun generateGender(
        description: String?
    ): OpenaiGender? {
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_GENDER_ID,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            null
        )
        try {
            logger.debug(
                "OPENAI_INVOKE generateGender(), answer = {}, description = {}",
                answer,
                description
            )
            val task = ObjectMapper().readValue(answer, OpenaiGender::class.java)

            return task
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI Gender task assistant answer", e)
        }
        return null
    }

    fun generateLocale(
        description: String?
    ): OpenaiLocale? {
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_LOCALE_ID,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            null
        )
        try {
            logger.debug(
                "OPENAI_INVOKE generateLocale(), answer = {}, description = {}",
                answer,
                description
            )
            val task = ObjectMapper().readValue(answer, OpenaiLocale::class.java)

            return task
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI Locale task assistant answer", e)
        }
        return null
    }

    fun generateSummary(
        description: String?
    ): OpenaiSummary? {
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_TITLE_ID,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            null
        )
        try {
            logger.debug(
                "OPENAI_INVOKE generateSummary(), answer = {}, description = {}",
                answer,
                description
            )
            val task = ObjectMapper().readValue(answer, OpenaiSummary::class.java)

            return task
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI Summary task assistant answer", e)
        }
        return null
    }

    fun generateMood(
        description: String?
    ): OpenaiMood? {
        val answer = invokeOpenAIAssistant(
            true,
            ASSISTANT_MOOD_ID,
            assistantMyUrl!!,
            null,
            { newOpenaiThreadIdBudget: String? -> },
            description,
            null
        )
        try {
            logger.debug("OPENAI_INVOKE generateMood(), answer = {}, description = {}", answer, description)
            val task = ObjectMapper().readValue(answer, OpenaiMood::class.java)

            return task
        } catch (e: Exception) {
            logger.error("Can't parse OpenAI Mood task assistant answer", e)
        }
        return null
    }

    fun invokeOpenAIAssistant(
        isLocal: Boolean,
        assistantId: String?,
        assistantUrl: String,
        openaiThreadId: String?,
        saveThreadId: Consumer<String?>?,
        question: String?,
        hint: String?
    ): String? {
        var result: String? = null
        for (i in 0..0) {
            result = invokeOpenAIAssistantLocal(assistantUrl, assistantId, question, hint)
            if (result != null) {
                break
            }
        }
        return result
    }

    fun invokeOpenAIAssistantLocal(
        assistantUrl: String,
        assistantId: String?,
        question: String?,
        hint: String?
    ): String? {
        try {
            val builder = UriComponentsBuilder.fromUri(URI.create(assistantUrl))
            val headers = HttpHeaders()
            headers["Content-Type"] = "application/json; charset=UTF-8"
            val entity: HttpEntity<OpenaiInternalMySendMessage> = HttpEntity(
                OpenaiInternalMySendMessage(assistantId, question, hint, null),
                headers
            )

            val response1 = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<OpenaiInternalMyAnswer?>() {
                }
            )
            logger.info("#################### answer = {}", response1.body?.answer)
            return response1.body?.answer
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to get OpenAI suggestion. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to get OpenAI suggestion. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to get OpenAI suggestion with error : {}", e.message, e)
        }
        return null
    }

    companion object {
        private const val ASSISTANT_AD_TEXT_ID = "asst_1234"
        private const val ASSISTANT_AD_TEXT_10_ID = "asst_1234"
        private const val ASSISTANT_IMAGE_DESCRIPTION_ID = "asst_1234"
        private const val ASSISTANT_MUSICGEN_TASK_ID = "asst_1234"
        private const val ASSISTANT_GENDER_ID = "asst_1234"
        private const val ASSISTANT_LOCALE_ID = "asst_1234"
        private const val ASSISTANT_TITLE_ID = "asst_1234"
        private const val ASSISTANT_MOOD_ID = "asst_1234"
    }
}
