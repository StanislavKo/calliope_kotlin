package com.calliope.server.model.domain.openai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenaiAnswerAdText {
    var advertisementText: String? = null
}
