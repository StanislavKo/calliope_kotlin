package com.calliope.server.service.ai

import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.google.*
import com.calliope.server.model.form.AdGenerateForm
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.services.speech.v1.SpeechScopes
import com.google.auth.oauth2.ServiceAccountCredentials
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Lazy
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value

@Service
class GoogleService(
    private val mapper: ObjectMapper,
    private val restOperations: RestOperations,
    @param:Lazy private val googleVoices: MutableList<GoogleInternalVoice>
) : VoiceGeneratorService {
    private val logger = KotlinLogging.logger {}

    @Value("\${google.key}") var googleKey: String? = null

    @Value("\${google.project}") var googleProject: String? = null

    @get:Throws(Exception::class)
    val token: String
        get() {
            val scopes: MutableList<String> = mutableListOf()
            scopes.add(SpeechScopes.CLOUD_PLATFORM)
            val credentials = ServiceAccountCredentials.fromStream(ByteArrayInputStream(googleKey!!.toByteArray())).createScoped(scopes)
            credentials.refreshIfExpired()
            val token = credentials.accessToken
            logger.info("Google token = " + token.tokenValue)

            return token.tokenValue
        }

    @Throws(Exception::class)
    fun initVoices(
    ): List<GoogleInternalVoice>? {
        val token = token
        val voices = initVoicesImpl(token)
        return voices
    }

    fun isApplicable(
        form: AdGenerateForm
    ): Boolean {
        return googleVoices
            .filter { voice: GoogleInternalVoice -> StringUtils.isEmpty(form.style) }
            .filter { voice: GoogleInternalVoice ->
                StringUtils.isEmpty(form.gender) || form.gender?.uppercase().equals(voice.ssmlGender)
            }
            .filter { voice: GoogleInternalVoice ->
                StringUtils.isEmpty(form.language) || Optional.ofNullable<List<String>?>(voice.languageCodes).orElse(listOf<String>()).contains(form.language)
            }
            .isNotEmpty()
    }

    override fun getProvider(
    ): String {
        return "google"
    }

    override fun getName(
        form: AdGenerateForm
    ): String? {
        val googleVoices2: MutableList<GoogleInternalVoice> = mutableListOf(*googleVoices.toTypedArray())
        googleVoices2.shuffle()
        val selectedVoice: String? = googleVoices2
            .filter { voice: GoogleInternalVoice ->
                StringUtils.isEmpty(form.gender) || form.gender?.uppercase().equals(voice.ssmlGender)
            }
            .filter { voice: GoogleInternalVoice? ->
                StringUtils.isEmpty(form.language) || Optional.ofNullable<List<String>?>(voice?.languageCodes).orElse(listOf<String>()).contains(form.language)
            }
            .map{it.name}
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
        val selectedHardcodeVoice: String? = googleVoices
            .filter { voice: GoogleInternalVoice? -> StringUtils.isEmpty(gender) /* same voice */ || selectedVoice != voice?.name }
            .filter { voice: GoogleInternalVoice? -> StringUtils.isEmpty(gender) || gender.uppercase(Locale.getDefault()) == voice?.ssmlGender }
            .filter { voice: GoogleInternalVoice? ->
                StringUtils.isEmpty(form.language) || Optional.ofNullable<List<String>?>(voice?.languageCodes).orElse(listOf()).contains(form.language)
            }
            .map {it.name}
            .firstOrNull()
        logger.info("getHardcodeName {}", selectedHardcodeVoice)
        return selectedHardcodeVoice
    }

    fun generateVoice(
        adText: String?,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        isHardcode: Boolean,
        token: String
    ): ByteArray? {
        val voice = generateVoiceImpl(token, adText, voiceRate, form, config, isHardcode)
        return voice
    }

    fun generateVoiceImpl(
        token: String,
        adText: String?,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        isHardcode: Boolean
    ): ByteArray? {
        var selectedVoice: String? = if (isHardcode) config.hardcodeVoiceName else config.voiceName
        val speakingRate = 1.2f * voiceRate

        try {
            val builder = UriComponentsBuilder.fromUri(URI.create("https://texttospeech.googleapis.com/v1/text:synthesize"))
            val headers = HttpHeaders()
            headers["X-Goog-User-Project"] = googleProject
            headers["Content-Type"] = "application/json"
            headers["Authorization"] = "Bearer $token"
            var synthesise: GoogleInternalSynthesise = GoogleInternalSynthesise()
            synthesise.input = GoogleInternalSynthesiseInput(adText)
            synthesise.voice = GoogleInternalSynthesiseVoice(form.language, selectedVoice)
            synthesise.audioConfig = GoogleInternalSynthesiseAudioConfig("LINEAR16", speakingRate)
            val entity = HttpEntity(
                synthesise,
                headers
            )
            logger.info("Google entity = {}", entity.body)

            val response1 = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<GoogleInternalSynthesiseOutput?>() {
                }
            )
            return Base64.decodeBase64(response1.body?.audioContent)
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to generate Google speech. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to generate Google speech. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to generate Google speech with error : {}", e.message, e)
        }
        return null
    }

    fun initVoicesImpl(
        token: String
    ): List<GoogleInternalVoice>? {
        try {
            val builder = UriComponentsBuilder.fromUri(URI.create("https://texttospeech.googleapis.com/v1/voices"))
            val headers = HttpHeaders()
            headers["Authorization"] = "Bearer $token"
            val entity = HttpEntity(
                "",
                headers
            )

            val response1 : ResponseEntity<GoogleInternalVoices?> = restOperations.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                object : ParameterizedTypeReference<GoogleInternalVoices?>() {
                }
            )
            return response1.body?.voices
        } catch (e: HttpClientErrorException) {
            logger.warn(
                "Failed to get Google voices. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: HttpServerErrorException) {
            logger.warn(
                "Failed to get Google voices. Status: {}, Body: {}",
                e.statusCode,
                e.responseBodyAsString,
                e
            )
        } catch (e: Exception) {
            logger.error("Failed to get Google voices with error : {}", e.message, e)
        }
        return null
    }

}
