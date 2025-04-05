package com.calliope.server.model.domain.google

class GoogleInternalSynthesiseVoice {
    constructor(languageCode: String?, name: String?) {
        this.languageCode = languageCode
        this.name = name
    }

    var languageCode: String? = null
    var name: String? = null
}
