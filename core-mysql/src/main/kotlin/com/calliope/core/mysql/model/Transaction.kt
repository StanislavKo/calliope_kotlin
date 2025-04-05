package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre_station database table.
 *
 */
@Entity
@Table(name = "transaction")
@NamedQuery(name = "Transaction.findAll", query = "SELECT u FROM Transaction u")
class Transaction : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    var transactionId: Int? = null

    //bi-directional many-to-one association to Genre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    var transactionReceipt: String? = null

    var budgetInput: Double? = null

    var budgetInputCurrecy: String? = null

    var budgetUsd: Double? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        return transactionId == other.transactionId
    }

    override fun hashCode(): Int {
        return transactionId ?: 0
    }

}