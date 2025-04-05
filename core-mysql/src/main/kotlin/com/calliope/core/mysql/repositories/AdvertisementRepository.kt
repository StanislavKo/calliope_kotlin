package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.Advertisement
import com.calliope.core.mysql.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AdvertisementRepository : JpaRepository<Advertisement?, Int?> {
    fun findByUser(user: User?, pageable: Pageable?): Page<Advertisement?>?
}