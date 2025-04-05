package com.calliope.server.exception

class CustomValidationError : CustomSubError {
    constructor(object1: String?, message: String?, field: String?, rejectedValue: Any?) {
        this.object1 = object1
        this.message = message
        this.field = field
        this.rejectedValue = rejectedValue
    }

    var object1: String? = null
    var message: String? = null
    var field: String? = null
    var rejectedValue: Any? = null
}