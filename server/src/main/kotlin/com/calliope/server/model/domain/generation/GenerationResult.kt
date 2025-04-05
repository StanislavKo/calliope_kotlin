package com.calliope.server.model.domain.generation

class GenerationResult {
    var artifactUuid: String? = null
    var musicUuid: String? = null
    var musicMood: String? = null
    var musicDuration: Float? = null

    var datePrefix: String? = null

    var voiceUuid: String? = null
    var voiceText: String? = null
    var voiceRate: Float? = null
    var voiceDurationRequested: Float? = null
    var voiceDuration: Float? = null
    var voiceProvider: String? = null
    var voiceName: String? = null

    var voiceHardcodeUuid: String? = null
    var voiceHardcodeDuration: Float? = null
    var voiceHardcodeProvider: String? = null
    var voiceHardcodeName: String? = null

    var bannerUuid: String? = null

    var sampleText: String? = null

    val voiceTexts: MutableList<String> = mutableListOf()
    var sampleSpeechCharacters: Int? = null
    var sampleSpeechDuration: Float? = null
}
