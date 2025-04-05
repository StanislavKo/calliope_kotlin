package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "generation")
@NamedQuery(name = "Generation.findAll", query = "SELECT u FROM Generation u")
class Generation : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "generation_id")
    var generationId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    var title: String? = null

    var duration: Int? = null

    var copies: Int? = null

    @Column(name = "date_prefix")
    var datePrefix: String? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Generation

        return generationId == other.generationId
    }

    override fun hashCode(): Int {
        return generationId ?: 0
    }

}