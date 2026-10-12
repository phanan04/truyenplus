"""Regression check against real already-built testOnly and distributable APKs."""
import argparse
from pathlib import Path
import subprocess
import sys
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--test-only-apk', required=True)
parser.add_argument('--installable-apk')
parser.add_argument('--aapt', required=True)
args = parser.parse_args()
script = Path(__file__).with_name('create-update-manifest.py')
with tempfile.TemporaryDirectory() as directory:
    output = Path(directory) / 'update.json'
    def run(apk):
        return subprocess.run([sys.executable, str(script), '--apk', apk, '--aapt', args.aapt,
            '--download-base-url', 'https://example.org/releases/test', '--output', str(output)],
            capture_output=True, text=True)
    bad = run(args.test_only_apk)
    assert bad.returncode != 0 and 'testOnly' in bad.stderr, bad.stdout + bad.stderr
    assert not output.exists(), 'Rejected APK must never produce an update manifest'
    print('PASS testOnly APK rejected before manifest publication')
    if args.installable_apk:
        good = run(args.installable_apk)
        assert good.returncode == 0 and output.exists(), good.stdout + good.stderr
        print('PASS normally installable APK produces update manifest')
