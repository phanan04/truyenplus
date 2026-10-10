"""Compile and execute the actual discovery code with cached Kotlin dependencies."""
from pathlib import Path
import os
import subprocess
import tempfile
import sys

root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, name, version=None):
    folder = cache / group / name
    files = sorted((folder / version).glob('*/*.jar')) if version else sorted(folder.glob('*/*/*.jar'))
    if not files: raise SystemExit(f'Missing cached dependency: {name}')
    return str(files[-1])
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.21')
annotations = jar('org.jetbrains', 'annotations')
compiler = os.pathsep.join([jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.2.21'),
    stdlib, annotations, jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.2.21'),
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.21'), jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm')])
runtime = os.pathsep.join([stdlib, annotations, jar('org.jetbrains.kotlinx', 'kotlinx-serialization-json-jvm'),
    jar('org.jetbrains.kotlinx', 'kotlinx-serialization-core-jvm'), jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'),
    *[str(p) for p in cache.glob('io.github.ireaderorg/ksoup*-jvm/*/*/*.jar')],
    *[str(p) for p in cache.glob('com.fleeksoft.*/*-jvm/*/*/*.jar')],
    *[str(p) for p in cache.glob('co.touchlab/*-jvm/*/*/*.jar')]])
java = str(Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home')) / 'bin/java')
sources = [root/'domain/src/commonMain/kotlin/ireader/domain/models/discovery/DiscoveryModels.kt',
           root/'data/src/commonMain/kotlin/ireader/data/discovery/NovelTrackerParser.kt', root/'data/src/commonMain/kotlin/ireader/data/discovery/NovelTrackerDiscoveryRepository.kt',
           root/'domain/src/commonMain/kotlin/ireader/domain/data/repository/DiscoveryRepository.kt', root/'domain/src/commonMain/kotlin/ireader/domain/data/repository/DiscoveryTranslations.kt', root/'presentation/src/commonMain/kotlin/ireader/presentation/ui/truyenplus/DiscoverySelection.kt', root/'scripts/DiscoveryChecks.kt', root/'data/src/commonMain/kotlin/ireader/data/discovery/VietnameseCatalogParser.kt']
with tempfile.TemporaryDirectory(prefix='truyenplus-discovery-') as output:
    subprocess.run([java, '-cp', compiler, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', runtime, '-d', output, *map(str, sources)], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([output, runtime]), 'DiscoveryChecksKt',
        sys.argv[1] if len(sys.argv) > 1 else str(root/'presentation/src/desktopTest/resources/discovery')], check=True)
