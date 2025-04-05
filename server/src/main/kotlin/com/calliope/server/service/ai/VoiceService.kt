package com.calliope.server.service.ai

import com.calliope.server.model.domain.azure.AzureInternalVoice
import com.calliope.server.model.domain.google.GoogleInternalVoice
import com.calliope.server.model.view.KeyValueView
import com.calliope.server.model.view.PageDto
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.polly.model.Voice
import java.util.*
import java.util.stream.Collectors

@Service
class VoiceService(
    private val azureVoices: List<AzureInternalVoice>,
    private val googleVoices: List<GoogleInternalVoice>,
    private val awsVoices: List<Voice>
) {
    fun genders(): PageDto<KeyValueView> {
        val gendersAzure = azureVoices
            .map<AzureInternalVoice, KeyValueView> { v: AzureInternalVoice ->
                KeyValueView(v.gender, v.gender)
            }
        val gendersGoogle = googleVoices
            .map<GoogleInternalVoice, KeyValueView> { v: GoogleInternalVoice ->
                KeyValueView(StringUtils.capitalize(v.ssmlGender?.lowercase()), StringUtils.capitalize(v.ssmlGender?.lowercase()))
            }
        val awsAzure = awsVoices
            .map<Voice, KeyValueView> { v: Voice -> KeyValueView(v.genderAsString(), v.genderAsString()) }
        val gendersSet: MutableSet<KeyValueView> = mutableSetOf()
        gendersSet += gendersAzure
        gendersSet += gendersGoogle
        gendersSet += awsAzure
        var genders: MutableList<KeyValueView> = mutableListOf(*gendersSet.toTypedArray())
        genders.sortWith(kotlin.Comparator.comparing{ keyValueView: KeyValueView -> keyValueView.value!! })
        return PageDto(
            genders,
            1,
            genders.size
        )
    }

    fun languages(): PageDto<KeyValueView> {
        val languagesAzure: MutableSet<KeyValueView> = mutableSetOf()
        languagesAzure.addAll(
            azureVoices
                .map { v: AzureInternalVoice ->
                    KeyValueView(v.locale, v.locale)
                }
                .toSet()
        )
        languagesAzure.addAll(
            azureVoices
                .flatMap { v: AzureInternalVoice ->
                    v.secondaryLocaleList.orEmpty()
                }
                .map { locale: String? -> KeyValueView(locale, locale) }
                .toSet()
        )
        val languagesGoogle: MutableSet<KeyValueView> = mutableSetOf()
        languagesGoogle.addAll(
            googleVoices
                .flatMap { v: GoogleInternalVoice ->
                    v.languageCodes.orEmpty()
                }
                .map { locale: String? -> KeyValueView(locale, locale) }
                .toSet()
        )
        val languagesAws: MutableSet<KeyValueView> = mutableSetOf()
        languagesAws.addAll(
            awsVoices
                .map { v: Voice ->
                    KeyValueView(v.languageCodeAsString(), v.languageCodeAsString())
                }
                .toSet()
        )
        languagesAws.addAll(
            awsVoices
                .flatMap { v: Voice ->
                    v.additionalLanguageCodesAsStrings().orEmpty()
                }
                .map { locale: String? -> KeyValueView(locale, locale) }
                .toSet()
        )
        val languagesSet: MutableSet<KeyValueView> = mutableSetOf()
        languagesSet.addAll(languagesAzure)
        languagesSet.addAll(languagesGoogle)
        languagesSet.addAll(languagesAws)
        var languages: MutableList<KeyValueView> = mutableListOf(*languagesSet.toTypedArray())
        languages.sortWith(kotlin.Comparator.comparing{ keyValueView: KeyValueView -> keyValueView.value!! })
        return PageDto(
            languages,
            1,
            languages.size
        )
    }

    fun languageCodes(): Set<String?> {
        val languagesAzure: MutableSet<String?> = mutableSetOf()
        languagesAzure.addAll(
            azureVoices
                .map { v: AzureInternalVoice -> v.locale }
                .toSet()
        )
        languagesAzure.addAll(
            azureVoices
                .flatMap { v: AzureInternalVoice ->
                    v.secondaryLocaleList.orEmpty()
                }
                .toSet()
        )
        val languagesGoogle: MutableSet<String?> = mutableSetOf()
        languagesGoogle.addAll(
            googleVoices
                .flatMap { v: GoogleInternalVoice ->
                    v.languageCodes.orEmpty()
                }
                .toSet()
        )
        val languagesAws: MutableSet<String?> = mutableSetOf()
        languagesAws.addAll(
            awsVoices
                .map { v: Voice -> v.languageCodeAsString() }
                .toSet()
        )
        languagesAws.addAll(
            awsVoices
                .flatMap { v: Voice ->
                    v.additionalLanguageCodesAsStrings().orEmpty()
                }
                .toSet()
        )
        val languagesSet: MutableSet<String?> = mutableSetOf()
        languagesSet.addAll(languagesAzure)
        languagesSet.addAll(languagesGoogle)
        languagesSet.addAll(languagesAws)
        return languagesSet
    }

    fun styles(
        gender: String?,
        language: String?
    ): PageDto<KeyValueView> {
        val styles: Set<KeyValueView> = mutableSetOf()
        var stylesList = styles.toMutableList()
        stylesList.sortWith(kotlin.Comparator.comparing{ keyValueView: KeyValueView -> keyValueView.value!! })
        return PageDto(
            stylesList.toList(),
            1,
            stylesList.size
        )
    }

    fun hardcodePositions(): PageDto<KeyValueView> {
        val hardcodePositions = listOf(
            KeyValueView("start", "Start"),
            KeyValueView("end", "End")
        )
        return PageDto(
            hardcodePositions,
            1,
            hardcodePositions.size
        )
    }
}
