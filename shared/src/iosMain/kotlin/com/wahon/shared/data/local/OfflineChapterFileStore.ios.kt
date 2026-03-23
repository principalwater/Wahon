package com.wahon.shared.data.local

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains

actual class OfflineChapterFileStore {
    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val baseDir: Path
        get() = (applicationSupportDirPath() / OFFLINE_DOWNLOADS_DIR_NAME)

    actual fun savePage(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
        pageIndex: Int,
        imageUrl: String,
        payload: ByteArray,
    ): OfflinePageArtifact {
        require(payload.isNotEmpty()) { "Downloaded page payload is empty" }

        val prefix = chapterDownloadPrefix(
            sourceId = sourceId,
            mangaUrl = mangaUrl,
            chapterUrl = chapterUrl,
        )
        val extension = imageExtensionFromUrl(imageUrl)
        val fileName = "$prefix-p${pageIndex.toString().padStart(4, '0')}.$extension"
        val absolutePath = baseDir / fileName

        fileSystem.createDirectories(baseDir)
        fileSystem.write(absolutePath) {
            write(payload)
        }

        return OfflinePageArtifact(
            relativePath = fileName,
            sizeBytes = payload.size.toLong(),
        )
    }

    actual fun deleteChapter(
        sourceId: String,
        mangaUrl: String,
        chapterUrl: String,
    ) {
        if (!fileSystem.exists(baseDir)) return
        val prefix = chapterDownloadPrefix(
            sourceId = sourceId,
            mangaUrl = mangaUrl,
            chapterUrl = chapterUrl,
        )
        fileSystem.list(baseDir)
            .filter { path -> path.name.startsWith("$prefix-p") }
            .forEach { path -> fileSystem.delete(path) }
    }

    actual fun deleteManga(
        sourceId: String,
        mangaUrl: String,
    ) {
        if (!fileSystem.exists(baseDir)) return
        val prefix = mangaDownloadPrefix(
            sourceId = sourceId,
            mangaUrl = mangaUrl,
        )
        fileSystem.list(baseDir)
            .filter { path -> path.name.startsWith(prefix) }
            .forEach { path -> fileSystem.delete(path) }
    }

    actual fun exists(relativePath: String): Boolean {
        val normalized = relativePath.trim()
        if (normalized.isBlank()) return false
        return fileSystem.exists(baseDir / normalized)
    }

    actual fun resolveFileUri(relativePath: String): String? {
        val normalized = relativePath.trim()
        if (normalized.isBlank()) return null
        val absolutePath = baseDir / normalized
        if (!fileSystem.exists(absolutePath)) return null
        return "file://${absolutePath.toString()}"
    }

    private fun applicationSupportDirPath(): Path {
        val directories = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true,
        )
        val path = directories.firstOrNull() as? String
            ?: error("Failed to resolve iOS application support directory")
        return path.toPath(normalize = true)
    }
}
