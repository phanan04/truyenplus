"""Run the real Vietnamese discovery regression tests with cached JVM dependencies."""
from pathlib import Path
import os
import subprocess
import tempfile
import argparse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--live-folder', type=Path, help='Verify server HTML previously downloaded into this folder')
parser.add_argument('--catalog-folder', type=Path, help='Verify downloaded TruyenHH pages with the production loader')
args = parser.parse_args()

root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'

def jar(group, name, version=None):
    folder = cache / group / name
    files = sorted((folder / version).glob('*/*.jar')) if version else sorted(folder.glob('*/*/*.jar'))
    if not files:
        raise SystemExit(f'Missing cached dependency: {name}')
    return str(files[-1])

stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.21')
compiler = os.pathsep.join([jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.2.21'), stdlib,
    jar('org.jetbrains', 'annotations'), jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.2.21'),
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.21'), jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm')])
runtime = os.pathsep.join([stdlib, jar('org.jetbrains', 'annotations'),
    jar('org.jetbrains.kotlinx', 'kotlinx-serialization-json-jvm'), jar('org.jetbrains.kotlinx', 'kotlinx-serialization-core-jvm'),
    jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'), jar('org.jetbrains.kotlin', 'kotlin-test'), jar('com.squareup.okio', 'okio-jvm'),
    jar('org.jetbrains.kotlin', 'kotlin-test-junit'), jar('junit', 'junit'), jar('org.hamcrest', 'hamcrest-core'),
    *[str(p) for p in cache.glob('io.github.ireaderorg/ksoup*-jvm/*/*/*.jar')],
    *[str(p) for p in cache.glob('com.fleeksoft.*/*-jvm/*/*/*.jar')],
    *[str(p) for p in cache.glob('co.touchlab/*-jvm/*/*/*.jar')]])
