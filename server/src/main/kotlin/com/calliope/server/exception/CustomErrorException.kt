package com.calliope.server.exception

class CustomErrorException : RuntimeException {
    val customError: CustomError
    val customErrorCode: CustomErrorCode

    constructor(customError: CustomError, customErrorCode: CustomErrorCode) : super() {
        this.customError = customError
        this.customErrorCode = customErrorCode

        customError.type = customErrorCode.name
    }

    constructor(
        customError: CustomError,
        customErrorCode: CustomErrorCode,
        cause: Throwable?
    ) : super(cause) {
        this.customError = customError
        this.customErrorCode = customErrorCode

        customError.type = customErrorCode.name
    }
}
