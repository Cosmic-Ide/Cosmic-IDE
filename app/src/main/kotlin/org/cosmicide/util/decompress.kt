package org.cosmicide.util

import android.annotation.SuppressLint
import android.system.ErrnoException
import android.system.Os
import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

fun extractTarZstStream(
    inputStream: InputStream,
    targetDir: File,
    filterPrefix: String?,
    longMax: Int = 30
): Boolean {
    return runCatching {
        ZstdInputStream(inputStream.buffered()).use { zstdIn ->
            zstdIn.setLongMax(longMax)

            extractTarStream(
                inputStream = zstdIn,
                targetDir = targetDir,
                filterPrefix = filterPrefix
            )
        }

        true
    }.onFailure {
        it.printStackTrace()
    }.getOrDefault(false)
}

private fun extractTarStream(
    inputStream: InputStream,
    targetDir: File,
    filterPrefix: String?
) {
    val header = ByteArray(TAR_BLOCK_SIZE)

    val canonicalTargetDir = targetDir.canonicalFile
    val canonicalTargetPath = canonicalTargetDir.path
    val canonicalTargetPrefix = canonicalTargetPath + File.separator

    var nextPaxPath: String? = null
    var nextGnuLongName: String? = null

    while (inputStream.readFullyStrict(header)) {
        if (header.isZeroBlock()) {
            break
        }

        val typeFlag = header[156].toInt().toChar()
        val size = parseTarOctal(header, 124, 12)
        val padding = tarPadding(size)

        when (typeFlag) {
            TAR_TYPE_PAX_EXTENDED_HEADER -> {
                val data = inputStream.readExactlyToString(size)
                inputStream.skipFullyStrict(padding)

                val paxRecords = parsePaxRecords(data)
                nextPaxPath = paxRecords["path"]

                continue
            }

            TAR_TYPE_GNU_LONG_NAME -> {
                nextGnuLongName = inputStream.readExactlyToString(size)
                    .trimEnd('\u0000', '\n')
                inputStream.skipFullyStrict(padding)

                continue
            }

            TAR_TYPE_PAX_GLOBAL_HEADER -> {
                inputStream.skipFullyStrict(size + padding)
                continue
            }
        }

        val ustarName = readTarName(header)
        val effectiveName = nextPaxPath ?: nextGnuLongName ?: ustarName

        nextPaxPath = null
        nextGnuLongName = null

        val cleanedName = cleanTarOutputName(
            rawName = effectiveName,
            filterPrefix = filterPrefix
        )

        if (cleanedName == null) {
            inputStream.skipFullyStrict(size + padding)
            continue
        }

        val outputFile = File(canonicalTargetDir, cleanedName).canonicalFile

        if (
            outputFile.path != canonicalTargetPath &&
            !outputFile.path.startsWith(canonicalTargetPrefix)
        ) {
            throw IOException("Tar Slip blocked: $effectiveName")
        }

        when (typeFlag) {
            TAR_TYPE_REGULAR,
            TAR_TYPE_REGULAR_ALT -> {
                if (effectiveName.endsWith("/")) {
                    outputFile.mkdirs()
                    inputStream.skipFullyStrict(size + padding)
                    continue
                }

                outputFile.parentFile?.mkdirs()

                if (outputFile.exists() && outputFile.isDirectory) {
                    throw IOException("Cannot overwrite directory with file: $outputFile")
                }

                outputFile.outputStream().use { out ->
                    inputStream.copyExactlyTo(out, size)
                }

                val mode = parseTarOctal(header, 100, 8)
                applyTarMode(outputFile, mode, isDirectory = false)

                inputStream.skipFullyStrict(padding)
            }

            TAR_TYPE_DIRECTORY -> {
                outputFile.mkdirs()

                val mode = parseTarOctal(header, 100, 8)
                applyTarMode(outputFile, mode, isDirectory = true)

                inputStream.skipFullyStrict(size + padding)
            }

            else -> {
                // Skip symlinks, hardlinks, devices, FIFOs, etc.
                // Your build script stores symlink info separately in glibc/.symlinks.
                inputStream.skipFullyStrict(size + padding)
            }
        }
    }
}

