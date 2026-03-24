package com.wahon.shared.data.local

import java.util.zip.ZipFile

actual class CbzArchiveReader actual constructor() {
    actual fun listPages(archivePath: String): List<CbzPageEntry> {
        val path = archivePath.trim()
        require(path.isNotBlank()) { "CBZ path is blank" }

        return ZipFile(path).use { zip ->
            zip.entries().toList()
                .asSequence()
                .filter { entry -> !entry.isDirectory }
                .map { entry ->
                    val relativePath = entry.name.trimStart('/')
                    CbzPageEntry(
                        relativePath = relativePath,
                        fileName = relativePath.substringAfterLast('/'),
                    )
                }
                .filter { entry -> isSupportedCbzImage(entry.relativePath) }
                .sortedWith(
                    compareBy<CbzPageEntry> { naturalSortKey(it.relativePath) }
                        .thenBy { it.relativePath.lowercase() },
                )
                .toList()
        }
    }

    actual fun readPageBytes(
        archivePath: String,
        relativePath: String,
    ): ByteArray {
        val zipPath = archivePath.trim()
        require(zipPath.isNotBlank()) { "CBZ path is blank" }

        val normalized = relativePath.trim().trimStart('/')
        require(normalized.isNotBlank()) { "CBZ entry path is blank" }

        return ZipFile(zipPath).use { zip ->
            val entry = zip.getEntry(normalized)
                ?: error("CBZ entry not found: $normalized")
            zip.getInputStream(entry).use { input ->
                input.readBytes()
            }
        }
    }
}
