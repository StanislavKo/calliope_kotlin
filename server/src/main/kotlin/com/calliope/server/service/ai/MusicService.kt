package com.calliope.server.service.ai

import com.calliope.server.model.view.KeyValueView
import com.calliope.server.model.view.PageDto
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class MusicService(
    @param:Qualifier("moods") private val moods: List<String>
) {
    fun moods(): PageDto<KeyValueView> {
        val moods = moods
            .map { mood: String? -> StringUtils.capitalize(mood) }
            .map { mood: String? -> KeyValueView(mood, mood) }
        return PageDto(
            moods,
            1,
            moods.size
        )
    }
}
