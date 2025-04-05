package com.calliope.server.model.domain.openai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenaiAnswerImageDescription {
    var entitiesAndCharacteristics: String? = null
}
