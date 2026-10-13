package ireader.domain.models.common

expect class Uri {
    override fun toString(): String
    companion object {
        fun parse(uriString: String): Uri
        /** URI suitable for the platform's EPUB/file reader, preserving path characters. */
        fun fromFilePath(path: String): Uri
    }
}
