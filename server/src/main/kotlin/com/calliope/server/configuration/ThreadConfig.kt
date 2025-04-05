package com.calliope.server.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Configuration
class ThreadConfig {
    @Bean(name = ["mainExecutor"])
    fun mainExecutor(): Executor {
        val executor: Executor = Executors.newFixedThreadPool(50)
        return executor
    }
}
