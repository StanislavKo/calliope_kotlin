package com.calliope.server.controller

import com.calliope.server.model.form.AdGenerateForm
import com.calliope.server.model.web.GenerateResponse
import com.calliope.server.service.advertisement.GenerateService
import com.calliope.server.service.advertisement.GenerationService
import com.calliope.server.service.user.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.security.NoSuchAlgorithmException
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/ad")
class AdController(
    private val userService: UserService,
    private val generateService: GenerateService,
    private val generationService: GenerationService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(
        value = ["/generate"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.POST]
    )
    @Throws(
        NoSuchAlgorithmException::class
    )
    fun adGenerate(
        request: HttpServletRequest,
        @RequestHeader("Authorization") authorizationHeader: String,
        @RequestBody form: @Valid AdGenerateForm
    ): GenerateResponse {
        logger.info("adGenerate, {}, {}", form, request.remoteAddr)
        val user = userService.auth(authorizationHeader)
        val isLocal = "0:0:0:0:0:0:0:1" == request.remoteAddr
        return generateService.generate(user, form, isLocal)
        //        return null;
    }
}