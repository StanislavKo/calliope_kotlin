package com.calliope.server.service.balance

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserMessage
import com.calliope.core.mysql.repositories.*
import com.calliope.server.configuration.RestConfiguration
import com.calliope.server.consts.Consts
import com.calliope.server.exception.CustomErrorException
import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.generation.GenerationResult
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.model.form.SignupForm
import com.calliope.server.model.view.OperationPriceView
import com.calliope.server.model.view.PageDto
import com.calliope.server.service.advertisement.GenerateService
import com.calliope.server.service.ai.OpenaiService
import com.calliope.server.service.balance.BalanceService
import com.calliope.server.service.user.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import software.amazon.awssdk.services.s3.S3Client
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.test.assertEquals

internal class BalanceServiceTest {

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun estimate1CopyTest() {
        val user: User = User()
        user.userId = 1
        user.givenName = "John"
        user.familyName = "Reed"
        user.email = "johnreed@gmail.com"
        user.saltPasswordHash = "25d96a1547cc9a61046dff7c8de64694"

        val operationDetails = "{\"seconds\":20,\"music\":true,\"banner\":true,\"copies\":1}"

        val mapper = RestConfiguration().objectMapper(Jackson2ObjectMapperBuilder())
        val userRepository = mockk<UserRepository>()
        val userBalanceRepository = mockk<UserBalanceRepository>()
        val userBalanceUsageRepository = mockk<UserBalanceUsageRepository>()

        every { userRepository.findByEmailAndSaltPasswordHash("johnreed@gmail.com", "25d96a1547cc9a61046dff7c8de64694") } returns user

        val service = BalanceService(
            mapper,
            userRepository,
            userBalanceRepository,
            userBalanceUsageRepository
        )

        val response: OperationPriceView = service.estimate(user, "generate", operationDetails)

        assertEquals(0.09, response.price!!, 0.000001)
        assertEquals("0.09", response.priceFormatted)
        assertEquals("Basic price: \$0.03<br/>Background music price: \$0.04<br/>Banner price: \$0.02", response.description)
    }

    @Test
    fun estimate2CopiesTest() {
        val user: User = User()
        user.userId = 1
        user.givenName = "John"
        user.familyName = "Reed"
        user.email = "johnreed@gmail.com"
        user.saltPasswordHash = "25d96a1547cc9a61046dff7c8de64694"

        val operationDetails = "{\"seconds\":20,\"music\":true,\"banner\":true,\"copies\":2}"

        val mapper = RestConfiguration().objectMapper(Jackson2ObjectMapperBuilder())
        val userRepository = mockk<UserRepository>()
        val userBalanceRepository = mockk<UserBalanceRepository>()
        val userBalanceUsageRepository = mockk<UserBalanceUsageRepository>()

        every { userRepository.findByEmailAndSaltPasswordHash("johnreed@gmail.com", "25d96a1547cc9a61046dff7c8de64694") } returns user

        val service = BalanceService(
            mapper,
            userRepository,
            userBalanceRepository,
            userBalanceUsageRepository
        )

        val response: OperationPriceView = service.estimate(user, "generate", operationDetails)

        assertEquals(0.18, response.price!!, 0.000001)
        assertEquals("0.18", response.priceFormatted)
        assertEquals(
            "Basic price: \$0.03 * 2 copies = \$0.06<br/>Background music price: \$0.04 * 2 copies = \$0.08<br/>Banner price: \$0.02 * 2 copies = \$0.04",
            response.description
        )
    }

}