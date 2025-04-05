package com.calliope.server.exception

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.http.HttpStatus

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class CustomErrorCode(val code: Int, val description: String, val status : HttpStatus) {
    SYSTEM(1, "Generic error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN(2, "Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND(3, "Resource not found", HttpStatus.NOT_FOUND),
    BAD_PARAMETER(4, "Bad parameter", HttpStatus.BAD_REQUEST),
    TIMEOUT(5, "Timed out", HttpStatus.REQUEST_TIMEOUT),
    QUOTA_EXCEEDED(6, "Quota exceeded", HttpStatus.BAD_REQUEST);
}
