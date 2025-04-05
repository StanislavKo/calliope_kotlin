package com.calliope.server.service.advertisement

import com.calliope.server.consts.Consts
import com.calliope.server.exception.CloudProviderNotFoundException
import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.service.ai.AwsService
import com.calliope.server.service.ai.AzureService
import com.calliope.server.service.ai.GoogleService
import com.calliope.server.service.ai.VoiceService
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.mutable.MutableObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import mu.KotlinLogging

@Service
class GenerateVoiceService(
    private val s3Client: S3Client,
    private val voiceService: VoiceService,
    private val generateCommonService: GenerateCommonService,
    private val azureService: AzureService,
    private val googleService: GoogleService,
    private val awsService: AwsService
) {
    private val logger = KotlinLogging.logger {}
    @Value("\${advertisement.generation.fs-prefix:}")
    private val fsPrefix: String? = null

    @Value("\${advertisement.generation.docker-fs-prefix:}")
    private val dockerFsPrefix: String? = null

    fun initConfigVoice(
        form: AdGenerateForm
    ): GenerationConfig {
        try {
            return initConfigVoiceImpl(form)
        } catch (e: CloudProviderNotFoundException) {
            val localeForm: String? = form.language
            val allLocales = voiceService.languageCodes()
            if (!allLocales.contains(localeForm)) {
                form.language = "en-US"
            }
            val allLocalesList: List<String?> = listOf(*allLocales.toTypedArray())
            Collections.shuffle(allLocalesList)
            val allLocalesMap: Map<String, MutableSet<String?>> = allLocalesList
                .groupBy { it!!.substring(0, 2) }
                .mapValues { (_, values) -> values.toMutableSet() }
            val sameFamilyLocales = allLocalesMap[localeForm?.substring(0, 2)]!!
            sameFamilyLocales.remove(localeForm)
            for (locale in sameFamilyLocales) {
                form.language = locale
                try {
                    return initConfigVoiceImpl(form)
                } catch (ignored: CloudProviderNotFoundException) {
                }
            }
        }
        throw RuntimeException("Can't select voice provider")
    }

    @Throws(CloudProviderNotFoundException::class)
    fun initConfigVoiceImpl(
        form: AdGenerateForm
    ): GenerationConfig {
        val configs: MutableSet<GenerationConfig?> = ConcurrentHashMap.newKeySet()

        val azureVoiceName = azureService.getName(form)
        if (azureVoiceName != null) {
            configs.add(GenerationConfig(azureService.getProvider(), azureVoiceName))
        }
        val googleVoiceName = googleService.getName(form)
        if (googleVoiceName != null) {
            configs.add(GenerationConfig(googleService.getProvider(), googleVoiceName))
        }
        val awsVoiceName = awsService.getName(form)
        if (awsVoiceName != null) {
            configs.add(GenerationConfig(awsService.getProvider(), awsVoiceName))
        }

        val configsList: List<GenerationConfig?> = listOf<GenerationConfig?>(*configs.toTypedArray())
        Collections.shuffle(configsList)
        val config = if (configsList.isEmpty()) null else configsList[0]

        if (config != null) {
            return config
        } else {
            throw CloudProviderNotFoundException("Can't select voice provider")
        }
    }

    fun initHardcodeVoice(
        form: AdGenerateForm,
        config: GenerationConfig,
        hardcodeVoice: String?
    ) {
        if (azureService.getProvider() == config.voiceProvider) {
            val azureVoiceName = azureService.getHardcodeName(form, config.voiceName!!)
            config.hardcodeVoiceProvider = if (azureVoiceName != null) azureService.getProvider() else config.voiceProvider
            config.hardcodeVoiceName = azureVoiceName ?: config.voiceName
            return
        }
        if (googleService.getProvider() == config.voiceProvider) {
            val googleVoiceName = googleService.getHardcodeName(form, config.voiceName!!)
            config.hardcodeVoiceProvider = if (googleVoiceName != null) googleService.getProvider() else config.voiceProvider
            config.hardcodeVoiceName = googleVoiceName ?: config.voiceName
            return
        }
        if (awsService.getProvider() == config.voiceProvider) {
            val awsVoiceName = awsService.getHardcodeName(form, config.voiceName!!)
            config.hardcodeVoiceProvider = if (awsVoiceName != null) awsService.getProvider() else config.voiceProvider
            config.hardcodeVoiceName = awsVoiceName ?: config.voiceName
            return
        }
    }

    fun generateVoice(
        adText: String,
        form: AdGenerateForm,
        configHolder: MutableObject<GenerationConfig?>,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        durationSetter: Consumer<Float?>
    ): CompletableFuture<Float?> {
        var duration: Float? = null
        val voiceRate = 1f
        duration = when (configHolder.value!!.voiceProvider) {
            "google" -> generateVoiceGoogle(
                adText,
                voiceRate,
                form,
                configHolder.value!!,
                uuid,
                fsPrefixCopy,
                artifactUuid,
                datePrefix,
                durationSetter,
                IS_HARDCODED_FALSE
            )

            "aws" -> generateVoiceAws(
                adText,
                voiceRate,
                form,
                configHolder.value!!,
                uuid,
                fsPrefixCopy,
                artifactUuid,
                datePrefix,
                durationSetter,
                IS_HARDCODED_FALSE
            )

            "azure" -> generateVoiceAzure(
                adText,
                voiceRate,
                form,
                configHolder.value!!,
                uuid,
                fsPrefixCopy,
                artifactUuid,
                datePrefix,
                durationSetter,
                IS_HARDCODED_FALSE
            )

            else -> throw RuntimeException("Unknown voice provider")
        }
        logger.info("generateVoice() EXIT")
        return CompletableFuture.completedFuture(duration)
    }

    fun generateVoice2(
        adText: String,
        voiceDurationRequested: Float,
        voiceRate: Float,
        form: AdGenerateForm,
        configHolder: MutableObject<GenerationConfig?>,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        durationSetter: Consumer<Float?>
    ): CompletableFuture<Float?> {
        var duration: Float = when (configHolder.value!!.voiceProvider) {
            "google" -> generateVoiceGoogle(
                adText,
                voiceRate,
                form,
                configHolder.value!!,
                uuid,
                fsPrefixCopy,
                artifactUuid,
                datePrefix,
                durationSetter,
                IS_HARDCODED_FALSE
            )

            "aws" -> generateVoiceAws(
                adText,
                voiceRate,
                form,
                configHolder.value!!,
                uuid,
                fsPrefixCopy,
                artifactUuid,
                datePrefix,
                durationSetter,
                IS_HARDCODED_FALSE
            )

            "azure" -> generateVoiceAzure(
                adText,
                voiceRate,
                form,
                configHolder.value!!,
                uuid,
                fsPrefixCopy,
                artifactUuid,
                datePrefix,
                durationSetter,
                IS_HARDCODED_FALSE
            )

            else -> throw RuntimeException("Unknown voice provider")
        }
        logger.info("generateVoice2() duration = {}", duration)

        if (duration > form.contentLength!! - 0.1 || duration < form.contentLength!! - 1) {
            val voiceRateCorrected = voiceRate * duration!! / voiceDurationRequested
            logger.info(
                "generateVoice2() voiceRate = {}, voiceRateCorrected = {}",
                voiceRate,
                voiceRateCorrected
            )
            duration = when (configHolder.value!!.voiceProvider) {
                "google" -> generateVoiceGoogle(
                    adText,
                    voiceRateCorrected,
                    form,
                    configHolder.value!!,
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    durationSetter,
                    IS_HARDCODED_FALSE
                )

                "aws" -> generateVoiceAws(
                    adText,
                    voiceRateCorrected,
                    form,
                    configHolder.value!!,
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    durationSetter,
                    IS_HARDCODED_FALSE
                )

                "azure" -> generateVoiceAzure(
                    adText,
                    voiceRateCorrected,
                    form,
                    configHolder.value!!,
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    durationSetter,
                    IS_HARDCODED_FALSE
                )

                else -> throw RuntimeException("Unknown voice provider")
            }
            logger.info("generateVoice2() durationCorrected = {}", duration)
        }

        logger.info("generateVoice() EXIT")
        return CompletableFuture.completedFuture(duration)
    }

    fun generateHardcodeVoice(
        hardcodeText: String?,
        form: AdGenerateForm,
        configHolder: MutableObject<GenerationConfig?>,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        durationSetter: Consumer<Float?>
    ): CompletableFuture<Float> {
        if (StringUtils.isEmpty(hardcodeText)) {
            return CompletableFuture.completedFuture(0f)
        }

        val filename = "voice_hardcoded_out.wav"
        val filepath = fsPrefix + fsPrefixCopy + File.separator + filename
        var duration: Float? = null
        when (configHolder.value!!.voiceProvider) {
            "google" -> {
                generateVoiceGoogle(
                    hardcodeText!!,
                    1f,
                    form,
                    configHolder.value!!,
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    durationSetter,
                    IS_HARDCODED_TRUE
                )
                shrinkHardcodeVoice(
                    "voice_hardcoded_in.wav",
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    form.hardcodeSpeed
                )
                duration = (FileUtils.sizeOf(File(filepath)).toFloat()) / 32000f
            }

            "aws" -> {
                generateVoiceAws(
                    hardcodeText!!,
                    1f,
                    form,
                    configHolder.value!!,
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    durationSetter,
                    IS_HARDCODED_TRUE
                )
                shrinkHardcodeVoice(
                    "voice_hardcoded_in.wav",
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    form.hardcodeSpeed
                )
                duration = (FileUtils.sizeOf(File(filepath)).toFloat()) / 32000f
            }

            "azure" -> {
                generateVoiceAzure(
                    hardcodeText!!,
                    1f,
                    form,
                    configHolder.value!!,
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    durationSetter,
                    IS_HARDCODED_TRUE
                )
                shrinkHardcodeVoice(
                    "voice_hardcoded_in.wav",
                    uuid,
                    fsPrefixCopy,
                    artifactUuid,
                    datePrefix,
                    form.hardcodeSpeed
                )
                duration = (FileUtils.sizeOf(File(filepath)).toFloat()) / 32000f
            }

            else -> throw RuntimeException("Unknown voice provider")
        }

        //        return CompletableFuture.completedFuture(duration);
        return CompletableFuture.completedFuture(duration)
    }

    private fun generateVoiceAzure(
        adText: String,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        durationSetter: Consumer<Float?>,
        isHardcode: Boolean
    ): Float {
        val t1 = System.currentTimeMillis()
        var duration: Float? = null
        try {
            FileUtils.writeStringToFile(
                File(fsPrefix + fsPrefixCopy + File.separator + (if (isHardcode) "hardcodeText" else "adText") + ".txt"),
                adText,
                Charset.forName("UTF-8")
            )
            val voice = azureService.generateVoice(adText, voiceRate, form, config, isHardcode)
            val filenameS3 = if (isHardcode) "voice_hardcoded_out_$uuid.wav" else "voice_out_$uuid.wav"
            val filenameFs = if (isHardcode) "voice_hardcoded_out.wav" else "voice_out.wav"
            if (!isHardcode) {
                s3Client.putObject(
                    PutObjectRequest.builder().bucket(Consts.BUCKET).key(
                        generateCommonService.getS3Key(
                            "$datePrefix/$artifactUuid/$uuid", "wav"
                        )
                    ).build(), RequestBody.fromBytes(voice)
                )
            }
            FileUtils.writeByteArrayToFile(File(fsPrefix + fsPrefixCopy + File.separator + filenameFs), voice)
            duration = generateCommonService.getWavDuration(filenameFs, fsPrefixCopy)
            durationSetter.accept(duration)
        } catch (e: IOException) {
            logger.error("Can't generate azure voice", e)
        }
        val t2 = System.currentTimeMillis()
        logger.info("voice-azure generated in {} milliseconds, is {}", (t2 - t1), adText)
        return duration!!
    }

    private fun generateVoiceGoogle(
        adText: String,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        durationSetter: Consumer<Float?>,
        isHardcode: Boolean
    ): Float {
        val t1 = System.currentTimeMillis()
        var duration: Float? = null
        try {
            FileUtils.writeStringToFile(
                File(fsPrefix + fsPrefixCopy + File.separator + (if (isHardcode) "hardcodeText" else "adText") + ".txt"),
                adText,
                Charset.forName("UTF-8")
            )

            val token = googleService.token
            val voice = googleService.generateVoice(adText, voiceRate, form, config, isHardcode, token)
            val filenameS3 = if (isHardcode) "voice_hardcoded_out_$uuid.wav" else "voice_out_$uuid.wav"
            val filenameFs = if (isHardcode) "voice_hardcoded_out.wav" else "voice_out.wav"
            s3Client.putObject(
                PutObjectRequest.builder().bucket(Consts.BUCKET).key(
                    generateCommonService.getS3Key(
                        "$datePrefix/$artifactUuid/$uuid", "wav"
                    )
                ).build(), RequestBody.fromBytes(voice)
            )
            FileUtils.writeByteArrayToFile(File(fsPrefix + fsPrefixCopy + File.separator + filenameFs), voice)
            duration = generateCommonService.getWavDuration(filenameFs, fsPrefixCopy)
            durationSetter.accept(duration)
            val t2 = System.currentTimeMillis()
            logger.info("voice-google generated in {} milliseconds, is {}", (t2 - t1), adText)
        } catch (e: Exception) {
            logger.error("Can't generate google voice", e)
        }
        return duration!!
    }

    private fun generateVoiceAws(
        adText: String,
        voiceRate: Float,
        form: AdGenerateForm,
        config: GenerationConfig,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        durationSetter: Consumer<Float?>,
        isHardcode: Boolean
    ): Float {
        val t1 = System.currentTimeMillis()
        var duration: Float? = null
        try {
            FileUtils.writeStringToFile(
                File(fsPrefix + fsPrefixCopy + File.separator + (if (isHardcode) "hardcodeText" else "adText") + ".txt"),
                adText,
                Charset.forName("UTF-8")
            )

            val voice = awsService.generateVoice(adText, voiceRate, form, config, isHardcode)
            val filenameS3 = if (isHardcode) "voice_hardcoded_in_$uuid.wav" else "voice_out_$uuid.wav"
            val filenameFs = if (isHardcode) "voice_hardcoded_in.wav" else "voice_out.wav"
            if (!isHardcode) {
                s3Client.putObject(
                    PutObjectRequest.builder().bucket(Consts.BUCKET).key(
                        generateCommonService.getS3Key(
                            "$datePrefix/$artifactUuid/$uuid", "wav"
                        )
                    ).build(), RequestBody.fromBytes(voice)
                )
            }
            FileUtils.writeByteArrayToFile(File(fsPrefix + fsPrefixCopy + File.separator + filenameFs), voice)
            duration = generateCommonService.getWavDuration(filenameFs, fsPrefixCopy)
            durationSetter.accept(duration)
        } catch (e: IOException) {
            logger.error("Can't generate aws voice", e)
        }
        val t2 = System.currentTimeMillis()
        logger.info("voice-aws generated in {} milliseconds, is {}", (t2 - t1), adText)
        return duration!!
    }

    private fun shrinkHardcodeVoice(
        filename: String,
        uuid: String?,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        hardcodeSpeedIn: Float?
    ) {
        try {
            logger.info("shrinkHardcodeVoice >> before shrinking hardcoded voice")

            val hardcodeSpeed = hardcodeSpeedIn ?: 1f
            //            downloadFile(uuid, "wav", "g:\\tmp\\calliope\\voice_hardcoded_in.wav");
            val command = "docker run --rm " +
                    "  -v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                    "  linuxserver/ffmpeg " +
                    "  -i /config/voice_hardcoded_in.wav " +
                    "  -filter:a \"atempo=" + hardcodeSpeed + "\" " +
                    "  /config/voice_hardcoded_out.wav"
            logger.info("shrinkHardcodeVoice >> {}", command)

            val builder = ProcessBuilder("cmd.exe", "/c", command)
            builder.redirectErrorStream(true)
            val p = builder.start()
            val r = BufferedReader(InputStreamReader(p.inputStream))
            var line: String?
            while (true) {
                line = r.readLine()
                if (line == null) {
                    break
                }
                println(line)
            }

            s3Client.putObject(
                PutObjectRequest.builder().bucket(Consts.BUCKET).key(
                    generateCommonService.getS3Key(
                        "$datePrefix/$artifactUuid/$uuid", "wav"
                    )
                ).build(),
                RequestBody.fromFile(File(fsPrefix + fsPrefixCopy + File.separator + "voice_hardcoded_out.wav"))
            )

            logger.info("shrinkHardcodeVoice >> EXIT")
        } catch (e: Exception) {
            logger.error("Can't shrinkHardcodeVoice", e)
        }
    }

    companion object {
        private const val IS_HARDCODED_TRUE = true
        private const val IS_HARDCODED_FALSE = false
    }
}
