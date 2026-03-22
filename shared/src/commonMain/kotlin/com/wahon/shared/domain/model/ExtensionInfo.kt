package com.wahon.shared.domain.model

data class ExtensionInfo(
    val id: String,
    val name: String,
    val version: Int,
    val iconUrl: String,
    val downloadUrl: String,
    val languages: List<String>,
    val nsfw: Boolean,
    val baseUrl: String,
    val repoUrl: String,
    val installed: Boolean = false,
)
