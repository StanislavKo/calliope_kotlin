package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "user_balance")
@NamedQuery(name = "UserBalance.findAll", query = "SELECT u FROM UserBalance u")
class UserBalance : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_balance_id")
    var userBalanceId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    var budget: Double? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserBalance

        return userBalanceId == other.userBalanceId
    }

    override fun hashCode(): Int {
        return userBalanceId ?: 0
    }

}