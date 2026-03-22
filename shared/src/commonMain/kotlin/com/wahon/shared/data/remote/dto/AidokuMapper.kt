package com.wahon.shared.data.remote.dto

import com.wahon.shared.domain.model.ExtensionInfo
import com.wahon.shared.domain.model.ExtensionRepo

fun SourceListDto.toExtensionRepo(url: String): ExtensionRepo {
    return ExtensionRepo(
        url = url,
        name = name,
    )
}

fun SourceEntryDto.toExtensionInfo(repoBaseUrl: String): ExtensionInfo {
    val base = repoBaseUrl.trimEnd('/')
    return ExtensionInfo(
        id = id,
        name = name,
        version = version,
        iconUrl = if (iconUrl.startsWith("http")) iconUrl else "$base/$iconUrl",
        downloadUrl = if (downloadUrl.startsWith("http")) downloadUrl else "$base/$downloadUrl",
        languages = languages,
        nsfw = contentRating >= 2,
        baseUrl = baseUrl,
        repoUrl = repoBaseUrl,
    )
}
