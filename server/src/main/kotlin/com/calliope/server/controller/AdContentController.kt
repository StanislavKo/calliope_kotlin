package com.calliope.server.controller

import com.calliope.server.model.view.*
import com.calliope.server.service.ai.AdContentService
import com.calliope.server.service.user.UserService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/content")
class AdContentController(
    private val userService: UserService,
    private val adContentService: AdContentService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(value = ["/lengths"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun lengths(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("lengths")
        val user = userService.auth(authorizationHeader)
        return adContentService.lengths()
    }

    @RequestMapping(
        value = ["/narratives"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun narratives(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("narratives")
        val user = userService.auth(authorizationHeader)
        return adContentService.narratives()
    }
}