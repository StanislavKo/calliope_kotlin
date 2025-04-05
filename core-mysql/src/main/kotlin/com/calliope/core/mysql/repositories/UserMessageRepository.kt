package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.User
import com.calliope.core.mysql.model.UserMessage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserMessageRepository : JpaRepository<UserMessage, Int> {
    fun findByUserIn(users: List<User>, pageable: Pageable): Page<UserMessage>
}