package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserBalanceUsage
import org.springframework.data.jpa.repository.JpaRepository

interface UserBalanceUsageRepository : JpaRepository<UserBalanceUsage?, Int?> {
    fun findByUserIn(users: List<User?>?): List<UserBalanceUsage?>?
}