private fun readTarName(header: ByteArray): String {
    val name = readTarString(header, 0, 100)
    val prefix = readTarString(header, 345, 155)

    return if (prefix.isNotEmpty()) {
        "$prefix/$name"
    } else {
        name
    }
}

private fun readTarString(
    header: ByteArray,
    offset: Int,
    length: Int
): String {
    val end = (offset until offset + length)
        .firstOrNull { header[it].toInt() == 0 }
        ?: (offset + length)

    return String(header, offset, end - offset, StandardCharsets.UTF_8)
        .trim { it <= ' ' }
}

private fun parseTarOctal(
    header: ByteArray,
    offset: Int,
    length: Int
): Long {
    val text = readTarString(header, offset, length)
    if (text.isEmpty()) {
        return 0L
    }

    return text.trim().toLongOrNull(8)
        ?: throw IOException("Invalid tar octal field: '$text'")
}

private fun tarPadding(size: Long): Long {
    return (TAR_BLOCK_SIZE - (size % TAR_BLOCK_SIZE)) % TAR_BLOCK_SIZE
}

private fun cleanTarOutputName(
    rawName: String,
    filterPrefix: String?
): String? {
    var name = rawName.replace('\\', '/')

    if (name.isEmpty()) return null
    if (name.startsWith("/")) return null
    if (name.contains('\u0000')) return null

    while (name.startsWith("./")) {
        name = name.removePrefix("./")
    }

    val normalizedPrefix = filterPrefix?.replace('\\', '/')
    if (normalizedPrefix != null) {
        if (!name.startsWith(normalizedPrefix)) {
            return null
        }

        name = name.removePrefix(normalizedPrefix)
    }

    name = name.trimStart('/')
    name = name.trimEnd('/')

    if (name.isEmpty()) return null
    if (name == ".") return null

    val parts = name.split('/')
    if (parts.any { it.isEmpty() || it == "." || it == ".." }) {
        return null
    }

    return name
}

private fun parsePaxRecords(data: String): Map<String, String> {
    val records = LinkedHashMap<String, String>()
    var index = 0

    while (index < data.length) {
        val spaceIndex = data.indexOf(' ', startIndex = index)
        if (spaceIndex <= index) {
            break
        }

        val recordLength = data.substring(index, spaceIndex).toIntOrNull()
            ?: break

        if (recordLength <= 0 || index + recordLength > data.length) {
            break
        }

        val record = data.substring(spaceIndex + 1, index + recordLength)
            .trimEnd('\n')

        val equalsIndex = record.indexOf('=')
        if (equalsIndex > 0) {
            val key = record.substring(0, equalsIndex)
            val value = record.substring(equalsIndex + 1)
            records[key] = value
        }

        index += recordLength
    }

    return records
}

@SuppressLint("SetWorldReadable")
private fun applyTarMode(
    file: File,
    mode: Long,
    isDirectory: Boolean
) {
    file.setReadable(true, false)

    if (isDirectory) {
        file.setWritable(true, true)
        file.setExecutable(true, false)
        return
    }

    file.setWritable((mode and TAR_MODE_OWNER_WRITE) != 0L, true)

    if ((mode and TAR_MODE_ANY_EXECUTE) != 0L) {
        file.setExecutable(true, false)
    } else {
        file.setExecutable(false, false)
    }
}

private fun ByteArray.isZeroBlock(): Boolean {
    for (byte in this) {
        if (byte.toInt() != 0) {
            return false
        }
    }

    return true
}

private fun InputStream.readFullyStrict(buffer: ByteArray): Boolean {
    var offset = 0

    while (offset < buffer.size) {
        val read = read(buffer, offset, buffer.size - offset)

        if (read == -1) {
            if (offset == 0) {
                return false
            }

            throw IOException("Unexpected EOF while reading tar header")
        }

        offset += read
    }

    return true
}

private fun InputStream.readExactlyToString(size: Long): String {
    if (size > Int.MAX_VALUE) {
        throw IOException("Metadata entry too large: $size")
    }

    val output = ByteArrayOutputStream(size.toInt())
    copyExactlyTo(output, size)
    return output.toString(StandardCharsets.UTF_8.name())
}

