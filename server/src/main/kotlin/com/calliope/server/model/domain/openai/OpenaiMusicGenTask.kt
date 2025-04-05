package com.calliope.server.model.domain.openai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenaiMusicGenTask {
    var countries: List<String>? = null
    var genres: List<String>? = null
    var moods: List<String>? = null
    var instruments: List<String>? = null
}
