package com.calliope.server.model.domain.openai

import com.fasterxml.jackson.annotation.JsonProperty

class OpenaiInternalRun {
    @JsonProperty("assistant_id")
    var assistantId: String? = null
}
