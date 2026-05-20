package com.klogviewer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
)
