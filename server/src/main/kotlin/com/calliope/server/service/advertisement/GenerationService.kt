package com.calliope.server.service.advertisement

import com.calliope.core.mysql.model.Artifact
import com.calliope.core.mysql.model.Generation
import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.enums.ArtifactType
import com.calliope.core.mysql.repositories.ArtifactRepository
import com.calliope.core.mysql.repositories.GenerationCopyRepository
import com.calliope.core.mysql.repositories.GenerationRepository
import com.calliope.server.model.domain.generation.ArtifactHardcodeDetails
import com.calliope.server.model.domain.generation.ArtifactMusicDetails
import com.calliope.server.model.domain.generation.ArtifactSpeechDetails
import com.calliope.server.model.view.*
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors

@Service
class GenerationService(
    private val mapper: ObjectMapper,
    private val generationRepository: GenerationRepository,
    private val generationCopyRepository: GenerationCopyRepository,
    private val artifactRepository: ArtifactRepository
) {
    fun generations(user: User): PageDto<GenerationView> {
        val generationsDb: Page<Generation> =
            generationRepository.findByUser(user, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))

        val generationsView: List<GenerationView> = generationsDb
            .map<GenerationView> { m: Generation ->
                GenerationView(
                    m.generationId,
                    m.title,
                    m.copies,
                    m.duration,
                    SDF_MESSAGE.format(Date(m.createdAt!!.time))
                )
            }
            .toList()

        for (i in generationsView.indices) {
            generationsView.get(i).order = i + 1
        }
        return PageDto(generationsView, 1, generationsView.size)
    }

    fun generationVoices(user: User?, generationId: Int): PageDto<GenerationVoiceView> {
        val generationDb = generationRepository.findById(generationId).get()
        val voicesDb = artifactRepository.findByGenerationAndType(generationDb, ArtifactType.SPEECH)

        val voicesView = voicesDb
            .sortedWith(Comparator.comparing<Artifact, Timestamp>(Artifact::createdAt))
            .map<Artifact, GenerationVoiceView> { m: Artifact ->
                try {
                    var details: ArtifactSpeechDetails? = null
                    if (m.details != null) {
                        details = mapper.readValue(m.details, ArtifactSpeechDetails::class.java)
                    }
                    return@map GenerationVoiceView(
                        m.generationCopy!!.generationCopyId,
                       "https://calliope.cerebro1.com/" + m.generationCopy!!.generation!!.datePrefix + "/" + m.generationCopy!!.uuid + "/" + m.uuid + ".wav",
                        details?.voiceProvider.orEmpty(),
                        details?.voiceName.orEmpty(),
                        details?.voiceText.orEmpty(),
                        )
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
            }
            .toList()

        for (i in voicesView.indices) {
            voicesView.get(i).order = i + 1
        }
        return PageDto(voicesView, 1, voicesView.size)
    }

    fun generationHardcodes(user: User?, generationId: Int): PageDto<GenerationHardcodedView> {
        val generationDb = generationRepository.findById(generationId).get()
        val hardcodesDb = artifactRepository.findByGenerationAndType(generationDb, ArtifactType.HARDCODED)

        val hardcodesView = hardcodesDb
            .sortedWith(Comparator.comparing<Artifact, Timestamp>(Artifact::createdAt))
            .map<Artifact, GenerationHardcodedView> { m: Artifact ->
                try {
                    var details: ArtifactHardcodeDetails? = null
                    if (m.details != null) {
                        details = mapper.readValue(m.details, ArtifactHardcodeDetails::class.java)
                    }
                    return@map GenerationHardcodedView(
                        m.generationCopy!!.generationCopyId,
                        "https://calliope.cerebro1.com/" + m.generationCopy!!.generation!!.datePrefix + "/" + m.generationCopy!!.uuid + "/" + m.uuid + ".wav",
                        details?.voiceHardcodeProvider.orEmpty(),
                        details?.voiceHardcodeName.orEmpty()
                    )
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
            }
            .toList()

        for (i in hardcodesView.indices) {
            hardcodesView.get(i).order = i + 1
        }
        return PageDto(hardcodesView, 1, hardcodesView.size)
    }

    fun generationMusics(user: User?, generationId: Int): PageDto<GenerationMusicView> {
        val generationDb = generationRepository.findById(generationId).get()
        val musicsDb = artifactRepository.findByGenerationAndType(generationDb, ArtifactType.MUSIC)

        val musicsView = musicsDb
            .sortedWith(Comparator.comparing<Artifact, Timestamp>(Artifact::createdAt))
            .map<Artifact, GenerationMusicView> { m: Artifact ->
                try {
                    var details: ArtifactMusicDetails? = null
                    if (m.details != null) {
                        details = mapper.readValue(m.details, ArtifactMusicDetails::class.java)
                    }
                    return@map GenerationMusicView(
                        m.generationCopy!!.generationCopyId,
                        "https://calliope.cerebro1.com/" + m.generationCopy!!.generation!!.datePrefix + "/" + m.generationCopy!!.uuid + "/" + m.uuid + ".wav",
                        details?.musicMood.orEmpty()
                    )
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
            }
            .toList()

        for (i in musicsView.indices) {
            musicsView.get(i).order = i + 1
        }
        return PageDto(musicsView, 1, musicsView.size)
    }

    fun generationBanners(user: User?, generationId: Int): PageDto<GenerationBannerView> {
        val generationDb = generationRepository.findById(generationId).get()
        val bannersDb = artifactRepository.findByGenerationAndType(generationDb, ArtifactType.BANNER)

        val bannersView = bannersDb
            .sortedWith(Comparator.comparing<Artifact, Timestamp>(Artifact::createdAt))
            .map<Artifact, GenerationBannerView> { m: Artifact ->
                GenerationBannerView(
                    m.generationCopy!!.generationCopyId,
                    "https://calliope.cerebro1.com/" + m.generationCopy!!.generation!!.datePrefix + "/" + m.generationCopy!!.uuid + "/" + m.uuid + ".jpeg"
                )
            }
            .toList()

        for (i in bannersView.indices) {
            bannersView.get(i).order = i + 1
        }
        return PageDto(bannersView, 1, bannersView.size)
    }

    companion object {
        private val SDF_MESSAGE = SimpleDateFormat("EEE, d MMM yyyy HH:mm")
    }
}
