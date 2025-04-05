package com.calliope.server.configuration

import com.calliope.server.exception.CustomError
import com.calliope.server.exception.CustomErrorException
import com.calliope.server.exception.CustomSubError
import com.calliope.server.exception.CustomValidationError
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.function.Consumer
import mu.KotlinLogging

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(value = [CustomErrorException::class])
    fun handleCustomErrorException(ex: CustomErrorException): ResponseEntity<Any> {
        logger.error("CustomErrorException 1", ex)

        return ResponseEntity(
            ex.customError,
            ex.customErrorCode.status
        )
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val result = ex.bindingResult
        val fieldErrors = result.fieldErrors
        val subErrors: MutableList<CustomSubError> = mutableListOf()
        fieldErrors.forEach(Consumer { fieldError: FieldError ->
            subErrors.add(
                CustomValidationError(
                    null,
                    fieldError.field,
                    fieldError.defaultMessage,
                    fieldError.rejectedValue
                )
            )
        })
        val customError = CustomError("Validation error")
        customError.subErrors = subErrors

        return ResponseEntity(
            customError,
            HttpStatus.BAD_REQUEST
        )
    }
}
