package com.calliope.server.service.user

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserMessage
import com.calliope.core.mysql.repositories.UserMessageRepository
import com.calliope.core.mysql.repositories.UserRepository
import com.calliope.server.consts.Consts
import com.calliope.server.exception.CustomError
import com.calliope.server.exception.CustomErrorCode
import com.calliope.server.exception.CustomErrorException
import com.calliope.server.model.form.SignupForm
import com.calliope.server.model.view.MessageView
import com.calliope.server.model.view.PageDto
import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMessageRepository: UserMessageRepository
) {
    private val logger = KotlinLogging.logger {}
    @Throws(NoSuchAlgorithmException::class)
    fun signup(
        ipAddress: String,
        ipAddress2: String?,
        form: SignupForm
    ) {
        var user = userRepository.findByEmail(form.email!!)
        if (user != null) {
            throw CustomErrorException(
                CustomError("Invalid email"),
                CustomErrorCode.BAD_PARAMETER
            )
        }

        val saltPassword = Consts.SALT + form.password
        val saltPasswordHash = md5(saltPassword)

        user = User()
        user.ip = ipAddress + (if (ipAddress2 != null) ",$ipAddress2" else "")
        user.givenName = form.givenName
        user.familyName = form.familyName
        user.email = form.email
        user.saltPasswordHash = saltPasswordHash

        user = userRepository.save(user)
    }

    fun auth(authorizationHeader: String): User {
        try {
            // Authorization: Basic base64credentials
            val base64Credentials = authorizationHeader.substring("Basic".length).trim { it <= ' ' }
            val credDecoded = Base64.decodeBase64(base64Credentials)
            val credentials = String(credDecoded, StandardCharsets.UTF_8)
            // credentials = username:password
            val values = credentials.split(":".toRegex(), limit = 2).toTypedArray()

            return auth(values[0], values[1])
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw CustomErrorException(
                CustomError("Invalid user 1"),
                CustomErrorCode.BAD_PARAMETER
            )
        }
    }

    fun auth(email: String, password: String): User {
        if (StringUtils.isEmpty(email)) {
            throw CustomErrorException(
                CustomError("Invalid user 2"),
                CustomErrorCode.BAD_PARAMETER
            )
        }

        try {
            val saltPassword = Consts.SALT + password
            val saltPasswordHash = md5(saltPassword)
            val user = userRepository.findByEmailAndSaltPasswordHash(email, saltPasswordHash)
                ?: throw CustomErrorException(
                    CustomError("Invalid user 3"),
                    CustomErrorCode.BAD_PARAMETER
                )
            return user
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw CustomErrorException(
                CustomError("Invalid user 4"),
                CustomErrorCode.BAD_PARAMETER
            )
        }
    }

    fun messages(user: User): PageDto<MessageView> {
        val me = userRepository.findByEmail(Consts.MY_EMAIL)!!

        val meName: String = me.givenName + " " + me.familyName
        val userName = if (StringUtils.isNotEmpty(user.givenName) || StringUtils.isNotEmpty(user.familyName))
            user.givenName.orEmpty() + " " + user.familyName.orEmpty()
        else
            "Вы"

        val messagesDb = userMessageRepository.findByUserIn(
            listOf<User>(me, user),
            PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
        val messagesView = messagesDb!!
            .sortedWith(Comparator.comparing<UserMessage, Timestamp>(UserMessage::createdAt))
            .map<UserMessage, MessageView> { m: UserMessage ->
                MessageView(
                    m.user!!.equals(user),
                    m.message,
                    if (m.user!!.equals(user)) userName else meName,
                    SDF_MESSAGE.format(Date(m.createdAt!!.time))
                )
            }
            .toList()

        for (i in messagesView.indices) {
            messagesView.get(i).order = i + 1
            val messageDecoded: ByteArray = Base64.decodeBase64(messagesView[i].message)
            val messageText = String(messageDecoded, StandardCharsets.UTF_8)
            messagesView[i].message = messageText
        }
        return PageDto(messagesView, 1, messagesView.size)
    }

    fun postMessage(user: User, message: String) {
        try {
            if (StringUtils.isEmpty(message)) {
                throw CustomErrorException(
                    CustomError("Invalid message"),
                    CustomErrorCode.BAD_PARAMETER
                )
            }
            if (message.length > 1024) {
                throw CustomErrorException(
                    CustomError("Too long message"),
                    CustomErrorCode.BAD_PARAMETER
                )
            }

            val me = userRepository.findByEmail(Consts.MY_EMAIL)!!

            val messages = userMessageRepository.findByUserIn(listOf<User>(me, user), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
            if (CollectionUtils.isNotEmpty(messages!!.content)
                && messages.content.size == 10 && messages.content[messages.content.size - 1]!!.createdAt!!.after(
                    Timestamp.from(Instant.now().minusSeconds((10 * 60).toLong()))
                )
            ) {
                throw CustomErrorException(
                    CustomError("Too many messages"),
                    CustomErrorCode.BAD_PARAMETER
                )
            }

            val messageEncoded = Base64.encodeBase64String(message.toByteArray())

            logger.info("user = {}", user)
            var userMessage = UserMessage()
            userMessage.user = user
            userMessage.message = messageEncoded

            userMessage = userMessageRepository.save(userMessage)
        } catch (e: CustomErrorException) {
            throw e
        } catch (e: Exception) {
            logger.error(e.message, e)
            e.printStackTrace()
            throw CustomErrorException(
                CustomError("Generic error"),
                CustomErrorCode.BAD_PARAMETER
            )
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun md5(text: String): String {
        val m = MessageDigest.getInstance("MD5")
        m.reset()
        m.update(text.toByteArray())
        val digest = m.digest()
        val bigInt = BigInteger(1, digest)
        var hashtext = bigInt.toString(16)
        // Now we need to zero pad it if you actually want the full 32 chars.
        while (hashtext.length < 32) {
            hashtext = "0$hashtext"
        }
        return hashtext
    }

    companion object {
        private val SDF_MESSAGE = SimpleDateFormat("EEE, d MMM yyyy HH:mm")
    }
}
