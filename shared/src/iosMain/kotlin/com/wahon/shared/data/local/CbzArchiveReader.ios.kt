package com.wahon.shared.data.local

import okio.Buffer
import okio.FileSystem
import okio.Inflater
import okio.InflaterSource
import okio.Path.Companion.toPath

actual class CbzArchiveReader actual constructor() {
    private val fileSystem: FileSystem = FileSystem.SYSTEM

    private var cachedArchivePath: String? = null
    private var cachedArchive: ParsedCbzArchive? = null

    actual fun listPages(archivePath: String): List<CbzPageEntry> {
        return ensureArchiveLoaded(archivePath).pages
    }

    actual fun readPageBytes(
        archivePath: String,
        relativePath: String,
    ): ByteArray {
        val normalizedRelativePath = normalizeEntryPath(relativePath)
        val archive = ensureArchiveLoaded(archivePath)
        val entry = archive.entriesByPath[normalizedRelativePath]
            ?: error("CBZ entry not found: $normalizedRelativePath")

        val compressedData = archive.payload.copyOfRange(entry.dataStart, entry.dataEndExclusive)
        return when (entry.compressionMethod) {
            COMPRESSION_STORED -> compressedData
            COMPRESSION_DEFLATE -> inflateRawDeflate(
                compressed = compressedData,
                expectedSize = entry.uncompressedSize,
            )

            else -> error(
                "Unsupported CBZ compression method ${entry.compressionMethod} for ${entry.relativePath}",
            )
        }
    }

    private fun ensureArchiveLoaded(rawArchivePath: String): ParsedCbzArchive {
        val archivePath = rawArchivePath.trim()
        require(archivePath.isNotBlank()) { "CBZ path is blank" }

        if (cachedArchivePath == archivePath) {
            cachedArchive?.let { return it }
        }

        val payload = fileSystem.read(archivePath.toPath(normalize = true)) {
            readByteArray()
        }
        val parsed = parseArchive(payload)
        cachedArchivePath = archivePath
        cachedArchive = parsed
        return parsed
    }

    private fun parseArchive(payload: ByteArray): ParsedCbzArchive {
        val entries = linkedMapOf<String, CbzZipEntry>()
        var offset = 0

        while (offset + ZIP_SIGNATURE_SIZE <= payload.size) {
            val signature = readIntLe(payload, offset)
            when (signature) {
                ZIP_LOCAL_FILE_HEADER_SIGNATURE -> {
                    val header = parseLocalHeader(payload, offset)
                    val normalizedName = normalizeEntryPath(header.fileName)
                    if (
                        normalizedName.isNotEmpty() &&
                        !normalizedName.endsWith("/") &&
                        isSupportedCbzImage(normalizedName)
                    ) {
                        entries[normalizedName] = CbzZipEntry(
                            relativePath = normalizedName,
                            fileName = normalizedName.substringAfterLast('/'),
                            compressionMethod = header.compressionMethod,
                            uncompressedSize = header.uncompressedSize,
                            dataStart = header.dataStart,
                            dataEndExclusive = header.dataEndExclusive,
                        )
                    }
                    offset = header.nextOffset
                }

                ZIP_CENTRAL_DIRECTORY_SIGNATURE,
                ZIP_END_OF_CENTRAL_DIRECTORY_SIGNATURE,
                    -> break

                else -> error("Unsupported ZIP signature at offset $offset")
            }
        }

        val sortedEntries = entries.values
            .sortedWith(
                compareBy<CbzZipEntry> { naturalSortKey(it.relativePath) }
                    .thenBy { it.relativePath.lowercase() },
            )

        return ParsedCbzArchive(
            payload = payload,
            entriesByPath = sortedEntries.associateBy { it.relativePath },
            pages = sortedEntries.map { entry ->
                CbzPageEntry(
                    relativePath = entry.relativePath,
                    fileName = entry.fileName,
                )
            },
        )
    }

    private fun parseLocalHeader(
        payload: ByteArray,
        headerOffset: Int,
    ): ParsedLocalHeader {
        val fixedHeaderEnd = headerOffset + ZIP_LOCAL_HEADER_FIXED_SIZE
        require(fixedHeaderEnd <= payload.size) {
            "Truncated CBZ local header"
        }

        val flags = readShortLe(payload, headerOffset + 6)
        require((flags and ZIP_DATA_DESCRIPTOR_FLAG) == 0) {
            "CBZ with data descriptor is not supported"
        }

        val compressionMethod = readShortLe(payload, headerOffset + 8)
        val compressedSize = readIntLe(payload, headerOffset + 18)
        val uncompressedSize = readIntLe(payload, headerOffset + 22)
        val fileNameLength = readShortLe(payload, headerOffset + 26)
        val extraLength = readShortLe(payload, headerOffset + 28)

        val nameStart = fixedHeaderEnd
        val nameEnd = nameStart + fileNameLength
        val extraEnd = nameEnd + extraLength
        require(extraEnd <= payload.size) {
            "Truncated CBZ local header fields"
        }

        val fileName = payload.copyOfRange(nameStart, nameEnd).decodeToString()
        val dataStart = extraEnd
        val dataEnd = dataStart + compressedSize
        require(dataEnd <= payload.size) {
            "Truncated CBZ entry data for $fileName"
        }

        return ParsedLocalHeader(
            fileName = fileName,
            compressionMethod = compressionMethod,
            uncompressedSize = uncompressedSize,
            dataStart = dataStart,
            dataEndExclusive = dataEnd,
            nextOffset = dataEnd,
        )
    }

    private fun inflateRawDeflate(
        compressed: ByteArray,
        expectedSize: Int,
    ): ByteArray {
        val compressedBuffer = Buffer().write(compressed)
        val inflater = Inflater(true)
        val inflaterSource = InflaterSource(compressedBuffer, inflater)
        val outBuffer = Buffer()
        return try {
            outBuffer.writeAll(inflaterSource)
            val inflated = outBuffer.readByteArray()
            if (expectedSize > 0 && inflated.size != expectedSize) {
                error("CBZ inflate size mismatch: expected=$expectedSize actual=${inflated.size}")
            }
            inflated
        } finally {
            runCatching { inflaterSource.close() }
        }
    }

    private fun normalizeEntryPath(rawPath: String): String {
        return rawPath.trim()
            .replace('\\', '/')
            .trimStart('/')
    }

    private fun readShortLe(
        payload: ByteArray,
        offset: Int,
    ): Int {
        return (payload[offset].toInt() and 0xFF) or
            ((payload[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readIntLe(
        payload: ByteArray,
        offset: Int,
    ): Int {
        return (payload[offset].toInt() and 0xFF) or
            ((payload[offset + 1].toInt() and 0xFF) shl 8) or
            ((payload[offset + 2].toInt() and 0xFF) shl 16) or
            ((payload[offset + 3].toInt() and 0xFF) shl 24)
    }

    private data class ParsedCbzArchive(
        val payload: ByteArray,
        val entriesByPath: Map<String, CbzZipEntry>,
        val pages: List<CbzPageEntry>,
    )

    private data class CbzZipEntry(
        val relativePath: String,
        val fileName: String,
        val compressionMethod: Int,
        val uncompressedSize: Int,
        val dataStart: Int,
        val dataEndExclusive: Int,
    )

    private data class ParsedLocalHeader(
        val fileName: String,
        val compressionMethod: Int,
        val uncompressedSize: Int,
        val dataStart: Int,
        val dataEndExclusive: Int,
        val nextOffset: Int,
    )

    private companion object {
        private const val ZIP_SIGNATURE_SIZE = 4
        private const val ZIP_LOCAL_HEADER_FIXED_SIZE = 30
        private const val ZIP_DATA_DESCRIPTOR_FLAG = 0x08
        private const val COMPRESSION_STORED = 0
        private const val COMPRESSION_DEFLATE = 8

        private const val ZIP_LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50
        private const val ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014B50
        private const val ZIP_END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50
    }
}
