package com.calliope.server.controller

import com.calliope.server.model.form.OperationPriceForm
import com.calliope.server.model.view.OperationPriceView
import com.calliope.server.service.balance.BalanceService
import com.calliope.server.service.user.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.security.NoSuchAlgorithmException
import mu.KotlinLogging

@RestController
@RequestMapping("/v1/balance")
class BalanceController(
    private val userService: UserService,
    private val balanceService: BalanceService
) {
    private val logger = KotlinLogging.logger {}
    @RequestMapping(
        value = ["/estimate"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        method = [RequestMethod.POST]
    )
    @Throws(
        NoSuchAlgorithmException::class
    )
    fun estimate(
        request: HttpServletRequest,
        @RequestHeader("Authorization") authorizationHeader: String,
        @RequestBody form: OperationPriceForm
    ): OperationPriceView {
        logger.info("estimate, {}, {}", form, request.remoteAddr)
        val user = userService.auth(authorizationHeader)
        val isLocal = "0:0:0:0:0:0:0:1" == request.remoteAddr
        return balanceService.estimate(user, form.operation, form.operationDetails)
        //        return null;
    }
}