private fun InputStream.copyExactlyTo(
    outputStream: OutputStream,
    size: Long
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var remaining = size

    while (remaining > 0) {
        val toRead = minOf(remaining, buffer.size.toLong()).toInt()
        val read = read(buffer, 0, toRead)

        if (read == -1) {
            throw IOException("Unexpected EOF while reading tar entry data")
        }

        outputStream.write(buffer, 0, read)
        remaining -= read
    }
}

private fun InputStream.skipFullyStrict(size: Long) {
    var remaining = size

    while (remaining > 0) {
        val skipped = skip(remaining)

        if (skipped > 0) {
            remaining -= skipped
            continue
        }

        if (read() == -1) {
            throw IOException("Unexpected EOF while skipping tar data")
        }

        remaining--
    }
}

fun restoreSymlinksFromManifest(
    targetDir: File,
    manifestName: String = ".symlinks"
): Boolean {
    return runCatching {
        val manifest = File(targetDir, manifestName)
        if (!manifest.isFile) {
            return@runCatching true
        }

        val canonicalTargetDir = targetDir.canonicalFile
        val canonicalTargetPath = canonicalTargetDir.path
        val canonicalTargetPrefix = canonicalTargetPath + File.separator

        manifest.forEachLine { line ->
            if (line.isBlank()) {
                return@forEachLine
            }

            val tabIndex = line.indexOf('\t')
            if (tabIndex <= 0 || tabIndex == line.lastIndex) {
                throw IOException("Invalid symlink manifest line: $line")
            }

            val linkRel = line.substring(0, tabIndex)
            val rawTarget = line.substring(tabIndex + 1)

            if (!isSafeRelativePath(linkRel)) {
                throw IOException("Unsafe symlink path in manifest: $linkRel")
            }

            if (rawTarget.isEmpty() || rawTarget.startsWith("/")) {
                throw IOException("Unsafe absolute symlink target: $linkRel -> $rawTarget")
            }

            val linkFile = File(canonicalTargetDir, linkRel).canonicalFile

            if (
                linkFile.path != canonicalTargetPath &&
                !linkFile.path.startsWith(canonicalTargetPrefix)
            ) {
                throw IOException("Symlink path escapes target dir: $linkRel")
            }

            val resolvedTarget =
                File(linkFile.parentFile ?: canonicalTargetDir, rawTarget).canonicalFile

            if (
                resolvedTarget.path != canonicalTargetPath &&
                !resolvedTarget.path.startsWith(canonicalTargetPrefix)
            ) {
                throw IOException("Symlink target escapes target dir: $linkRel -> $rawTarget")
            }

            linkFile.parentFile?.mkdirs()

            if (linkFile.exists() || linkFile.isSymlink()) {
                linkFile.delete()
            }

            try {
                Os.symlink(rawTarget, linkFile.absolutePath)
            } catch (e: ErrnoException) {
                throw IOException("Failed to create symlink: $linkRel -> $rawTarget", e)
            }
        }

        true
    }.onFailure {
        it.printStackTrace()
    }.getOrDefault(false)
}

private fun File.isSymlink(): Boolean {
    return runCatching {
        absoluteFile.canonicalPath != absoluteFile.path
    }.getOrDefault(false)
}

private fun isSafeRelativePath(path: String): Boolean {
    if (path.isEmpty()) return false
    if (path.startsWith("/")) return false
    if (path.contains('\u0000')) return false

    val normalized = path.replace('\\', '/')
    val parts = normalized.split('/')

    return parts.none { it.isEmpty() || it == "." || it == ".." }
}

private const val TAR_BLOCK_SIZE = 512
private const val DEFAULT_BUFFER_SIZE = 8192

private const val TAR_TYPE_REGULAR = '0'
private const val TAR_TYPE_REGULAR_ALT = '\u0000'
private const val TAR_TYPE_DIRECTORY = '5'
private const val TAR_TYPE_PAX_EXTENDED_HEADER = 'x'
private const val TAR_TYPE_PAX_GLOBAL_HEADER = 'g'
private const val TAR_TYPE_GNU_LONG_NAME = 'L'

private const val TAR_MODE_OWNER_WRITE = 0b010_000_000L
private const val TAR_MODE_ANY_EXECUTE = 0b001_001_001L
