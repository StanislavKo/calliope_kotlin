package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserUsage
import org.springframework.data.jpa.repository.JpaRepository

interface UserUsageRepository : JpaRepository<UserUsage?, Int?> {
    fun findByUserIn(users: List<User?>?): List<UserUsage?>?
}