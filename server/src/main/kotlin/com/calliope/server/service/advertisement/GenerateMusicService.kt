package com.calliope.server.service.advertisement

import com.calliope.core.mysql.repositories.ArtifactRepository
import com.calliope.core.mysql.repositories.GenerationRepository
import com.calliope.core.mysql.repositories.UserMessageRepository
import com.calliope.core.mysql.repositories.UserRepository
import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.service.ai.*
import com.calliope.server.service.balance.BalanceService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import mu.KotlinLogging

@Service
class GenerateMusicService(
    private val mapper: ObjectMapper,
    private val azureVoices: List<AzureInternalVoice>,
    private val languageSpeeds: Map<String, LanguageSpeed>,
    private val s3Client: S3Client,
    private val userRepository: UserRepository,
    private val userMessageRepository: UserMessageRepository,
    private val generationRepository: GenerationRepository,
    private val artifactRepository: ArtifactRepository,
    private val balanceService: BalanceService,
    private val openaiService: OpenaiService,
    private val generateCommonService: GenerateCommonService,
    private val azureService: AzureService,
    private val googleService: GoogleService,
    private val awsService: AwsService,
    private val yandexService: YandexService
) {
    private val logger = KotlinLogging.logger {}
    @Value("\${advertisement.generation.fs-prefix:}")
    private val fsPrefix: String? = null

    @Value("\${advertisement.generation.docker-fs-prefix:}")
    private val dockerFsPrefix: String? = null

    fun generateMusicByMood(
        duration: Int,
        mood: String?,
        toGenerate: Boolean?,
        musicUuid: String,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        uuidSetter: Consumer<String?>
    ): CompletableFuture<Void?> {
        if (toGenerate == false) {
            uuidSetter.accept(null)
            return CompletableFuture.completedFuture(null)
        }

        try {
            generateMusic(duration, mood, musicUuid, fsPrefixCopy, artifactUuid, datePrefix)
        } catch (e: IOException) {
            logger.error("Can't generate music", e)
        }

        logger.info("generateMusicByMood() EXIT")
        return CompletableFuture.completedFuture(null)
    }

    @Throws(IOException::class)
    fun generateMusic(
        duration: Int,
        taskStr: String?,
        musicUuid: String,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String
    ) {
        val path_s3 = "$datePrefix/$artifactUuid/$musicUuid"
        val command = "docker run " +
                "-v " + dockerFsPrefix + "/" + fsPrefixCopy + "://config " +
                "-e MODAL_MUSICGEN_PHRASE=\"" + taskStr + "\" " +
                "-e MODAL_MUSICGEN_DURATION=\"" + duration + "\" " +
                "-e MODAL_MUSICGEN_OUT_S3=\"" + path_s3 + "\" " +
                "-e MODAL_MUSICGEN_OUT_FS=\"/config/music1.wav\" " +
                "calliope_python_lambda_modal_audiocraft_large"
        logger.info(command)
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
    }
}
