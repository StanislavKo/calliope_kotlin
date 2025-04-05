package com.calliope.server.controller

import com.calliope.server.model.view.*
import com.calliope.server.service.advertisement.GenerateService
import com.calliope.server.service.advertisement.GenerationService
import com.calliope.server.service.user.UserService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/generations")
class GenerationController(
    private val userService: UserService,
    private val generateService: GenerateService,
    private val generationService: GenerationService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun generations(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<GenerationView> {
        logger.info("generations")
        val user = userService.auth(authorizationHeader)
        return generationService.generations(user)
    }

    @RequestMapping(
        value = ["/{generation_id}/voices"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun generationVoices(
        @RequestHeader("Authorization") authorizationHeader: String,
        @PathVariable(value = "generation_id") generationId: Int
    ): PageDto<GenerationVoiceView> {
        logger.info("generationVoices")
        val user = userService.auth(authorizationHeader)
        return generationService.generationVoices(user, generationId)
    }

    @RequestMapping(
        value = ["/{generation_id}/hardcodes"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun generationHardcodes(
        @RequestHeader("Authorization") authorizationHeader: String,
        @PathVariable(value = "generation_id") generationId: Int
    ): PageDto<GenerationHardcodedView> {
        logger.info("generationHardcodes")
        val user = userService.auth(authorizationHeader)
        return generationService.generationHardcodes(user, generationId)
    }

    @RequestMapping(
        value = ["/{generation_id}/musics"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun generationMusics(
        @RequestHeader("Authorization") authorizationHeader: String,
        @PathVariable(value = "generation_id") generationId: Int
    ): PageDto<GenerationMusicView> {
        logger.info("generationMusics")
        val user = userService.auth(authorizationHeader)
        return generationService.generationMusics(user, generationId)
    }

    @RequestMapping(
        value = ["/{generation_id}/banners"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun generationBanners(
        @RequestHeader("Authorization") authorizationHeader: String,
        @PathVariable(value = "generation_id") generationId: Int
    ): PageDto<GenerationBannerView> {
        logger.info("generationBanners")
        val user = userService.auth(authorizationHeader)
        return generationService.generationBanners(user, generationId)
    }
}