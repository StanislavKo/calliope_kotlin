package com.calliope.server.service.advertisement

import com.calliope.core.mysql.repositories.*
import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.generation.GenerationResult
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.service.ai.OpenaiService
import com.calliope.server.service.balance.BalanceService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.S3Client
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.test.assertEquals

internal class GenerateServiceTest {

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun generateImplVoiceTest() {
        val formIn001: AdGenerateForm = AdGenerateForm()
        formIn001.copies = 1
        formIn001.music = false
        formIn001.banner = false
        formIn001.hardcodeText = null
        formIn001.productDescription = "Advertise trips to Ryazan"
        formIn001.contentLength = 20
        val generationResults: MutableList<GenerationResult> = mutableListOf()
        val datePrefix = "2025-04-01"
        val isLocal = true

        val formIn002: AdGenerateForm = AdGenerateForm()
        formIn002.copies = 1
        formIn002.music = false
        formIn002.banner = false
        formIn002.hardcodeText = null
        formIn002.productDescription = "Advertise trips to Ryazan"
        formIn002.contentLength = 20
        formIn002.language = "en-US"
        formIn002.gender = "male"

        val config: GenerationConfig = GenerationConfig("aws", "Tatyana")
        config.hardcodeVoiceName = null
        config.hardcodeVoiceProvider = null

        val generateGenderLocaleMoodService = mockk<GenerateGenderLocaleMoodService>()
        val formGenderSlot = slot<AdGenerateForm?>()
        every { generateGenderLocaleMoodService.generateGender(formIn001.productDescription, captureNullable(formGenderSlot)) } answers {
            println("118")
            println(formGenderSlot.captured)
            formGenderSlot.captured!!.gender = "male"
            CompletableFuture.completedFuture<Void?>(null)
        }
        val formLocaleSlot = slot<AdGenerateForm>()
        every { generateGenderLocaleMoodService.generateLocale(formIn001.productDescription, capture(formLocaleSlot)) } answers {
            println("118")
            println(formLocaleSlot.captured)
            formLocaleSlot.captured.language = "en-US"
            CompletableFuture.completedFuture<Void?>(null)
        }

        val generateVoiceService = mockk<GenerateVoiceService>()
        every { generateVoiceService.initConfigVoice(formIn002) } returns config
        every { generateVoiceService.generateHardcodeVoice(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(0f)

        val openaiService = mockk<OpenaiService>()
        every { openaiService.generateAdText(any(), any(), any(), any()) } returns "Advertise trips to Ryazan - fixed"

        val durationSetterSlot = slot<Consumer<Float?>>()
        every { generateVoiceService.generateVoice(any(), any(), any(), any(), any(), any(), any(), capture(durationSetterSlot)) } answers {
            durationSetterSlot.captured.accept(20f)
            CompletableFuture.completedFuture(20f)
        }
        val durationSetter2Slot = slot<Consumer<Float?>>()
        every { generateVoiceService.generateVoice2(any(), any(), any(), any(), any(), any(), any(), any(), any(), capture(durationSetter2Slot)) } answers {
            durationSetter2Slot.captured.accept(20f)
            CompletableFuture.completedFuture(20f)
        }

        every { generateGenderLocaleMoodService.generateMood(any(), any(), any(), any()) } returns null

        val generateMusicService = mockk<GenerateMusicService>()
        every { generateMusicService.generateMusicByMood(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(null)

        val generateBannerService = mockk<GenerateBannerService>()
        every { generateBannerService.generateImageDescription(any(), any()) } returns null
        every { generateBannerService.generateBanner(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(null)

        val mapper = mockk<ObjectMapper>()
        val azureVoices = mockk<List<AzureInternalVoice>>()
        val languageSpeeds = mockk<Map<String, LanguageSpeed>>()
        val languageCode2Name: Map<String, String> = mapOf("en-US" to "English")
        val s3Client = mockk<S3Client>()
        val executor: Executor = Executors.newFixedThreadPool(10)
        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()
        val generationRepository = mockk<GenerationRepository>()
        val generationCopyRepository = mockk<GenerationCopyRepository>()
        val artifactRepository = mockk<ArtifactRepository>()
        val balanceService = mockk<BalanceService>()
        val generateCommonService = mockk<GenerateCommonService>()

        val service = GenerateService(
            mapper!!,
            azureVoices!!,
            languageSpeeds!!,
            languageCode2Name!!,
            s3Client!!,
            executor,
            userRepository!!,
            userMessageRepository!!,
            generationRepository!!,
            generationCopyRepository!!,
            artifactRepository!!,
            balanceService!!,
            openaiService,
            generateCommonService!!,
            generateBannerService,
            generateVoiceService,
            generateMusicService,
            generateGenderLocaleMoodService
        )

        service!!.generateImpl(
            formIn001,
            generationResults,
            datePrefix,
            isLocal
        )
    }

    @Test
    fun generateImplBannerTest() {
        val formIn001: AdGenerateForm = AdGenerateForm()
        formIn001.copies = 1
        formIn001.music = false
        formIn001.banner = true
        formIn001.hardcodeText = null
        formIn001.productDescription = "Advertise trips to Ryazan"
        formIn001.contentLength = 20
        val generationResults: MutableList<GenerationResult> = mutableListOf()
        val datePrefix = "2025-04-01"
        val isLocal = true

        val formIn002: AdGenerateForm = AdGenerateForm()
        formIn002.copies = 1
        formIn002.music = false
        formIn002.banner = true
        formIn002.hardcodeText = null
        formIn002.productDescription = "Advertise trips to Ryazan"
        formIn002.contentLength = 20
        formIn002.language = "en-US"
        formIn002.gender = "male"

        val config: GenerationConfig = GenerationConfig("aws", "Tatyana")
        config.hardcodeVoiceName = null
        config.hardcodeVoiceProvider = null

        val generateGenderLocaleMoodService = mockk<GenerateGenderLocaleMoodService>()
        val formGenderSlot = slot<AdGenerateForm?>()
        every { generateGenderLocaleMoodService.generateGender(formIn001.productDescription, captureNullable(formGenderSlot)) } answers {
            println("118")
            println(formGenderSlot.captured)
            formGenderSlot.captured!!.gender = "male"
            CompletableFuture.completedFuture<Void?>(null)
        }
        val formLocaleSlot = slot<AdGenerateForm>()
        every { generateGenderLocaleMoodService.generateLocale(formIn001.productDescription, capture(formLocaleSlot)) } answers {
            println("118")
            println(formLocaleSlot.captured)
            formLocaleSlot.captured.language = "en-US"
            CompletableFuture.completedFuture<Void?>(null)
        }

        val generateVoiceService = mockk<GenerateVoiceService>()
        every { generateVoiceService.initConfigVoice(formIn002) } returns config
        every { generateVoiceService.generateHardcodeVoice(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(0f)

        val openaiService = mockk<OpenaiService>()
        every { openaiService.generateAdText(any(), any(), any(), any()) } returns "Advertise trips to Ryazan - fixed"

        val durationSetterSlot = slot<Consumer<Float?>>()
        every { generateVoiceService.generateVoice(any(), any(), any(), any(), any(), any(), any(), capture(durationSetterSlot)) } answers {
            durationSetterSlot.captured.accept(20f)
            CompletableFuture.completedFuture(20f)
        }
        val durationSetter2Slot = slot<Consumer<Float?>>()
        every { generateVoiceService.generateVoice2(any(), any(), any(), any(), any(), any(), any(), any(), any(), capture(durationSetter2Slot)) } answers {
            durationSetter2Slot.captured.accept(20f)
            CompletableFuture.completedFuture(20f)
        }

        every { generateGenderLocaleMoodService.generateMood(any(), any(), any(), any()) } returns null

        val generateMusicService = mockk<GenerateMusicService>()
        every { generateMusicService.generateMusicByMood(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(null)

        val generateBannerService = mockk<GenerateBannerService>()
        val bannerUuidSetterSlot = slot<Consumer<String?>>()
        every { generateBannerService.generateImageDescription(any(), any()) } returns "Advertise trips to Ryazan - banner fixed"
        every { generateBannerService.generateBanner(any(), any(), any(), any(), any(), any(), any(), capture(bannerUuidSetterSlot)) } answers {
            bannerUuidSetterSlot.captured.accept("1234")
            CompletableFuture.completedFuture(null)
        }

        val mapper = mockk<ObjectMapper>()
        val azureVoices = mockk<List<AzureInternalVoice>>()
        val languageSpeeds = mockk<Map<String, LanguageSpeed>>()
        val languageCode2Name: Map<String, String> = mapOf("en-US" to "English")
        val s3Client = mockk<S3Client>()
        val executor: Executor = Executors.newFixedThreadPool(10)
        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()
        val generationRepository = mockk<GenerationRepository>()
        val generationCopyRepository = mockk<GenerationCopyRepository>()
        val artifactRepository = mockk<ArtifactRepository>()
        val balanceService = mockk<BalanceService>()
        val generateCommonService = mockk<GenerateCommonService>()

        val service = GenerateService(
            mapper!!,
            azureVoices!!,
            languageSpeeds!!,
            languageCode2Name!!,
            s3Client!!,
            executor,
            userRepository!!,
            userMessageRepository!!,
            generationRepository!!,
            generationCopyRepository!!,
            artifactRepository!!,
            balanceService!!,
            openaiService,
            generateCommonService!!,
            generateBannerService,
            generateVoiceService,
            generateMusicService,
            generateGenderLocaleMoodService
        )

        service!!.generateImpl(
            formIn001,
            generationResults,
            datePrefix,
            isLocal
        )

        assertEquals("1234", generationResults.get(0).bannerUuid)
    }

    @Test
    fun generateImplMusicTest() {
        val formIn001: AdGenerateForm = AdGenerateForm()
        formIn001.copies = 1
        formIn001.music = false
        formIn001.banner = false
        formIn001.hardcodeText = null
        formIn001.productDescription = "Advertise trips to Ryazan"
        formIn001.contentLength = 20
        val generationResults: MutableList<GenerationResult> = mutableListOf()
        val datePrefix = "2025-04-01"
        val isLocal = true

        val formIn002: AdGenerateForm = AdGenerateForm()
        formIn002.copies = 1
        formIn002.music = false
        formIn002.banner = false
        formIn002.hardcodeText = null
        formIn002.productDescription = "Advertise trips to Ryazan"
        formIn002.contentLength = 20
        formIn002.language = "en-US"
        formIn002.gender = "male"

        val config: GenerationConfig = GenerationConfig("aws", "Tatyana")
        config.hardcodeVoiceName = null
        config.hardcodeVoiceProvider = null

        val generateGenderLocaleMoodService = mockk<GenerateGenderLocaleMoodService>()
        val formGenderSlot = slot<AdGenerateForm?>()
        every { generateGenderLocaleMoodService.generateGender(formIn001.productDescription, captureNullable(formGenderSlot)) } answers {
            println("118")
            println(formGenderSlot.captured)
            formGenderSlot.captured!!.gender = "male"
            CompletableFuture.completedFuture<Void?>(null)
        }
        val formLocaleSlot = slot<AdGenerateForm>()
        every { generateGenderLocaleMoodService.generateLocale(formIn001.productDescription, capture(formLocaleSlot)) } answers {
            println("118")
            println(formLocaleSlot.captured)
            formLocaleSlot.captured.language = "en-US"
            CompletableFuture.completedFuture<Void?>(null)
        }

        val generateVoiceService = mockk<GenerateVoiceService>()
        every { generateVoiceService.initConfigVoice(formIn002) } returns config
        every { generateVoiceService.generateHardcodeVoice(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(0f)

        val openaiService = mockk<OpenaiService>()
        every { openaiService.generateAdText(any(), any(), any(), any()) } returns "Advertise trips to Ryazan - fixed"

        val durationSetterSlot = slot<Consumer<Float?>>()
        every { generateVoiceService.generateVoice(any(), any(), any(), any(), any(), any(), any(), capture(durationSetterSlot)) } answers {
            durationSetterSlot.captured.accept(20f)
            CompletableFuture.completedFuture(20f)
        }
        val durationSetter2Slot = slot<Consumer<Float?>>()
        every { generateVoiceService.generateVoice2(any(), any(), any(), any(), any(), any(), any(), any(), any(), capture(durationSetter2Slot)) } answers {
            durationSetter2Slot.captured.accept(20f)
            CompletableFuture.completedFuture(20f)
        }

        every { generateGenderLocaleMoodService.generateMood(any(), any(), any(), any()) } returns null

        val generateMusicService = mockk<GenerateMusicService>()
        val musicUuidSetterSlot = slot<Consumer<String?>>()
        every { generateMusicService.generateMusicByMood(any(), any(), any(), any(), any(), any(), any(), capture(musicUuidSetterSlot)) } answers {
            musicUuidSetterSlot.captured.accept("5678")
            CompletableFuture.completedFuture(null)
        }

        val generateBannerService = mockk<GenerateBannerService>()
        every { generateBannerService.generateImageDescription(any(), any()) } returns null
        every { generateBannerService.generateBanner(any(), any(), any(), any(), any(), any(), any(), any()) } returns CompletableFuture.completedFuture(null)

        val mapper = mockk<ObjectMapper>()
        val azureVoices = mockk<List<AzureInternalVoice>>()
        val languageSpeeds = mockk<Map<String, LanguageSpeed>>()
        val languageCode2Name: Map<String, String> = mapOf("en-US" to "English")
        val s3Client = mockk<S3Client>()
        val executor: Executor = Executors.newFixedThreadPool(10)
        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()
        val generationRepository = mockk<GenerationRepository>()
        val generationCopyRepository = mockk<GenerationCopyRepository>()
        val artifactRepository = mockk<ArtifactRepository>()
        val balanceService = mockk<BalanceService>()
        val generateCommonService = mockk<GenerateCommonService>()

        val service = GenerateService(
            mapper!!,
            azureVoices!!,
            languageSpeeds!!,
            languageCode2Name!!,
            s3Client!!,
            executor,
            userRepository!!,
            userMessageRepository!!,
            generationRepository!!,
            generationCopyRepository!!,
            artifactRepository!!,
            balanceService!!,
            openaiService,
            generateCommonService!!,
            generateBannerService,
            generateVoiceService,
            generateMusicService,
            generateGenderLocaleMoodService
        )

        service!!.generateImpl(
            formIn001,
            generationResults,
            datePrefix,
            isLocal
        )

        assertEquals("5678", generationResults.get(0).musicUuid)
    }

}