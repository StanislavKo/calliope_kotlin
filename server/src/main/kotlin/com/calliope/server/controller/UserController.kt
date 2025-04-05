package com.calliope.server.controller

import com.calliope.server.model.form.MessageForm
import com.calliope.server.model.form.SignupForm
import com.calliope.server.model.view.*
import com.calliope.server.model.web.BasicResponse
import com.calliope.server.service.user.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.security.NoSuchAlgorithmException
import kotlin.math.min
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/user")
class UserController(
    private val userService: UserService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(
        value = ["/signup"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.POST]
    )
    @Throws(
        NoSuchAlgorithmException::class
    )
    fun signup(
        request: HttpServletRequest,
        @RequestBody form: SignupForm
    ): BasicResponse {
        logger.info("signup")
        val ipAddress = request.remoteAddr
        var ipAddress2 = request.getHeader("x-forwarded-for")
        if (ipAddress2 != null) {
            ipAddress2 = ipAddress2.substring(0, min(100.0, ipAddress2.length.toDouble()).toInt())
        }

        userService.signup(ipAddress, ipAddress2, form)
        return BasicResponse(true)
    }

    @RequestMapping(value = ["/auth"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun auth(
        @RequestHeader("Authorization") authorizationHeader: String
    ): BasicResponse {
        logger.info("auth")
        val user = userService.auth(authorizationHeader)
        return BasicResponse(true)
    }

    @RequestMapping(value = ["/messages"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun messages(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<MessageView> {
        logger.info("messages")
        val user = userService.auth(authorizationHeader)
        return userService.messages(user)
    }

    @RequestMapping(
        value = ["/post"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.POST]
    )
    @Throws(
        NoSuchAlgorithmException::class
    )
    fun postMessage(
        @RequestHeader("Authorization") authorizationHeader: String,
        @RequestBody form: MessageForm
    ): BasicResponse {
        logger.info("postMessage, {}", form.message)
        val user = userService.auth(authorizationHeader)
        userService.postMessage(user, form.message!!)
        return BasicResponse(true)
    }
}