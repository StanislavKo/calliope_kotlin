package com.calliope.server.application

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("com.calliope.core.mysql")
@ComponentScan(basePackages = ["com.calliope.core.mysql", "com.calliope.server"])
@EntityScan(basePackages = ["com.calliope.core.mysql.model"])
class CalliopeServerApplication : CommandLineRunner {
    @Throws(Exception::class)
    override fun run(vararg args: String) {
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CalliopeServerApplication::class.java, *args)
        }
    }
}
