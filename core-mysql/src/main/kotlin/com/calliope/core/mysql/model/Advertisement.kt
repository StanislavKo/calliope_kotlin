package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "advertisement")
@NamedQuery(name = "Advertisement.findAll", query = "SELECT u FROM Advertisement u")
class Advertisement : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advertisement_id")
    var advertisementId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifactt_id")
    var artifact: Artifact? = null

    var speechUuid: String? = null

    var hardcodeUuid: String? = null

    var clipUuid: String? = null

    var musicUuid: String? = null

    var bannerUuid: String? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Advertisement

        return advertisementId == other.advertisementId
    }

    override fun hashCode(): Int {
        return advertisementId ?: 0
    }
}