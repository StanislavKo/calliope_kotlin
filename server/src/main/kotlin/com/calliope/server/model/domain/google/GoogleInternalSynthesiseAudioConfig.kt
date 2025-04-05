package com.calliope.server.model.domain.google

class GoogleInternalSynthesiseAudioConfig {
    constructor(audioEncoding: String?, speakingRate: Float?) {
        this.audioEncoding = audioEncoding
        this.speakingRate = speakingRate
    }

    var audioEncoding: String? = null
    var speakingRate: Float? = null
}
