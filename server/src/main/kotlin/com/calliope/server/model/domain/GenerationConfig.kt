package com.calliope.server.model.domain

class GenerationConfig {
    constructor(voiceProvider: String?, voiceName: String?) {
        this.voiceProvider = voiceProvider
        this.voiceName = voiceName
    }

    var voiceProvider: String? = null
    var voiceName: String? = null
    var hardcodeVoiceProvider: String? = null
    var hardcodeVoiceName: String? = null //    String awsVoiceEngine;
    //    String awsHardcodeVoiceEngine;
}