java = str(Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home')) / 'bin/java')
sources = list((root / 'domain/src/commonMain/kotlin/ireader/domain/models/discovery').glob('*.kt')) + [
    root/'domain/src/commonMain/kotlin/ireader/domain/data/repository/DiscoveryRepository.kt',
    root/'domain/src/commonMain/kotlin/ireader/domain/data/repository/DiscoveryTranslations.kt',
    root/'presentation/src/commonMain/kotlin/ireader/presentation/ui/truyenplus/DiscoverySelection.kt',
    root/'presentation/src/desktopTest/kotlin/ireader/presentation/ui/truyenplus/TruyenFullDiscoveryTest.kt',
    root/'presentation/src/desktopTest/kotlin/ireader/presentation/ui/truyenplus/VietnameseCatalogTest.kt',
    root/'presentation/src/desktopTest/kotlin/ireader/presentation/ui/truyenplus/DiscoveryTranslationTest.kt',
    root/'data/src/commonMain/kotlin/ireader/data/discovery/CachedDiscoveryTranslations.kt',
    root/'data/src/commonMain/kotlin/ireader/data/discovery/FileDiscoveryCache.kt',
    root/'data/src/commonMain/kotlin/ireader/data/discovery/NovelTrackerParser.kt',
    root/'data/src/commonMain/kotlin/ireader/data/discovery/NovelTrackerDiscoveryRepository.kt',
    *list((root/'data/src/commonMain/kotlin/ireader/data/discovery').glob('TruyenFull*.kt')),
    *list((root/'data/src/commonMain/kotlin/ireader/data/discovery').glob('Vietnamese*.kt'))]
with tempfile.TemporaryDirectory(prefix='truyenplus-vietnamese-tests-') as output:
    if args.live_folder:
        driver = Path(output)/'Live.kt'
        driver.write_text('''import ireader.data.discovery.*
import ireader.domain.models.discovery.*
import ireader.presentation.ui.truyenplus.*
import java.io.File
import kotlinx.coroutines.runBlocking
fun main(args: Array<String>) = runBlocking {
    val folder = File(args.single())
    val loader = TruyenFullDiscoveryFetcher({ url ->
        val file = when {
            url.endsWith("truyen-hot/") -> "hot.html"
            url.endsWith("truyen-moi/") -> "new.html"
            else -> url.trimEnd('/').substringAfterLast('/') + ".html"
        }
        File(folder, file).readText()
    }, { java.time.Instant.ofEpochMilli(File(folder, "hot.html").lastModified()).toString().replace('T', ' ').replace("Z", " UTC") })
    val raw = loader.fetch()
    val feed = NovelTrackerParser.parse(DiscoverySource.TruyenFull, raw)
    check(feed.books.all { it.description.isNotBlank() && it.author.isNotBlank() && it.coverUrl != null && it.genres.isNotEmpty() })
    val listed = TruyenFullParser.listings(File(folder, "hot.html").readText(), File(folder, "new.html").readText())
    check(feed.books.count { it.rank != null } > 0)
    check(feed.books.all { book -> book.rank == listed.single { it.id == book.id }.rank })
    check(feed.unavailableBooks == listed.size - feed.books.size)
    check(feed.books.filter { it.recentlyUpdated }.all { it.rank == null })
    File(folder, "truyenfull.json").writeText(raw)
    println("PASS live HTML: ${feed.books.size} real books, ${feed.unavailableBooks} inaccessible details, " +
        "${filterDiscoveryBooks(feed, DiscoverySection.Hot, "", "").size} hot, " +
        "${filterDiscoveryBooks(feed, DiscoverySection.New, "", "").size} recently updated")
}
''')
        sources.append(driver)
    if args.catalog_folder:
        driver = Path(output)/'LiveCatalog.kt'
        driver.write_text('''import ireader.data.discovery.*
import ireader.domain.models.discovery.*
import java.io.File
import kotlinx.coroutines.runBlocking
fun main(args: Array<String>) = runBlocking {
    val folder = File(args.single())
    val source = DiscoverySource.TruyenHH
    val loader = VietnameseCatalogFetcher(source, { url ->
        val file = when (url) {
            source.feedUrl -> "hh-rank.html"
            "https://truyenhh.com/" -> "hh-home.html"
            "https://truyenhh.com/truyen/" -> "hh-new.html"
            "https://truyenhh.com/truyen/page/2/" -> "hh-new-2.html"
            else -> url.trimEnd('/').substringAfterLast('/') + ".html"
        }
        File(folder, file).readText()
    }, { java.time.Instant.now().toString().replace('T', ' ').replace("Z", " UTC") })
    val raw = loader.fetch()
    val feed = NovelTrackerParser.parse(source, raw)
    check(feed.books.all { it.description.isNotBlank() && it.author.isNotBlank() && it.coverUrl != null })
    check(feed.books.count { it.rank != null } == 100)
    val next = NovelTrackerParser.parse(source, loader.fetchPage(feed.nextPageUrls.single()))
    check(next.books.isNotEmpty() && next.books.all { it.recentlyUpdated && it.rank == null })
    check(next.nextPageUrls.single() == "https://truyenhh.com/truyen/page/3/")
    File(folder, "truyenhh-live.json").writeText(raw)
    File(folder, "truyenhh-page2.json").writeText(VietnameseCatalogParser(source).snapshot(next.books, next.updatedTime, next.nextPageUrls, next.unavailableBooks))
    println("PASS TruyenHH live: ${feed.books.size} complete records, ${feed.books.count { it.rank != null }} ranked, ${feed.books.count { it.isHot }} hot, ${feed.books.count { it.recentlyUpdated }} updated; ${next.books.size} records on page 2; ${feed.unavailableBooks + next.unavailableBooks} inaccessible details")
}
''')
        sources.append(driver)
    subprocess.run([java, '-cp', compiler, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect',
        '-classpath', runtime, '-d', output, *map(str, sources)], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([output, runtime, str(root/'presentation/src/desktopTest/resources')]),
        'org.junit.runner.JUnitCore', 'ireader.data.discovery.TruyenFullDiscoveryTest', 'ireader.data.discovery.DiscoveryTranslationTest', 'ireader.data.discovery.VietnameseCatalogTest'], check=True)
    if args.live_folder:
        subprocess.run([java, '-cp', os.pathsep.join([output, runtime]), 'LiveKt', str(args.live_folder)], check=True)

    if args.catalog_folder:
        subprocess.run([java, '-cp', os.pathsep.join([output, runtime]), 'LiveCatalogKt', str(args.catalog_folder)], check=True)
