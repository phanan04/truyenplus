"""Check UI syntax, translation XML, and that the IReader business layers stayed intact."""
from pathlib import Path
import os
import subprocess
import tempfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
original = root.parent / 'IReader-master'
changed = []
for folder in ('presentation', 'android', 'desktop'):
    for path in (root / folder).rglob('*.kt'):
        if 'build' in path.parts:
            continue
        reference = original / path.relative_to(root)
        if not reference.exists() or path.read_bytes() != reference.read_bytes():
            changed.append(path)
allowed_existing = {'data/src/commonMain/kotlin/ireader/data/di/repositoryInjectModule.kt'}
for folder in ('domain', 'data', 'core-utils', 'source-api', 'plugin-api', 'source-runtime-js', 'android-compat', 'supabase'):
    for reference in (original / folder).rglob('*'):
        if reference.is_file() and 'build' not in reference.parts:
            current = root / reference.relative_to(original)
            assert current.exists(), str(current)
            if str(reference.relative_to(original)) not in allowed_existing:
                assert current.read_bytes() == reference.read_bytes(), str(current)
print('PASS: existing IReader business/source layers preserved, apart from discovery DI registration', flush=True)
translations = root / 'i18n/src/commonMain/composeResources'
en = {node.attrib['name'] for node in ET.parse(translations / 'values/truyenplus.xml').getroot()}
vi = {node.attrib['name'] for node in ET.parse(translations / 'values-vi/truyenplus.xml').getroot()}
assert en == vi, 'Missing Vietnamese translation keys'
ET.parse(root / 'android/src/main/AndroidManifest.xml')
print('PASS: XML resources and English/Vietnamese translation keys', flush=True)

cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, name, version=None):
    folder = cache / group / name
    matches = sorted((folder / version).glob('*/*.jar')) if version else sorted(folder.glob('*/*/*.jar'))
    if not matches:
        raise SystemExit(f'Missing cached compiler library: {name}')
    return str(matches[-1])
classpath = os.pathsep.join([
    jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.2.21'),
    jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.21'),
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.2.21'),
    jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.2.21'),
    jar('org.jetbrains', 'annotations'),
    jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'),
])
java_home = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home'))
with tempfile.TemporaryDirectory(prefix='truyenplus-syntax-') as output:
    subprocess.run([str(java_home / 'bin/javac'), '-cp', classpath, '-d', output,
                    str(root / 'scripts/KotlinSyntaxCheck.java')], check=True)
    subprocess.run([str(java_home / 'bin/java'), '-cp', os.pathsep.join([output, classpath]),
                    'KotlinSyntaxCheck', *map(str, changed)], check=True)
