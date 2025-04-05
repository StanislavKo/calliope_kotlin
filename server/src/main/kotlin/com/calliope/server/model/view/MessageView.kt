package com.calliope.server.model.view

class MessageView(
    var isClient: Boolean?,
    var name: String?,
    var message: String?,
    var time: String?
) {
    var order: Int? = null
}
