package com.calliope.server.controller

import com.calliope.server.model.view.*
import com.calliope.server.service.ai.YandexService
import com.calliope.server.service.user.UserService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/banner")
class BannerController(
    private val userService: UserService,
    private val yandexService: YandexService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(
        value = ["/aspectRatios"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.GET]
    )
    fun aspectRatios(
        @RequestHeader("Authorization") authorizationHeader: String
    ): PageDto<KeyValueView> {
        logger.info("aspectRatios")
        val user = userService.auth(authorizationHeader)
        return yandexService.aspectRatios()
    }
}