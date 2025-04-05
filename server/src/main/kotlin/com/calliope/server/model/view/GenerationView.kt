package com.calliope.server.model.view

class GenerationView(
    val generationId: Int?,
    val summary: String?,
    val copies: Int?,
    val duration: Int?,
    val time: String?
) {
    var order: Int? = null
}
