package com.calliope.server.controller

import com.calliope.server.model.view.*
import com.calliope.server.service.ai.MusicService
import com.calliope.server.service.user.UserService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/music")
class MusicController(
    private val userService: UserService,
    private val musicService: MusicService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(value = ["/moods"], produces = [MediaType.APPLICATION_JSON_VALUE], method = [RequestMethod.GET])
    fun moods(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("moods")
        val user = userService.auth(authorizationHeader)
        return musicService.moods()
    }
}