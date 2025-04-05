package com.calliope.server.exception

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

class CustomError(private val message: String) {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val timestamp: LocalDateTime = LocalDateTime.now()
    var payload: String? = null
    var type: String? = null
    var subErrors: List<CustomSubError>? = null

    constructor(message: String, payload: String?) : this(message) {
        this.payload = payload
    }

//    fun setType(type: String?) {
//        this.type = type
//    }
//
//    fun setSubErrors(subErrors: List<CustomSubError>?) {
//        this.subErrors = subErrors
//    }
}
