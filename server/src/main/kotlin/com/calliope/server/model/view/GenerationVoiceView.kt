package com.calliope.server.model.view

class GenerationVoiceView(
    val generationCopyId: Int?,
    val url: String?,
    val provider: String?,
    val name: String?,
    val text: String?
) {
    var order: Int? = null
}
