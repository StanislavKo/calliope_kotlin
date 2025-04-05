package com.calliope.server.model.domain.openai

import com.fasterxml.jackson.annotation.JsonProperty

class OpenaiInternalMyAnswer {
    var answer: String? = null

    @JsonProperty("thread_id")
    var threadId: String? = null
}
