package ireader.data.shelf
import ireader.domain.usecases.epub.EpubMetadataReader
expect fun readShelfEpubMetadata(path:String): EpubMetadataReader.Metadata
