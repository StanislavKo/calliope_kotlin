package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.Transaction
import com.calliope.core.mysql.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<Transaction?, Int?> {
    fun findByUser(user: User?): List<Transaction?>?
}