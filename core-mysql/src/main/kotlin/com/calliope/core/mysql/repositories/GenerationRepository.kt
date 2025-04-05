package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.Generation
import com.calliope.core.mysql.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface GenerationRepository : JpaRepository<Generation, Int> {
    fun findByUser(user: User, pageable: Pageable?): Page<Generation>
}