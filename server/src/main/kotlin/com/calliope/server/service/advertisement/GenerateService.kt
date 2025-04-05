package com.calliope.server.service.advertisement

import com.calliope.core.mysql.model.Artifact
import com.calliope.core.mysql.model.Generation
import com.calliope.core.mysql.model.GenerationCopy
import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.enums.ArtifactType
import com.calliope.core.mysql.repositories.*
import com.calliope.server.consts.Consts
import com.calliope.server.exception.CustomError
import com.calliope.server.exception.CustomErrorCode
import com.calliope.server.exception.CustomErrorException
import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.balance.SpendType
import com.calliope.server.model.domain.generation.ArtifactHardcodeDetails
import com.calliope.server.model.domain.generation.ArtifactMusicDetails
import com.calliope.server.model.domain.generation.ArtifactSpeechDetails
import com.calliope.server.model.domain.generation.GenerationResult
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.model.form.OperationGeneratePriceForm
import com.calliope.server.model.web.GenerateResponse
import com.calliope.server.service.ai.OpenaiService
import com.calliope.server.service.balance.BalanceService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.mutable.MutableObject
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.math.abs

@Service
class GenerateService(
    private val mapper: ObjectMapper,
    private val azureVoices: List<AzureInternalVoice>,
    private val languageSpeeds: Map<String, LanguageSpeed>,
    @param:Qualifier("languageCode2Name") private val languageCode2Name: Map<String, String>,
    private val s3Client: S3Client,
    @param:Qualifier("mainExecutor") private val executor: Executor,
    private val userRepository: UserRepository,
    private val userMessageRepository: UserMessageRepository,
    private val generationRepository: GenerationRepository,
    private val generationCopyRepository: GenerationCopyRepository,
    private val artifactRepository: ArtifactRepository,
    private val balanceService: BalanceService,
    private val openaiService: OpenaiService,
    private val generateCommonService: GenerateCommonService,
    private val generateBannerService: GenerateBannerService,
    private val generateVoiceService: GenerateVoiceService,
    private val generateMusicService: GenerateMusicService,
    private val generateGenderLocaleMoodService: GenerateGenderLocaleMoodService
) {
    private val logger = KotlinLogging.logger {}
    private val DATE_FILE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DTF_FILE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss")
    private val IS_RANDOM_TRUE = true
    private val IS_RANDOM_FALSE = false
    private val VOICE_RESERVE = 0.6f

    @Value("\${advertisement.generation.fs-prefix:}")
    private val fsPrefix: String? = null

    @Value("\${advertisement.generation.docker-fs-prefix:}")
    private val dockerFsPrefix: String? = null

    fun generate(user: User?, form: AdGenerateForm, isLocal: Boolean): GenerateResponse {
        try {
            val price: Double = balanceService.estimate(
                user,
                SpendType.GENERATION_UI.estimationOperation,
                mapper.writeValueAsString(
                    OperationGeneratePriceForm(form.contentLength, form.music, form.banner, form.copies)
                )
            )?.price!!

            val datePrefix = DATE_FILE.format(LocalDateTime.now())
            val generationSummary = MutableObject<String?>(null)
            CompletableFuture.runAsync({
                generateGenderLocaleMoodService.generateSummary(
                    form.productDescription
                ) { summary: String? -> generationSummary.setValue(summary) }
            }, executor)

            val generationResultsFutures: MutableList<CompletableFuture<Void>> = mutableListOf()
            val generationResults: MutableList<GenerationResult> = mutableListOf()
            for (i in 0..<form.copies!!) {
                generationResultsFutures.add(CompletableFuture.runAsync({
                    generateImpl(
                        form,
                        generationResults,
                        datePrefix,
                        isLocal
                    )
                }, executor))
            }
            val combinedFutures =
                CompletableFuture.allOf(*generationResultsFutures.toTypedArray())
            combinedFutures.join()
            logger.info("after generation copies combinedFutures.join()")

            var generation = Generation()
            generation.user = user
            generation.title =
                if (generationSummary.value == null) DTF_FILE.format(LocalDateTime.now()) else generationSummary.value
            generation.duration = form.contentLength
            generation.copies = form.copies
            generation.datePrefix = datePrefix
            generation = generationRepository.save(generation)

            var lastArtifactUuid: String? = null
            for (i in generationResults.indices) {
                val generationResult = generationResults[i]
                var generationCopy = GenerationCopy()
                generationCopy.generation = generation
                generationCopy.uuid = generationResult.artifactUuid
                generationCopy.orderNum = i
                generationCopy = generationCopyRepository.save(generationCopy)

                if (generationResult.bannerUuid != null) {
                    var artifact = Artifact()
                    artifact.generationCopy = generationCopy
                    artifact.type = ArtifactType.BANNER
                    artifact.uuid = generationResult.bannerUuid
                    artifact = artifactRepository.save(artifact)
                }
                if (generationResult.musicUuid != null) {
                    var artifact = Artifact()
                    artifact.generationCopy = generationCopy
                    artifact.type = ArtifactType.MUSIC
                    artifact.uuid = generationResult.musicUuid
                    artifact.duration = generationResult.musicDuration
                    val details: ArtifactMusicDetails = ArtifactMusicDetails()
                    details.musicMood = generationResult.musicMood
                    artifact.details = mapper.writeValueAsString(details)
                    artifact = artifactRepository.save(artifact)
                }
                if (generationResult.voiceUuid != null) {
                    var artifact = Artifact()
                    artifact.generationCopy = generationCopy
                    artifact.type = ArtifactType.SPEECH
                    artifact.uuid = generationResult.voiceUuid
                    artifact.duration = generationResult.voiceDuration
                    var details: ArtifactSpeechDetails = ArtifactSpeechDetails()
                    details.voiceDuration = generationResult.voiceDuration
                    details.voiceText = generationResult.voiceText
                    details.voiceProvider = generationResult.voiceProvider
                    details.voiceName = generationResult.voiceName
                    artifact.details = mapper.writeValueAsString(details)
                    artifact = artifactRepository.save(artifact)
                }
                if (generationResult.voiceHardcodeUuid != null) {
                    var artifact = Artifact()
                    artifact.generationCopy = generationCopy
                    artifact.type = ArtifactType.HARDCODED
                    artifact.uuid = generationResult.voiceHardcodeUuid
                    artifact.duration = generationResult.voiceHardcodeDuration
                    var details: ArtifactHardcodeDetails = ArtifactHardcodeDetails()
                    details.voiceHardcodeDuration = generationResult.voiceHardcodeDuration
                    details.voiceHardcodeText = form.hardcodeText
                    details.voiceHardcodeProvider = generationResult.voiceHardcodeProvider
                    details.voiceHardcodeName = generationResult.voiceHardcodeName
                    artifact.details = mapper.writeValueAsString(details)
                    artifact = artifactRepository.save(artifact)
                }
                lastArtifactUuid = generationResult.artifactUuid
            }

            balanceService.spend(
                user,
                SpendType.GENERATION_UI,
                generation.generationId.toString(),
                mapper.writeValueAsString(form),
                price
            )

            val url = "https://calliope.cerebro1.com/$datePrefix/$lastArtifactUuid/$lastArtifactUuid.wav"
            return GenerateResponse(url)
        } catch (e: CustomErrorException) {
            throw e
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw CustomErrorException(
                CustomError("Generic error"),
                CustomErrorCode.BAD_PARAMETER
            )
        }
    }

    fun generateImpl(
        formIn: AdGenerateForm,
        generationResults: MutableList<GenerationResult>,
        datePrefix: String,
        isLocal: Boolean
    ) {
        val form: AdGenerateForm = formIn.clone()
        if (form.contentLength == null) {
            form.contentLength = 30
        }
        if (form.contentLength!! < 1 || form.contentLength!! > 40) {
            throw RuntimeException("Invalid length")
        }

        //            String artifactUuid = "";
//            String musicUuid = "d6d4e497-3e82-44f4-91d4-bd82b6073055";
//            String voiceUuid = "b592892d-cc6f-4cf1-b1de-a158a73cdf3f";
//            String voiceHardcodeUuid = null;
//            String bannerUuid = "ced5de3a-591a-4e3a-a49c-e7183a23f5eb";
        val artifactUuid = UUID.randomUUID().toString()
        val musicUuid = UUID.randomUUID().toString()
        val voiceUuid = UUID.randomUUID().toString()
        val voiceHardcodeUuid = if (StringUtils.isEmpty(form.hardcodeText)) null else UUID.randomUUID().toString()
        val bannerUuid = UUID.randomUUID().toString()
        println("form = " + form)
        logger.info("UUIDs:")
        logger.info("artifactUuid : {}", musicUuid)
        logger.info("musicUuid : {}", musicUuid)
        logger.info("voiceUuid : {}", voiceUuid)
        logger.info("voiceHardcodeUuid : {}", voiceHardcodeUuid)
        logger.info("bannerUuid : {}", bannerUuid)
        val fsPrefixCopy = artifactUuid

        if (isLocal) {
            try {
                FileUtils.cleanDirectory(File(fsPrefix, fsPrefixCopy))
            } catch (ignored: IOException) {
            }
            File(fsPrefix, fsPrefixCopy).mkdirs()
        }

        val config = MutableObject<GenerationConfig?>(null)

        val generationResult: GenerationResult = GenerationResult()
        generationResult.artifactUuid = artifactUuid
        generationResult.musicUuid = musicUuid
        generationResult.musicDuration = form.contentLength!!.toFloat()
        generationResult.voiceUuid = voiceUuid
        generationResult.voiceHardcodeUuid = voiceHardcodeUuid
        generationResult.bannerUuid = bannerUuid
        generationResult.datePrefix = datePrefix

        val combinedFutures = CompletableFuture.allOf(
            CompletableFuture.supplyAsync<CompletableFuture<Void>>(
                {
                    generateGenderLocaleMoodService.generateGender(
                        form.productDescription,
                        form
                    )
                }, executor
            ).thenComposeAsync<Void?>(
                { none: CompletableFuture<Void>? ->
                    generateGenderLocaleMoodService.generateLocale(
                        form.productDescription,
                        form
                    )
                }, executor
            ).thenComposeAsync<Void?>(
                { none: Void? -> initConfigVoice(form, config, generationResult) }, executor
            ).thenComposeAsync<Float?>(
                { none: Void? ->
                    generateVoiceService.generateHardcodeVoice(
                        form.hardcodeText,
                        form,
                        config,
                        voiceHardcodeUuid,
                        fsPrefixCopy,
                        artifactUuid,
                        datePrefix
                    ) { duration: Float? ->
                        generationResult.voiceHardcodeDuration = duration
                        logger.warn("Setter setVoiceHardcodeDuration @@@@@@@@@@@@@ {}", duration)
                    }
                }, executor
            ).thenComposeAsync<String?>(
                { hardcodeDuration: Float? ->
                    generateSampleSpeech(
                        form.productDescription!!,
                        form.language,
                        generationResult
                    )
                }, executor
            ).thenComposeAsync<Float?>(
                { sampleText: String ->
                    generateVoiceService.generateVoice(
                        sampleText,
                        form,
                        config,
                        voiceUuid,
                        fsPrefixCopy,
                        artifactUuid,
                        datePrefix
                    ) { duration: Float? -> generationResult.sampleSpeechDuration = duration }
                }, executor
            ).thenComposeAsync<String?>(
                { voiceDuration: Float? ->
                    generateAdText(
                        form.contentLength!!,
                        form.productDescription!!,
                        form.language,
                        generationResult.voiceHardcodeDuration,
                        generationResult
                    )
                }, executor
            ).thenComposeAsync<Float?>(
                { adText: String ->
                    generateVoiceService.generateVoice2(
                        adText,
                        generationResult.voiceDurationRequested!!,
                        generationResult.voiceRate!!,
                        form,
                        config,
                        voiceUuid,
                        fsPrefixCopy,
                        artifactUuid,
                        datePrefix
                    ) { duration: Float? -> generationResult.voiceDuration = duration }
                }, executor
            ),
            CompletableFuture.supplyAsync<String?>(
                {
                    generateGenderLocaleMoodService.generateMood(
                        form.music,
                        form.productDescription,
                        form.mood
                    ) { mood: String? -> generationResult.musicMood = mood }
                }, executor
            ).thenComposeAsync<Void?>(
                { mood: String? ->
                    generateMusicService.generateMusicByMood(
                        form.contentLength!!,
                        mood,
                        form.music,
                        musicUuid,
                        fsPrefixCopy,
                        artifactUuid,
                        datePrefix
                    ) { musicUuidNew: String? -> generationResult.musicUuid = musicUuidNew }
                },
                executor
            ),
            CompletableFuture.supplyAsync<String?>(
                {
                    generateBannerService.generateImageDescription(
                        form.banner,
                        form.productDescription
                    )
                }, executor
            ).thenComposeAsync<Void?>(
                { adText: String? ->
                    generateBannerService.generateBanner(
                        adText,
                        form.banner,
                        form.aspectRatio,
                        bannerUuid,
                        fsPrefixCopy,
                        artifactUuid,
                        datePrefix
                    ) { bannerUuidNew: String? -> generationResult.bannerUuid = bannerUuidNew }
                }, executor
            )
        )
        combinedFutures.join()
        logger.info("after combinedFutures.join()")

        mergeVoiceMusic(
            musicUuid,
            voiceUuid,
            voiceHardcodeUuid,
            form.hardcodePosition,
            form.music!!,
            fsPrefixCopy,
            artifactUuid,
            datePrefix,
            isLocal
        )
        generationResults.add(generationResult)
    }

    private fun initConfigVoice(
        form: AdGenerateForm,
        configHolder: MutableObject<GenerationConfig?>,
        generationResult: GenerationResult
    ): CompletableFuture<Void?> {
        val t1 = System.currentTimeMillis()
        val config = generateVoiceService.initConfigVoice(form)
        if (StringUtils.isNotEmpty(form.hardcodeText) && "same" != form.hardcodeVoice) {
            generateVoiceService.initHardcodeVoice(form, config!!, form.hardcodeVoice)
        } else if (StringUtils.isNotEmpty(form.hardcodeText) && "same" == form.hardcodeVoice) {
            config.hardcodeVoiceProvider = config.voiceProvider
            config.hardcodeVoiceName = config.voiceName
        }
        configHolder.value = config
        generationResult.voiceProvider = config.voiceProvider
        generationResult.voiceName = config.voiceName
        generationResult.voiceHardcodeProvider = config.hardcodeVoiceProvider
        generationResult.voiceHardcodeName = config.hardcodeVoiceName
        logger.info("config = {}", config)
        val t2 = System.currentTimeMillis()
        return CompletableFuture.completedFuture(null)
    }

    private fun generateSampleSpeech(
        description: String,
        locale: String?,
        generationResult: GenerationResult
    ): CompletableFuture<String?> {
        val t1 = System.currentTimeMillis()
        val characters = 500
        val sampleText = openaiService.generateAdText(
            description, locale,
            languageCode2Name[locale?.substring(0, 2)], characters
        )
        generationResult.sampleSpeechCharacters = characters
        generationResult.sampleText = sampleText
        val t2 = System.currentTimeMillis()
        logger.info("sampleSpeech generated in {} milliseconds, is {}", (t2 - t1), sampleText)
        return CompletableFuture.completedFuture(sampleText)
    }

    private fun generateAdText(
        duration: Int,
        description: String,
        locale: String?,
        hardcodeDuration: Float?,
        generationResult: GenerationResult
    ): CompletableFuture<String?> {
        val t1 = System.currentTimeMillis()
        val requestedVoiceDuration = Optional.ofNullable(duration.toFloat()).map { d: Float ->
            d - Optional.ofNullable(
                hardcodeDuration
            ).orElse(0f)
        }.orElse(20.0f) - VOICE_RESERVE
        val sampleAlphaNumericCharacters: Long = generationResult.sampleText!!.chars().filter { ch ->
            Character.isLetterOrDigit(
                ch
            )
        }.count()
        val alphaNumericCharacters =
            (requestedVoiceDuration * sampleAlphaNumericCharacters / generationResult.sampleSpeechDuration!!).toInt()
        val totalCharacters =
            (requestedVoiceDuration * generationResult.sampleText!!.length / generationResult.sampleSpeechDuration!!).toInt()
        logger.info("@@@@@@@@@ generateAdText2(), duration = {}", duration)
        logger.info("@@@@@@@@@ generateAdText2(), requestedVoiceDuration = {}", requestedVoiceDuration)
        logger.info(
            "@@@@@@@@@ generateAdText2(), sampleAlphaNumericCharacters = {}",
            sampleAlphaNumericCharacters
        )
        logger.info(
            "@@@@@@@@@ generateAdText2(), generationResult.getSampleText().length() = {}",
            generationResult.sampleText!!.length
        )
        logger.info(
            "@@@@@@@@@ generateAdText2(), generationResult.getSampleSpeechCharacters() = {}",
            generationResult.sampleSpeechCharacters
        )
        logger.info(
            "@@@@@@@@@ generateAdText2(), generationResult.getSampleSpeechDuration() = {}",
            generationResult.sampleSpeechDuration
        )
        logger.info("@@@@@@@@@ generateAdText2(), alphaNumericCharacters = {}", alphaNumericCharacters)
        logger.info("@@@@@@@@@ generateAdText2(), totalCharacters = {}", totalCharacters)
        val futureTasks: MutableList<CompletableFuture<*>> = mutableListOf()
        for (i in 0..7) {
            futureTasks.add(
                CompletableFuture.runAsync({
                    generateAdTextImpl(
                        duration,
                        description,
                        locale,
                        generationResult.voiceHardcodeDuration,
                        generationResult,
                        alphaNumericCharacters
                    )
                }, executor)
            )
        }
        val combinedFutures = CompletableFuture.allOf(*futureTasks.toTypedArray())
        combinedFutures.join()

        var minDist = Int.MAX_VALUE
        var adText: String? = null
        for (adTextIt in generationResult.voiceTexts) {
            logger.info("@@@@@@@@@     generateAdText(), adTextIt.length() = {}", adTextIt.length)
            if (adTextIt.length <= totalCharacters * 1.1 && abs((adTextIt.length - totalCharacters).toDouble()) < minDist) {
                minDist = abs((adTextIt.length - totalCharacters).toDouble()).toInt()
                adText = adTextIt
            }
        }
        if (adText == null) {
            for (adTextIt in generationResult.voiceTexts) {
                logger.info("@@@@@@@@@     generateAdText(), adTextIt.length() = {}", adTextIt.length)
                if (abs((adTextIt.length - totalCharacters).toDouble()) < minDist) {
                    minDist = abs((adTextIt.length - totalCharacters).toDouble()).toInt()
                    adText = adTextIt
                }
            }
        }
        generationResult.voiceText = adText
        generationResult.voiceRate = adText!!.length.toFloat() / totalCharacters.toFloat()
        generationResult.voiceDurationRequested = requestedVoiceDuration
        logger.info("@@@@@@@@@     generateAdText() result, adText.length() = {}", adText!!.length)
        logger.info(
            "@@@@@@@@@     generateAdText() result, voice rate = {}",
            generationResult.voiceRate
        )
        val t2 = System.currentTimeMillis()
        logger.info("adText generated in {} milliseconds, is {}", (t2 - t1), adText)
        return CompletableFuture.completedFuture(adText)
    }

    private fun generateAdTextImpl(
        duration: Int,
        description: String,
        locale: String?,
        hardcodeDuration: Float?,
        generationResult: GenerationResult?,
        characters: Int
    ): CompletableFuture<Void?> {
        val adText = openaiService.generateAdText(
            description, locale,
            languageCode2Name[locale?.substring(0, 2)], characters
        )
        if (adText != null) {
            generationResult?.voiceTexts?.add(adText)
        }
        return CompletableFuture.completedFuture(null)
    }

    private fun mergeVoiceMusic(
        musicUuid: String,
        voiceUuid: String,
        voiceHardcodeUuid: String?,
        voiceHardcodePosition: String?,
        toGenerateMusic: Boolean,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        isLocal: Boolean
    ) {
        try {
            if (isLocal) {
                FileUtils.copyFile(
                    File(fsPrefix + fsPrefixCopy + File.separator + "voice_out.wav"),
                    File(fsPrefix + fsPrefixCopy + File.separator + "voice.wav")
                )
                if (toGenerateMusic) {
                    FileUtils.copyFile(
                        File(fsPrefix + fsPrefixCopy + File.separator + "music1.wav"),
                        File(fsPrefix + fsPrefixCopy + File.separator + "music.wav")
                    )
                }
                if (voiceHardcodeUuid != null) {
                    FileUtils.copyFile(
                        File(fsPrefix + fsPrefixCopy + File.separator + "voice_hardcoded_out.wav"),
                        File(fsPrefix + fsPrefixCopy + File.separator + "voice_hardcoded.wav")
                    )
                }
                mergeVoiceMusicLocal(voiceHardcodeUuid, voiceHardcodePosition, toGenerateMusic, fsPrefixCopy)

                s3Client.putObject(
                    PutObjectRequest.builder().bucket(Consts.BUCKET).key(
                        generateCommonService.getS3Key(
                            "$datePrefix/$artifactUuid/$fsPrefixCopy", "wav"
                        )
                    ).build(), RequestBody.fromFile(File(fsPrefix + fsPrefixCopy + File.separator + "adAudio.wav"))
                )
            }
        } catch (e: Exception) {
            logger.error("Can't merge voice and music", e)
        }
    }

    private fun mergeVoiceMusicLocal(
        voiceHardcodeUuid: String?,
        voiceHardcodePosition: String?,
        toGenerateMusic: Boolean,
        fsPrefixCopy: String
    ) {
        try {
            logger.info("merge >> before music converted into 48kHz")

            var command: String
            var builder: ProcessBuilder
            var p: Process
            var r: BufferedReader
            var line: String?
            if (toGenerateMusic) {
                command = "docker run --rm " +
                        "  -v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                        "  linuxserver/ffmpeg " +
                        "  -i /config/music.wav " +
                        "  -filter:a \"volume=0.5\" " +
                        "  -ar 48000 /config/music48000.wav"
                logger.info("merge >> {}", command)

                builder = ProcessBuilder("cmd.exe", "/c", command)
                builder.redirectErrorStream(true)
                p = builder.start()
                r = BufferedReader(InputStreamReader(p.inputStream))
                while (true) {
                    line = r.readLine()
                    if (line == null) {
                        break
                    }
                    println(line)
                }
            }

            if (voiceHardcodeUuid != null) {
                logger.info("merge >> voice and hardcode merging")

                command = if ("start" == voiceHardcodePosition) {
                    "docker run --rm " +
                            "  -v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                            "  linuxserver/ffmpeg " +
                            "  -i /config/voice_hardcoded.wav " +
                            "  -i /config/voice.wav " +
                            "  -filter_complex \"[0:a][1:a]concat=n=2:v=0:a=1\" " +  //                            "  -map '[out]' " +
                            "  /config/voice_and_hardcode.wav"
                } else {
                    "docker run --rm " +
                            "  -v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                            "  linuxserver/ffmpeg " +
                            "  -i /config/voice.wav " +
                            "  -i /config/voice_hardcoded.wav " +
                            "  -filter_complex \"[0:a][1:a]concat=n=2:v=0:a=1\" " +  //                            "  -map '[out]' " +
                            "  /config/voice_and_hardcode.wav"
                }
                logger.info("merge >> {}", command)

                builder = ProcessBuilder("cmd.exe", "/c", command)
                builder.redirectErrorStream(true)
                p = builder.start()
                r = BufferedReader(InputStreamReader(p.inputStream))
                while (true) {
                    line = r.readLine()
                    if (line == null) {
                        break
                    }
                    println(line)
                }
            }

            logger.info("merge >> before voice and music merging")

            val voiceFileName = if (voiceHardcodeUuid != null) "voice_and_hardcode.wav" else "voice.wav"
            if (toGenerateMusic) {
                command = "docker run --rm " +
                        "  -v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                        "  linuxserver/ffmpeg " +
                        "  -i /config/music48000.wav " +
                        "  -i /config/" + voiceFileName + " " +
                        "  -filter_complex " +
                        "  \"[1]adelay=500|500[b]; [0][b]amix=2\"" +
                        "  /config/adAudio.wav"
                logger.info("merge >> {}", command)

                builder = ProcessBuilder("cmd.exe", "/c", command)
                builder.redirectErrorStream(true)
                p = builder.start()
                r = BufferedReader(InputStreamReader(p.inputStream))
                while (true) {
                    line = r.readLine()
                    if (line == null) {
                        break
                    }
                    println(line)
                }
            } else {
                FileUtils.copyFile(
                    File(fsPrefix + fsPrefixCopy + File.separator + voiceFileName),
                    File(fsPrefix + fsPrefixCopy + File.separator + "adAudio.wav")
                )
            }

            logger.info("merge >> EXIT")
        } catch (e: Exception) {
            logger.error("Can't merge voice and music", e)
        }
    }

    companion object {
        private const val IS_HARDCODED_TRUE = true
        private const val IS_HARDCODED_FALSE = false
    }
}
