package ireader.data.discovery

import okio.FileSystem
import okio.Path

/** JSON snapshots belong in files: desktop preference strings have a size limit. */
class FileDiscoveryCache(private val directory: Path, private val files: FileSystem) : DiscoveryCache {
    override fun read(key: String): String? {
        val path = directory / "$key.json"
        val size = files.metadataOrNull(path)?.size ?: return null
        if (size > 2_100_000) return null
        return files.read(path) { readUtf8() }
    }
    override fun write(key: String, value: String) {
        files.createDirectories(directory)
        val target = directory / "$key.json"
        val temporary = directory / "$key.tmp"
        files.write(temporary) { writeUtf8(value) }
        files.atomicMove(temporary, target)
    }
}
