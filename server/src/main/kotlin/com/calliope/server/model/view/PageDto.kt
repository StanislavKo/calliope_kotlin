package com.calliope.server.model.view

class PageDto<T> (
    val content: List<T>? = null,
    val page: Int? = null,
    val pageSize: Int? = null
)
