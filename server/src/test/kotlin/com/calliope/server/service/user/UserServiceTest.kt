package com.calliope.server.service.user

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserMessage
import com.calliope.core.mysql.repositories.*
import com.calliope.server.consts.Consts
import com.calliope.server.exception.CustomErrorException
import com.calliope.server.model.domain.GenerationConfig
import com.calliope.server.model.domain.LanguageSpeed
import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.generation.GenerationResult
import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.model.form.SignupForm
import com.calliope.server.model.view.PageDto
import com.calliope.server.service.advertisement.GenerateService
import com.calliope.server.service.ai.OpenaiService
import com.calliope.server.service.balance.BalanceService
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
import software.amazon.awssdk.services.s3.S3Client
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.test.assertEquals

internal class UserServiceTest {

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun postMessageTest() {
        val user: User = User()
        user.userId = 1
        user.givenName = "John"
        user.familyName = "Reed"
        user.email = "johnreed@gmail.com"
        user.saltPasswordHash = "1234"

        val me: User = User()

        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()

        every { userRepository.findByEmail(Consts.MY_EMAIL) } returns me

        every { userMessageRepository.findByUserIn(listOf<User>(me, user), any()) } returns PageImpl<UserMessage>(listOf(), PageRequest.of(1, 10), 0)

        val userMessageSlot = slot<UserMessage>()
        every { userMessageRepository.save(capture(userMessageSlot)) } answers {
            UserMessage()
        }

        val service = UserService(
            userRepository,
            userMessageRepository
        )

        service.postMessage(user, "text")

        assertEquals(user, userMessageSlot.captured.user)
        assertEquals(Base64.encodeBase64String("text".toByteArray()), userMessageSlot.captured.message)
    }

    @Test
    fun signupFailedTest() {
        val signupForm: SignupForm = SignupForm()
        signupForm.givenName = "John"
        signupForm.familyName = "Reed"
        signupForm.email = "johnreed@gmail.com"
        signupForm.password = "1234"

        val user: User = User()
        user.userId = 1
        user.givenName = "John"
        user.familyName = "Reed"
        user.email = "johnreed@gmail.com"
        user.saltPasswordHash = "1234"

        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()

        every { userRepository.findByEmail("johnreed@gmail.com") } returns user

        val service = UserService(
            userRepository,
            userMessageRepository
        )

        assertThrows<CustomErrorException> { service.signup("1.1.1.1", "2.2.2.2", signupForm) }
    }

    @Test
    fun signupSuccessTest() {
        val signupForm: SignupForm = SignupForm()
        signupForm.givenName = "John"
        signupForm.familyName = "Reed"
        signupForm.email = "johnreed@gmail.com"
        signupForm.password = "1234"

        val user: User = User()
        user.userId = 1
        user.givenName = "John"
        user.familyName = "Reed"
        user.email = "johnreed@gmail.com"
        user.saltPasswordHash = "1234"

        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()

        every { userRepository.findByEmail("johnreed@gmail.com") } returns null

        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers {
            User()
        }

        val service = UserService(
            userRepository,
            userMessageRepository
        )

        val saltPassword = Consts.SALT + "1234"
        val saltPasswordHash = service.md5(saltPassword)
        println("saltPasswordHash = " + saltPasswordHash)

        service.signup("1.1.1.1", "2.2.2.2", signupForm)

        assertEquals("John", userSlot.captured.givenName)
        assertEquals("Reed", userSlot.captured.familyName)
        assertEquals("johnreed@gmail.com", userSlot.captured.email)
        assertEquals(saltPasswordHash, userSlot.captured.saltPasswordHash)
    }

    @Test
    fun authSuccessTest() {
        val user: User = User()
        user.userId = 1
        user.givenName = "John"
        user.familyName = "Reed"
        user.email = "johnreed@gmail.com"
        user.saltPasswordHash = "25d96a1547cc9a61046dff7c8de64694"

        val userRepository = mockk<UserRepository>()
        val userMessageRepository = mockk<UserMessageRepository>()

        every { userRepository.findByEmailAndSaltPasswordHash("johnreed@gmail.com", "25d96a1547cc9a61046dff7c8de64694") } returns user

        val service = UserService(
            userRepository,
            userMessageRepository
        )

        val userOut = service.auth("Basic am9obnJlZWRAZ21haWwuY29tOjEyMzQ=")

        assertEquals("John", userOut.givenName)
        assertEquals("Reed", userOut.familyName)
        assertEquals("johnreed@gmail.com", userOut.email)
        assertEquals("25d96a1547cc9a61046dff7c8de64694", userOut.saltPasswordHash)
    }

}