package com.calliope.server.service.advertisement

import com.calliope.core.mysql.repositories.*
import com.calliope.server.configuration.RestConfiguration
import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.generation.GenerationResult
import com.calliope.server.model.domain.yandex.AspectRatio
import com.calliope.server.model.domain.yandex.YandexKey
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.service.ai.OpenaiService
import com.calliope.server.service.ai.YandexService
import com.calliope.server.service.balance.BalanceService
import com.calliope.server.service.user.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.test.assertEquals

internal class GenerateBannerTest {

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun generateImplVoiceTest() {
        val mapper = RestConfiguration().objectMapper(Jackson2ObjectMapperBuilder())
        val restTemplate = RestConfiguration().restTemplate(mapper)
        val yandexService = spyk(
            YandexService(
                mapper,
                restTemplate,
                YandexKey(),
                listOf(AspectRatio(1, 1, "1x1"))
            ),
            recordPrivateCalls = true
        )

        val s3Client = mockk<S3Client>()
        val executor: Executor = Executors.newFixedThreadPool(10)
        val generateCommonService = mockk<GenerateCommonService>()
        val openaiService = mockk<OpenaiService>()

        //justRun { yandexService invoke "generateBanner" }
        every { yandexService invokeNoArgs "authJWT" } returns "111"
        every { yandexService invoke "authIAMToken" withArguments listOf("111") } returns "222"
        every { yandexService invoke "generateBannerImpl" withArguments listOf("222", "Advertise trips to Ryazan", 1, 1) } returns "333"
        every { yandexService invoke "downloadImage" withArguments listOf("222", "333") } returns "444"

        every { s3Client.putObject(any(PutObjectRequest::class), any(RequestBody::class)) } returns null
        every { generateCommonService.getS3Key(any(), any()) } returns "asdf"

        val service = GenerateBannerService(
            s3Client,
            executor,
            generateCommonService,
            openaiService,
            yandexService
        )

        service.generateBanner(
            "Advertise trips to Ryazan",
            true,
            "1x1",
            "uuid",
            "fsPrefix",
            "artifactUuid",
            "datePrefix",
            { bannerUuidNew: String? ->  }
        )

        verifySequence {
            yandexService.generateBanner("Advertise trips to Ryazan", "1x1")
            yandexService["authJWT"]()
            yandexService["authIAMToken"]("111")
            yandexService["generateBannerImpl"]("222", "Advertise trips to Ryazan", 1, 1)
            yandexService["downloadImage"]("222", "333")
        }
    }

}