package com.wahon.shared.data.local

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual class LocalArchiveFileScanner actual constructor() {
    private val fileSystem: FileSystem = FileSystem.SYSTEM

    actual fun listCbzFiles(
        directoryPath: String,
        recursive: Boolean,
    ): List<String> {
        val normalizedDirectoryPath = directoryPath.trim()
        require(normalizedDirectoryPath.isNotBlank()) { "Directory path is blank" }

        val root = normalizedDirectoryPath.toPath(normalize = true)
        val rootMetadata = fileSystem.metadata(root)
        require(rootMetadata.isDirectory) { "Path is not a directory: $normalizedDirectoryPath" }

        val paths = if (recursive) {
            discoverRecursive(root)
        } else {
            fileSystem.list(root)
                .filter { path -> isCbzFile(path) }
        }
        return paths.map { path -> path.toString() }
    }

    private fun discoverRecursive(root: Path): List<Path> {
        val archives = mutableListOf<Path>()
        val stack = ArrayDeque<Path>()
        stack.addLast(root)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val metadata = try {
                fileSystem.metadata(current)
            } catch (_: Throwable) {
                null
            } ?: continue

            if (metadata.isDirectory) {
                val children = try {
                    fileSystem.list(current)
                } catch (_: Throwable) {
                    emptyList()
                }
                children.forEach { child ->
                    stack.addLast(child)
                }
                continue
            }

            if (isCbzFile(current)) {
                archives += current
            }
        }

        return archives
    }

    private fun isCbzFile(path: Path): Boolean {
        val metadata = try {
            fileSystem.metadata(path)
        } catch (_: Throwable) {
            null
        } ?: return false

        if (!metadata.isRegularFile) return false
        return path.name.lowercase().endsWith(CBZ_EXTENSION)
    }

    private companion object {
        private const val CBZ_EXTENSION = ".cbz"
    }
}
