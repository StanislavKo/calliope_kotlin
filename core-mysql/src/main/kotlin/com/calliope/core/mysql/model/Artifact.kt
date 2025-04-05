package com.calliope.core.mysql.model

import com.calliope.core.mysql.model.enums.ArtifactType
import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "artifact")
@NamedQuery(name = "Artifact.findAll", query = "SELECT u FROM Artifact u")
class Artifact : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artifact_id")
    var artifacttId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_copy_id")
    var generationCopy: GenerationCopy? = null

    @Column(columnDefinition = "ENUM('BANNER', 'MUSIC', 'SPEECH', 'HARDCODED', 'CLIP')")
    @Enumerated(EnumType.STRING)
    var type: ArtifactType? = null

    var uuid: String? = null

    @Column(nullable = true)
    var duration: Float? = null

    @Column(nullable = true, columnDefinition = "json")
    var details: String? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Artifact

        return artifacttId == other.artifacttId
    }

    override fun hashCode(): Int {
        return artifacttId ?: 0
    }

}