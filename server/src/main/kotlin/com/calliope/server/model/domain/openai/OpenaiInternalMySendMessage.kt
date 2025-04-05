package com.calliope.server.model.domain.openai

import com.fasterxml.jackson.annotation.JsonProperty

class OpenaiInternalMySendMessage (
    @JsonProperty("assistant_id")
    val assistantId: String? = null,
    val question: String? = null,
    val hint: String? = null,
    val thread: String? = null
)
