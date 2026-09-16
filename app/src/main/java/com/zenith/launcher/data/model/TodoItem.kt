package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/** One item in the Todo widget. Serializable so the list persists as one JSON blob. */
@Serializable
data class TodoItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
