package com.calliope.server.model.domain.balance

enum class SpendType(@kotlin.jvm.JvmField val estimationOperation: String) {
    GENERATION_UI("generate"), GENERATION_API("generate")
}
