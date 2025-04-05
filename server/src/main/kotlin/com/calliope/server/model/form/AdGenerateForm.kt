package com.calliope.server.model.form

import com.google.gson.Gson
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

class AdGenerateForm {
    var productName: String? = null
    var projectName: String? = null
    var productDescription: @NotNull String? = null
    var voiceProvider: String? = null
    var contentLength: @NotNull Int? = null
    var contentNarrative: @NotNull String? = null
    var gender: String? = null
    var language: String? = null
    var style: String? = null
    var styleDegree: Double? = null
    var scenary: String? = null
    var personality: String? = null

    var music: @NotNull Boolean? = null
    var mood: String? = null

    var banner: @NotNull Boolean? = null
    var aspectRatio: String? = null

    var hardcodePosition: String? = null
    var hardcodeText: String? = null
    var hardcodeSpeed: Float? = null
    var hardcodeVoice: String? = null

    var copies: @NotNull @Min(value = 1) @Max(value = 5) Int? = null

    fun clone(): AdGenerateForm
    {
        val stringAnimal = Gson().toJson(this, AdGenerateForm::class.java)
        return Gson().fromJson<AdGenerateForm>(stringAnimal, AdGenerateForm::class.java)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdGenerateForm

        if (contentLength != other.contentLength) return false
        if (styleDegree != other.styleDegree) return false
        if (music != other.music) return false
        if (banner != other.banner) return false
        if (hardcodeSpeed != other.hardcodeSpeed) return false
        if (copies != other.copies) return false
        if (productName != other.productName) return false
        if (projectName != other.projectName) return false
        if (productDescription != other.productDescription) return false
        if (voiceProvider != other.voiceProvider) return false
        if (contentNarrative != other.contentNarrative) return false
        if (gender != other.gender) return false
        if (language != other.language) return false
        if (style != other.style) return false
        if (scenary != other.scenary) return false
        if (personality != other.personality) return false
        if (mood != other.mood) return false
        if (aspectRatio != other.aspectRatio) return false
        if (hardcodePosition != other.hardcodePosition) return false
        if (hardcodeText != other.hardcodeText) return false
        if (hardcodeVoice != other.hardcodeVoice) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentLength ?: 0
        result = 31 * result + (styleDegree?.hashCode() ?: 0)
        result = 31 * result + (music?.hashCode() ?: 0)
        result = 31 * result + (banner?.hashCode() ?: 0)
        result = 31 * result + (hardcodeSpeed?.hashCode() ?: 0)
        result = 31 * result + (copies ?: 0)
        result = 31 * result + (productName?.hashCode() ?: 0)
        result = 31 * result + (projectName?.hashCode() ?: 0)
        result = 31 * result + (productDescription?.hashCode() ?: 0)
        result = 31 * result + (voiceProvider?.hashCode() ?: 0)
        result = 31 * result + (contentNarrative?.hashCode() ?: 0)
        result = 31 * result + (gender?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (style?.hashCode() ?: 0)
        result = 31 * result + (scenary?.hashCode() ?: 0)
        result = 31 * result + (personality?.hashCode() ?: 0)
        result = 31 * result + (mood?.hashCode() ?: 0)
        result = 31 * result + (aspectRatio?.hashCode() ?: 0)
        result = 31 * result + (hardcodePosition?.hashCode() ?: 0)
        result = 31 * result + (hardcodeText?.hashCode() ?: 0)
        result = 31 * result + (hardcodeVoice?.hashCode() ?: 0)
        return result
    }

}
