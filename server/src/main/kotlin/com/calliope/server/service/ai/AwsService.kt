package com.calliope.server.service.ai

import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.utils.WavUtils.pcmToWav
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.polly.PollyClient
import software.amazon.awssdk.services.polly.model.*
import java.util.*
import kotlin.math.floor


@Service
class AwsService(
    private val pollyClient: PollyClient,
    @param:Lazy private val awsVoices: List<Voice?>
) : VoiceGeneratorService {
    private val logger = KotlinLogging.logger {}

    fun initVoices(
    ): List<Voice> {
        val voices = initVoicesImpl()
        return voices
    }

    fun isApplicable(
        form: AdGenerateForm
    ): Boolean {
        return awsVoices
            .filter { voice: Voice? ->
                StringUtils.isEmpty(form.gender) || form.gender.equals(voice!!.genderAsString())
            }
            .filter { voice: Voice? ->
                StringUtils.isEmpty(form.language) || form.language.equals(voice!!.languageCodeAsString())
                        || Optional.ofNullable<List<String>>(voice.additionalLanguageCodesAsStrings()).orElse(listOf<String>()).contains(form.language)
            }
            .count() > 0
    }

    override fun getProvider(
    ): String {
        return "aws"
    }

    override fun getName(
        form: AdGenerateForm
    ): String? {
        val awsVoices2: MutableList<Voice?> = mutableListOf(*awsVoices.toTypedArray())
        awsVoices2.shuffle()
        val selectedVoice = awsVoices
            .filter { voice: Voice? ->
                StringUtils.isEmpty(form.gender) || form.gender.equals(voice!!.genderAsString())
            }
            .filter { voice: Voice? ->
                StringUtils.isEmpty(form.language) || form.language.equals(voice!!.languageCodeAsString())
                        || Optional.ofNullable<List<String>>(voice.additionalLanguageCodesAsStrings()).orElse(listOf<String>()).contains(form.language)
            }
            .map<Voice?, String?> { obj: Voice? -> obj!!.idAsString() }
            .firstOrNull()
        logger.info("getName {}", selectedVoice)
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
        val selectedHardcodeVoice = awsVoices
            .filter { voice: Voice? -> StringUtils.isEmpty(gender) /* same voice */ || selectedVoice != voice!!.idAsString() }
            .filter { voice: Voice? -> StringUtils.isEmpty(gender) || gender == voice!!.genderAsString() }
            .filter { voice: Voice? ->
                StringUtils.isEmpty(form.language) || form.language.equals(voice!!.languageCodeAsString())
                        || Optional.ofNullable<List<String>>(voice.additionalLanguageCodesAsStrings()).orElse(listOf<String>()).contains(form.language)
            }
            .map<Voice?, String?> { obj: Voice? -> obj!!.idAsString() }
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
        val voice = generateVoiceImpl(adText, voiceRate, form, config, isHardcode)
        return voice
    }

    fun generateVoiceImpl(
        adText: String,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        isHardcode: Boolean
    ): ByteArray? {
        val selectedVoiceName: String? = if (isHardcode) config.hardcodeVoiceName else config.voiceName
        logger.info("generateVoiceImpl(), {}", selectedVoiceName)
        logger.info("generateVoiceImpl(), {}", isHardcode)
        logger.info("generateVoiceImpl(), {}", config)

        val selectedVoice = awsVoices
            .filter { v: Voice? -> v!!.idAsString() == selectedVoiceName }
            .firstOrNull()

        try {
            val rate = Math.round(floor((120f * voiceRate - 100).toDouble()))
            val ssml = """<speak xmlns:mstts="http://www.w3.org/2001/mstts" version='1.0'>
<prosody rate="$rate%">
$adText</prosody></speak>"""
            val speech: ResponseInputStream<SynthesizeSpeechResponse> = pollyClient.synthesizeSpeech(
                SynthesizeSpeechRequest.builder()
                    .engine(getEngine(selectedVoice!!.supportedEngines()))
                    .voiceId(selectedVoiceName)
                    .outputFormat(OutputFormat.PCM)
                    .languageCode(form.language)
                    .text(ssml)
                    .textType(TextType.SSML)
                    .build()
            )

            val wav = pcmToWav(
                IOUtils.toByteArray(speech),
                16000,
                1,
                16
            )

            return wav
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

    fun getEngine(engines: List<Engine?>): Engine? {
        if (engines.contains(Engine.NEURAL)) {
            return Engine.NEURAL
        }
        if (engines.contains(Engine.GENERATIVE)) {
            return Engine.GENERATIVE
        }
        if (engines.contains(Engine.STANDARD)) {
            return Engine.STANDARD
        }
        return engines[0]
    }

    fun initVoicesImpl(): List<Voice> {
        val voices = pollyClient.describeVoices().voices()
        return listOf(*voices.toTypedArray())
    }
}
