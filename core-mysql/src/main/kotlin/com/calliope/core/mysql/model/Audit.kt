package com.calliope.core.mysql.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.sql.Timestamp

@MappedSuperclass
open class Audit {
    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = Timestamp(System.currentTimeMillis())
        updatedAt = Timestamp(System.currentTimeMillis())
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Timestamp(System.currentTimeMillis())
    }
}
