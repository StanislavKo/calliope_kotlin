package com.calliope.server.controller

import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

@RestController
class HealthController {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(value = ["/health"], method = [RequestMethod.GET])
    fun test(): String {
        logger.info("health")

        return "green"
    }
}