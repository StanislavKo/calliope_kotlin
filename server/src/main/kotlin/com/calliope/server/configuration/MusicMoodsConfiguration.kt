package com.calliope.server.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MusicMoodsConfiguration {
    @Bean(name = ["moods"])
    fun moods(): List<String> {
        return MOODS
    }

    companion object {
        private val MOODS: List<String> = listOf<String>(
            "angry",
            "dark",
            "energetic",
            "epic",
            "euphoric",
            "happy",
            "mysterious",
            "relaxing",
            "romantic",
            "sad",
            "scary",
            "glamorous",
            "uplifting",
            "sentimental"
        )
    }
}
