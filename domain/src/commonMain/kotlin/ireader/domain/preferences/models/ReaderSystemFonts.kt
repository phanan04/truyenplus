package ireader.domain.preferences.models

import ireader.domain.models.common.FontFamilyModel

/** Stable identifiers keep built-in families out of the downloadable Google Fonts path. */
fun decodeReaderFont(saved: String): FontType = when (saved.lowercase()) {
    "system:default", "default" -> FontType("Default", FontFamilyModel.Default)
    "system:serif", "serif" -> FontType("Serif", FontFamilyModel.Serif)
    "system:sans", "sans serif", "sans-serif", "sansserif" -> FontType("Sans serif", FontFamilyModel.SansSerif)
    "system:mono", "monospace" -> FontType("Monospace", FontFamilyModel.Monospace)
    "system:cursive", "cursive" -> FontType("Cursive", FontFamilyModel.Cursive)
    else -> FontType(saved, FontFamilyModel.Custom(saved))
}
fun encodeReaderFont(font: FontType): String = when (font.fontFamily) {
    FontFamilyModel.Default -> "system:default"
    FontFamilyModel.Serif -> "system:serif"
    FontFamilyModel.SansSerif -> "system:sans"
    FontFamilyModel.Monospace -> "system:mono"
    FontFamilyModel.Cursive -> "system:cursive"
    is FontFamilyModel.Custom -> font.name
}
