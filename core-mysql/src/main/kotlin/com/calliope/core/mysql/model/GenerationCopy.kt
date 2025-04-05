package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "generation_copy")
@NamedQuery(name = "GenerationCopy.findAll", query = "SELECT u FROM GenerationCopy u")
class GenerationCopy : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "generation_copy_id")
    var generationCopyId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    var generation: Generation? = null

    var uuid: String? = null

    @Column(name = "order_num")
    var orderNum: Int? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenerationCopy

        return generationCopyId == other.generationCopyId
    }

    override fun hashCode(): Int {
        return generationCopyId ?: 0
    }

}