package com.calliope.server.model.domain.azure

import com.fasterxml.jackson.annotation.JsonProperty

class AzureInternalVoiceTag {
    @JsonProperty("TailoredScenarios")
    var tailoredScenarios: List<String>? = null

    @JsonProperty("VoicePersonalities")
    var voicePersonalities: List<String>? = null
}
