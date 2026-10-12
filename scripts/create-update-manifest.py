"""Create Truyện+ update.json from already-built APKs; never builds or uploads anything."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import quote, urlparse


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, action="append", required=True)
    parser.add_argument("--download-base-url", required=True, help="HTTPS release asset directory, including the version tag")
    parser.add_argument("--notes-file", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--aapt", help="Path to Android SDK build-tools/aapt")
    args = parser.parse_args()
    url = urlparse(args.download_base_url)
    if url.scheme != "https" or not url.hostname or url.username or url.password or url.query or url.fragment:
        parser.error("download-base-url must be a plain HTTPS directory URL")
    aapt = args.aapt or shutil.which("aapt")
    if not aapt:
        sdk_root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        candidates = sorted((Path(sdk_root) / "build-tools").glob("*/aapt")) if sdk_root else []
        if candidates:
            aapt = str(candidates[-1])
    if not aapt:
        parser.error("Set ANDROID_HOME or pass --aapt /path/to/build-tools/aapt")
    release = None
    assets = []
    seen_abis = set()
    for apk in args.apk:
        if not apk.is_file() or apk.suffix != ".apk":
            parser.error(f"Not an APK file: {apk}")
        output = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True)
        manifest = subprocess.check_output(
            [aapt, "dump", "xmltree", str(apk), "AndroidManifest.xml"], text=True)
        if re.search(r"android:testOnly[^\n]*0xffffffff", manifest):
            parser.error(f"APK has android:testOnly=true and cannot be installed normally: {apk}")
        package_line = next(line for line in output.splitlines() if line.startswith("package:"))
        package = dict(re.findall(r"(\w+)='([^']*)'", package_line))
        sdk = re.search(r"^sdkVersion:'(\d+)'", output, re.MULTILINE)
        metadata = {"versionCode": int(package["versionCode"]), "versionName": package["versionName"],
                    "packageName": package["name"], "minSdk": int(sdk.group(1)) if sdk else 26}
        if release is not None and metadata != release:
            parser.error("All APKs must have identical package, versionCode, versionName and minSdk")
        release = metadata
        native = re.search(r"^native-code:(.*)$", output, re.MULTILINE)
        abis = re.findall(r"'([^']+)'", native.group(1)) if native else []
        abi = abis[0] if len(abis) == 1 else "universal"
        if abi in seen_abis:
            parser.error(f"Duplicate APK architecture: {abi}")
        seen_abis.add(abi)
        digest = hashlib.sha256()
        with apk.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(block)
        assets.append({"abi": abi, "url": args.download_base_url.rstrip("/") + "/" + quote(apk.name),
                       "sha256": digest.hexdigest(), "sizeBytes": apk.stat().st_size})
    release["notes"] = args.notes_file.read_text(encoding="utf-8") if args.notes_file else ""
    release["assets"] = assets
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(release, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Created {args.output}: {release['versionName']} ({release['versionCode']}), {len(assets)} APK(s)")


if __name__ == "__main__":
    main()
