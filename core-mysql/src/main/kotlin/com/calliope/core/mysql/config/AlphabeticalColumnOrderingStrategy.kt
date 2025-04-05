package com.calliope.core.mysql.config

import org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy
import org.hibernate.cfg.AvailableSettings
import org.hibernate.mapping.Column
import org.hibernate.mapping.Table
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
open class AlphabeticalColumnOrderingStrategy : ColumnOrderingStrategyLegacy(), HibernatePropertiesCustomizer {
    fun orderTableColumns(table: Table, metadata: Metadata?): List<Column> {
        return table.getColumns().stream()
            .sorted(Comparator.comparing(Column::getName))
            .toList()
    }

    override fun customize(hibernateProperties: MutableMap<String?, Any?>) {
        hibernateProperties[AvailableSettings.COLUMN_ORDERING_STRATEGY] = this
    }
}
