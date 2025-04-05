package com.calliope.server.controller

import com.calliope.server.model.view.*
import com.calliope.server.service.ai.VoiceService
import com.calliope.server.service.user.UserService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/voice")
class VoiceController(
    private val userService: UserService,
    private val voiceService: VoiceService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(value = ["/genders"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun genders(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("genders")
        val user = userService.auth(authorizationHeader)
        return voiceService.genders()
    }

    @RequestMapping(value = ["/languages"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun languages(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("languages")
        val user = userService.auth(authorizationHeader)
        return voiceService.languages()
    }

    @RequestMapping(value = ["/styles"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun styles(
        @RequestHeader("Authorization") authorizationHeader: String,
        @RequestParam(required = false) gender: String?,
        @RequestParam(required = false) language: String?
    ): PageDto<KeyValueView> {
        logger.info("styles")
        val user = userService.auth(authorizationHeader)
        return voiceService.styles(gender, language)
    }

    @RequestMapping(
        value = ["/hardcodePositions"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun hardcodePositions(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("hardcodePositions")
        val user = userService.auth(authorizationHeader)
        return voiceService.hardcodePositions()
    }
}