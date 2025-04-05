package com.calliope.server.service.advertisement

import com.calliope.server.consts.Consts
import com.calliope.server.service.ai.OpenaiService
import com.calliope.server.service.ai.YandexService
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Consumer
import mu.KotlinLogging

@Service
class GenerateBannerService(
    private val s3Client: S3Client,
    @param:Qualifier("mainExecutor") private val executor: Executor,
    private val generateCommonService: GenerateCommonService,
    private val openaiService: OpenaiService,
    private val yandexService: YandexService
) {
    private val logger = KotlinLogging.logger {}
    @Value("\${advertisement.generation.fs-prefix:}")
    private val fsPrefix: String? = null

    fun generateImageDescription(
        toGenerate: Boolean?,
        description: String?
    ): String? {
        val t1 = System.currentTimeMillis()
        if (toGenerate == false) {
            return null
        }
        val imageDescription = openaiService.generateImageDescription(description)
        val t2 = System.currentTimeMillis()
        logger.info(
            "imageDescription generated in {} milliseconds, is {}",
            (t2 - t1),
            imageDescription
        )
        return imageDescription
    }

    fun generateBanner(
        adText: String?,
        toGenerate: Boolean?,
        aspectRatio: String?,
        uuid: String,
        fsPrefixCopy: String,
        artifactUuid: String,
        datePrefix: String,
        bannerUuidSetter: Consumer<String?>
    ): CompletableFuture<Void> {
        val t1 = System.currentTimeMillis()
        if (toGenerate == false) {
            bannerUuidSetter.accept(null)
            return CompletableFuture.completedFuture<Void>(null)
        }
        try {
            FileUtils.writeStringToFile(
                File(fsPrefix + fsPrefixCopy + File.separator + "adTextBanner.txt"),
                adText,
                Charset.forName("UTF-8")
            )
        } catch (e: IOException) {
        }
        val banner = yandexService.generateBanner(adText!!, aspectRatio)
        if (banner == null) {
            bannerUuidSetter.accept(null)
            return CompletableFuture.completedFuture<Void>(null)
        }
        try {
            FileUtils.writeByteArrayToFile(
                File(fsPrefix + fsPrefixCopy + File.separator + "banner.jpeg"),
                Base64.decodeBase64(banner)
            )
        } catch (e: IOException) {
        }
        s3Client.putObject(
            PutObjectRequest.builder().bucket(Consts.BUCKET).key(
                generateCommonService.getS3Key(
                    "$datePrefix/$artifactUuid/$uuid", "jpeg"
                )
            ).build(), RequestBody.fromBytes(Base64.decodeBase64(banner))
        )
        val t2 = System.currentTimeMillis()
        logger.info("banner generated in {} milliseconds, is {}", (t2 - t1), adText)
        logger.info("generateBanner() EXIT")
        return CompletableFuture.completedFuture<Void>(null)
    }
}
