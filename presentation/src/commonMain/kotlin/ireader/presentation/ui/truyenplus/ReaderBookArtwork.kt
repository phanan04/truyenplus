package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import ireader.domain.utils.cover.CoverColorExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

/** A real cover supplies the hero tone. No title hash or fabricated cover palette. */
internal fun readerBookHeroColor(seed:Color):Color {
    var color=lerp(seed,Color.Black,.22f)
    while(1.05f/(color.luminance()+.05f)<4.5f)color=lerp(color,Color.Black,.12f)
    return color
}
private val heroSeeds=linkedMapOf<String,Color>()
@Composable internal fun rememberReaderBookArtwork(book:ReaderBookUi):Color? {
    val cover=book.cover
    val key="${cover?.cover}|${cover?.lastModified}"
    val extractor=remember {runCatching {org.koin.mp.KoinPlatform.getKoin().getOrNull<CoverColorExtractor>()}.getOrNull()}
    val seed by produceState(heroSeeds[key],key,extractor) {
        val path=cover?.cover ?: return@produceState
        if(value!=null || extractor==null)return@produceState
        val color=try {withContext(Dispatchers.IO) {
            val result=if(path.startsWith("/")) {
                val file=path.toPath();val size=FileSystem.SYSTEM.metadataOrNull(file)?.size ?: 0
                if(size in 1..8*1024*1024)extractor.extractDominantColorFromBitmap(FileSystem.SYSTEM.read(file){readByteArray()}) else null
            } else if(path.startsWith("https://") || path.startsWith("http://"))extractor.extractDominantColor(path,cover.sourceId) else null
            result?.let {Color(it.red,it.green,it.blue,it.alpha)}
        }} catch(e:CancellationException){throw e} catch(_:Exception){null}
        if(color!=null) {
            if(heroSeeds.size>=32)heroSeeds.remove(heroSeeds.keys.first())
            heroSeeds[key]=color;value=color
        }
    }
    return seed?.let(::readerBookHeroColor)
}
