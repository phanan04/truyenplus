"""Run the isolated presentation calculation with the installed Kotlin compiler."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
version = '2.2.21'
def jar(group, name, version=None):
    folder = cache / group / name
    candidates = sorted((folder / version).glob('*/*.jar')) if version else sorted(folder.glob('*/*/*.jar'))
    if not candidates:
        raise SystemExit(f'Missing cached Kotlin dependency: {name}')
    return str(candidates[-1])
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', version)
annotations = jar('org.jetbrains', 'annotations')
compiler_cp = os.pathsep.join([
    jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', version),
    stdlib, annotations,
    jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.2.21'),
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.21'),
    jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'),
])
java_home = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home'))
java = str(java_home / 'bin/java')
source = root / 'presentation/src/commonMain/kotlin/ireader/presentation/ui/component/list/layouts/BookReadingProgress.kt'
with tempfile.TemporaryDirectory(prefix='truyenplus-progress-') as out:
    subprocess.run([java, '-cp', compiler_cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                    '-no-stdlib', '-no-reflect', '-classpath', os.pathsep.join([stdlib, annotations]),
                    '-d', out, str(root / 'scripts/verify-reading-progress.kt'), str(source)], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([out, stdlib]), 'Verify_reading_progressKt'], check=True)
