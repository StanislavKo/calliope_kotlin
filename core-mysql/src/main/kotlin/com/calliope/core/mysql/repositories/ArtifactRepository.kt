package com.calliope.core.mysql.repositories

import com.calliope.core.mysql.model.Artifact
import com.calliope.core.mysql.model.Generation
import com.calliope.core.mysql.model.enums.ArtifactType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArtifactRepository : JpaRepository<Artifact, Int> {
    @Query("select a from Artifact a where a.generationCopy.generation=:generation and a.type=:type")
    fun findByGenerationAndType(generation: Generation, type: ArtifactType): List<Artifact>
}