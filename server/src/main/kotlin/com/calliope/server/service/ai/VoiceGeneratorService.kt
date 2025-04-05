package com.calliope.server.service.ai

import com.calliope.server.model.form.AdGenerateForm

interface VoiceGeneratorService {
    fun getProvider(): String

    fun getName(form: AdGenerateForm): String?

    fun getHardcodeName(form: AdGenerateForm, hardcodeVoice: String): String?
}
