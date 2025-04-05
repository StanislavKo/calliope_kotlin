package com.calliope.server.service.ai

import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.azure.AzureInternalVoiceTag
import com.calliope.server.model.form.AdGenerateForm
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Lazy
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*
import kotlin.math.floor
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value

@Service
class AzureService(
    private val mapper: ObjectMapper,
    private val restOperations: RestOperations,
    @param:Lazy private val azureVoices: List<AzureInternalVoice?>
) : VoiceGeneratorService {
    private val logger = KotlinLogging.logger {}

    @Value("\${azure.subscription-key}") var subscriptionKey: String? = null

    fun initVoices(
    ): List<AzureInternalVoice?>? {
        val voices = initVoicesImpl()
        return voices
    }

    fun isApplicable(
        form: AdGenerateForm
    ): Boolean {
        return azureVoices
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.gender) || form.gender.equals(voice?.gender)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.language) || form.language.equals(voice?.locale) 
                        || Optional.ofNullable<List<String>?>(voice?.secondaryLocaleList).orElse(listOf()).contains(form.language)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.style) || Optional.ofNullable<List<String>?>(voice?.styles).orElse(listOf()).contains(form.style)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.scenary) || Optional.ofNullable<AzureInternalVoiceTag>(voice?.voiceTag)
                    .map { it.tailoredScenarios }.orElse(listOf())!!.contains(form.scenary)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.personality) || Optional.ofNullable<AzureInternalVoiceTag>(voice?.voiceTag)
                    .map { it.voicePersonalities }.orElse(listOf())!!.contains(form.personality)
            }
            .count() > 0
    }

    override fun getProvider(
    ): String {
        return "azure"
    }

    override fun getName(
        form: AdGenerateForm
    ): String? {
        val azureVoices2: MutableList<AzureInternalVoice?> = mutableListOf(*azureVoices.toTypedArray())
        azureVoices2.shuffle()
        val selectedVoice: String? = azureVoices2
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.gender) || form.gender.equals(voice?.gender)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.language) || form.language.equals(voice?.locale)
                        || Optional.ofNullable<List<String>?>(voice?.secondaryLocaleList).orElse(listOf())!!.contains(form.language)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.style) || Optional.ofNullable<List<String>?>(voice?.styles).orElse(listOf()).contains(form.style)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.scenary) || Optional.ofNullable<AzureInternalVoiceTag?>(voice?.voiceTag)
                    .map { it.tailoredScenarios }.orElse(listOf())!!.contains(form.scenary)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.personality) || Optional.ofNullable<AzureInternalVoiceTag?>(voice?.voiceTag)
                    .map { it.voicePersonalities }.orElse(listOf())!!.contains(form.personality)
            }
            .map { it?.shortName }
            .firstOrNull()
        return selectedVoice
    }

    override fun getHardcodeName(
        form: AdGenerateForm,
        selectedVoice: String
    ): String? {
        var gender1 = ""
        when (form.hardcodeVoice) {
            "anotherMale" -> gender1 = "Male"
            "anotherFemale" -> gender1 = "Female"
        }
        val gender = gender1
        val selectedHardcodeVoice: String? = azureVoices
            .filter { voice: AzureInternalVoice? -> StringUtils.isEmpty(gender) /* same voice */ || selectedVoice != voice?.shortName }
            .filter { voice: AzureInternalVoice? -> StringUtils.isEmpty(gender) || gender == voice?.gender }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.language) || form.language.equals(voice?.locale)
                        || Optional.ofNullable<List<String>?>(voice?.secondaryLocaleList).orElse(listOf()).contains(form.language)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.style) || Optional.ofNullable<List<String>?>(voice?.styles).orElse(listOf()).contains(form.style)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.scenary) || Optional.ofNullable<AzureInternalVoiceTag?>(voice?.voiceTag)
                    .map { it.tailoredScenarios }.orElse(listOf())!!.contains(form.scenary)
            }
            .filter { voice: AzureInternalVoice? ->
                StringUtils.isEmpty(form.personality) || Optional.ofNullable<AzureInternalVoiceTag?>(voice?.voiceTag)
                    .map { it.voicePersonalities }.orElse(listOf())!!.contains(form.personality)
            }
            .map { it?.shortName }
            .firstOrNull()
        logger.info("getHardcodeName {}", selectedHardcodeVoice)
        return selectedHardcodeVoice
    }

    fun generateVoice(
        adText: String,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        isHardcode: Boolean
    ): ByteArray? {
        val token = auth()
        val voice = generateVoiceImpl(token, adText, voiceRate, form, config, isHardcode)
        return voice
    }

    fun generateVoiceImpl(
        token: String?,
        adText: String,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        isHardcode: Boolean
    ): ByteArray? {
        var selectedVoice: String? = if (isHardcode) config.hardcodeVoiceName else config.voiceName
        val rate = Math.round(floor((120f * voiceRate - 100).toDouble()))

        try {
            val builder =
                UriComponentsBuilder.fromUri(URI.create("https://uksouth.tts.speech.microsoft.com/cognitiveservices/v1"))
            val headers = HttpHeaders()
            headers["X-Microsoft-OutputFormat"] = "riff-48khz-16bit-mono-pcm"
            headers["Content-Type"] = "application/ssml+xml"
            headers["Authorization"] = "Bearer $token"
            // <prosody> and <voice> can contain each other - https://www.w3.org/TR/speech-synthesis11/#S3.2.4
            val entity = HttpEntity<String>(
                """<speak xmlns:mstts="http://www.w3.org/2001/mstts" version='1.0' xml:lang='${form.language}'>
<voice xml:lang='${form.language}' xml:gender='${form.gender}' name='$selectedVoice'>
<prosody rate="$rate%">
$adText</prosody>
</voice></speak>""",
                headers
            )
            logger.info("Azure entity = {}", entity.body)

            val response1 = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<ByteArray?>() {
                }
            )
            return response1.body
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to generate Azure speech. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to generate Azure speech. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to generate Azure speech with error : {}", e.message, e)
        }
        return null
    }

    fun initVoicesImpl(): List<AzureInternalVoice?>? {
        try {
            val builder =
                UriComponentsBuilder.fromUri(URI.create("https://uksouth.tts.speech.microsoft.com/cognitiveservices/voices/list"))
            val headers = HttpHeaders()
            headers["Ocp-Apim-Subscription-Key"] = subscriptionKey
            val entity = HttpEntity(
                "",
                headers
            )

            val response1: ResponseEntity<List<AzureInternalVoice?>?> = restOperations.exchange<List<AzureInternalVoice?>?>(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                object : ParameterizedTypeReference<List<AzureInternalVoice?>?>() {
                }
            )
            return response1.body
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to get Azure voices. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to get Azure voices. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to get Azure voices with error : {}", e.message, e)
        }
        return null
    }

    fun auth(
    ): String? {
        try {
            val builder =
                UriComponentsBuilder.fromUri(URI.create("https://uksouth.api.cognitive.microsoft.com/sts/v1.0/issueToken"))
            val headers = HttpHeaders()
            headers["Ocp-Apim-Subscription-Key"] = subscriptionKey
            headers["Content-Type"] = "application/x-www-form-urlencoded"
            headers["Content-Length"] = "0"
            val entity = HttpEntity(
                "",
                headers
            )

            val response1 = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<String?>() {
                }
            )
            logger.info("#################### Azure token = {}", response1.body)
            return response1.body
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to get Azure token. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to get Azure token. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to get Azure token with error : {}", e.message, e)
        }
        return null
    }

}
