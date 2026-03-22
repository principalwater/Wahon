package com.wahon.shared.domain.model

enum class SourceRuntimeKind {
    JAVASCRIPT,
    AIDOKU_AIX,
    UNKNOWN,
}

data class LoadedSource(
    val extensionId: String,
    val sourceId: String,
    val name: String,
    val language: String,
    val supportsNsfw: Boolean,
    val baseUrl: String,
    val localFilePath: String,
    val runtimeKind: SourceRuntimeKind,
    val isRuntimeExecutable: Boolean,
    val runtimeMessage: String? = null,
)
