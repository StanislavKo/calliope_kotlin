package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "user_balance_usage")
@NamedQuery(name = "UserBalanceUsage.findAll", query = "SELECT u FROM UserBalanceUsage u")
class UserBalanceUsage : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_balance_usage_id")
    var userBalanceUsageId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    var type: String? = null

    var linkedId: String? = null

    @Column(columnDefinition = "TEXT")
    var typeDetails: String? = null

    var price: Double? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserBalanceUsage

        return userBalanceUsageId == other.userBalanceUsageId
    }

    override fun hashCode(): Int {
        return userBalanceUsageId ?: 0
    }

}