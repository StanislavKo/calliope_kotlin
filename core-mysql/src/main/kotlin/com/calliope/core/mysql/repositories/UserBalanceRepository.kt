package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserBalance
import org.springframework.data.jpa.repository.JpaRepository

interface UserBalanceRepository : JpaRepository<UserBalance?, Int?> {
    fun findByUser(user: User?): UserBalance?
}