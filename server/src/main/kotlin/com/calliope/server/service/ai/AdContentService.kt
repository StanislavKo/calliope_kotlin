package com.calliope.server.service.ai

import com.calliope.server.model.view.KeyValueView
import com.calliope.server.model.view.PageDto
import org.springframework.stereotype.Service

@Service
class AdContentService {
    fun lengths(): PageDto<KeyValueView> {
        val lengths: MutableList<KeyValueView> = mutableListOf()
        lengths.add(KeyValueView("10", "10 seconds"))
        lengths.add(KeyValueView("15", "15 seconds"))
        lengths.add(KeyValueView("20", "20 seconds"))
        lengths.add(KeyValueView("30", "30 seconds"))
        lengths.add(KeyValueView("40", "40 seconds"))
        return PageDto(
            lengths,
            1,
            lengths.size
        )
    }

    fun narratives(): PageDto<KeyValueView> {
        val narratives: MutableList<KeyValueView> = mutableListOf()
        narratives.add(KeyValueView("first", "First Person"))
        narratives.add(KeyValueView("third", "Third Person"))
        return PageDto(
            narratives,
            1,
            narratives.size
        )
    }
}
