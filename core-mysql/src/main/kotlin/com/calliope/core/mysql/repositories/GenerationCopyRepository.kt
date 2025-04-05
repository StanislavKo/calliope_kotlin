package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.Generation
import com.calliope.core.mysql.model.GenerationCopy
import org.springframework.data.jpa.repository.JpaRepository

interface GenerationCopyRepository : JpaRepository<GenerationCopy?, Int?> {
    fun findByGeneration(generation: Generation?): List<GenerationCopy?>?
}