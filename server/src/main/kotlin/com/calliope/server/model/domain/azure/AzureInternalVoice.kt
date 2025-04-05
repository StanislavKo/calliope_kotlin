package com.calliope.server.model.domain.azure

import com.fasterxml.jackson.annotation.JsonProperty

class AzureInternalVoice {
    @JsonProperty("ShortName")
    var shortName: String? = null

    @JsonProperty("Gender")
    var gender: String? = null

    @JsonProperty("Locale")
    var locale: String? = null

    @JsonProperty("StyleList")
    var styles: List<String>? = null

    @JsonProperty("SecondaryLocaleList")
    var secondaryLocaleList: List<String>? = null

    @JsonProperty("VoiceTag")
    var voiceTag: AzureInternalVoiceTag? = null
}
