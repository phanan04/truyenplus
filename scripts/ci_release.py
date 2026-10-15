#!/usr/bin/env python3
"""Build metadata and guarded GitHub publication for the existing Android updater."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile

PACKAGE = "ir.kazemcodes.infinityreader.debug"
SIGNER = "0668c5aa3949e38feeba2444c300d065599be1dbfcacc0c01f56929e9e229f21"
CONFIG = Path("buildSrc/src/main/kotlin/ProjectConfig.kt")


def version_for_run(base_name, base_code, run):
    if not re.fullmatch(r"\d+\.\d+\.\d+", base_name) or run < 1 or not 0 < base_code + run < 2_100_000_000:
        raise ValueError("Invalid CI version or run number")
    major, minor, patch = map(int, base_name.split("."))
    return f"{major}.{minor}.{patch + run}", base_code + run


def patch_version(path, name, code):
    text = path.read_text()
    text, names = re.subn(r'(const val versionName = )"[^"\n]+"', lambda m: m[1] + json.dumps(name), text)
    text, codes = re.subn(r"(const val versionCode = )\d+", lambda m: m[1] + str(code), text)
    if names != 1 or codes != 1:
        raise ValueError("Version config must contain exactly one name and code")
    path.write_text(text)


def release_state(release, sha):
    if release["target_commitish"] != sha:
        raise ValueError("Release tag belongs to a different source revision")
    return "resume" if release["draft"] else "done"


def verify_assets(expected, uploaded):
    actual = {a["name"]: a for a in uploaded}
    if set(expected) != set(actual):
        raise ValueError("Release assets are incomplete or unexpected")
    for name, (size, digest) in expected.items():
        if actual[name]["size"] != size or actual[name].get("digest") != "sha256:" + digest:
            raise ValueError("Uploaded asset failed integrity verification: " + name)


def verify_metadata(badging, manifest, cert, name, code):
    required = [f"name='{PACKAGE}'", f"versionCode='{code}'", f"versionName='{name}'", "sdkVersion:'26'", "native-code: 'arm64-v8a'"]
    if not all(item in badging for item in required):
        raise ValueError("APK package/version/SDK/ABI does not match this release")
    if re.search(r"android:testOnly[^\n]*0xffffffff", manifest):
        raise ValueError("testOnly APK must not be published")
    if "Verified using v2 scheme (APK Signature Scheme v2): true" not in cert or f"Signer #1 certificate SHA-256 digest: {SIGNER}" not in cert:
        raise ValueError("APK signing certificate differs from the installed TruyenPlus app")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        # Authenticated API calls must never forward the token to a redirect host.
        return None


class HttpsRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        if urllib.parse.urlparse(newurl).scheme != "https":
            raise ValueError("Insecure release download redirect")
        return super().redirect_request(request, fp, code, msg, headers, newurl)


class GitHub:
    def __init__(self):
        self.repo = os.environ["GITHUB_REPOSITORY"]
        if self.repo != "phanan04/truyenplus":
            raise ValueError("Publication is restricted to the configured TruyenPlus repository")
        self.base = "https://api.github.com/repos/" + self.repo
        self.token = os.environ["GH_TOKEN"]

    def api(self, path, method="GET", body=None, binary=None, missing_ok=False):
        url = path if path.startswith("https://") else self.base + path
        if not (url.startswith(self.base + "/") or url.startswith("https://uploads.github.com/repos/" + self.repo + "/releases/")):
            raise ValueError("Unexpected API host/path")
        headers = {"Authorization": "Bearer " + self.token, "Accept": "application/vnd.github+json",
                   "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "TruyenPlus-CI"}
        data = binary if binary is not None else json.dumps(body).encode() if body is not None else None
        if data is not None:
            headers["Content-Type"] = "application/octet-stream" if binary is not None else "application/json"
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.build_opener(NoRedirect()).open(request, timeout=180) as response:
                payload = response.read()
                return json.loads(payload) if payload else None
        except urllib.error.HTTPError as error:
            if error.code == 404 and missing_ok:
                return None
            raise RuntimeError(f"GitHub API failed with HTTP {error.code}") from None

    def current(self, sha):
        return self.api("/branches/main")["commit"]["sha"] == sha


def download(url):
    if urllib.parse.urlparse(url).scheme != "https":
        raise ValueError("Release downloads require HTTPS")
    return urllib.request.build_opener(HttpsRedirect()).open(
        urllib.request.Request(url, headers={"User-Agent": "TruyenPlus-CI", "Cache-Control": "no-cache"}), timeout=120)


def info(path):
    with path.open("rb") as stream:
        return path.stat().st_size, hashlib.file_digest(stream, "sha256").hexdigest()


def outputs(values):
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        for key, value in values.items():
            stream.write(f"{key}={value}\n")


def plan():
    api = GitHub()
    sha = os.environ["GITHUB_SHA"]
    if not api.current(sha):
        outputs({"active": "false"})
        print("A newer main revision is queued; skipping this revision.")
        return
    config = CONFIG.read_text()
    name, code = version_for_run(re.search(r'versionName = "([^"]+)"', config)[1],
                                 int(re.search(r"versionCode = (\d+)", config)[1]), int(os.environ["GITHUB_RUN_NUMBER"]))
    tag = "v" + name
    existing = api.api("/releases/tags/" + tag, missing_ok=True)
    if existing and release_state(existing, sha) == "done":
        outputs({"active": "false"})
        print("This workflow run already published its release; no duplicate build.")
        return
    latest = api.api("/releases/latest", missing_ok=True)
    if latest:
        asset = next(a for a in latest["assets"] if a["name"] == "update.json")
        with download(asset["browser_download_url"]) as response:
            metadata = json.load(response)
        if metadata["packageName"] != PACKAGE or metadata["versionCode"] >= code:
            raise ValueError("CI version must exceed the latest published app; increase the source version baseline")
    outputs({"active": "true", "version_name": name, "version_code": code, "tag": tag})
    print(f"Reserved {tag} ({code}) for run {os.environ['GITHUB_RUN_NUMBER']}")


def prepare(name, code):
    key = Path(os.environ["KEYSTORE_FILE"])
    key.parent.mkdir(parents=True, exist_ok=True)
    key.write_bytes(base64.b64decode("".join(os.environ["ANDROID_KEYSTORE_BASE64"].split()), validate=True))
    key.chmod(0o600)
    certificate = key.with_suffix(".certificate.der")
    try:
        subprocess.run(["keytool", "-exportcert", "-keystore", str(key), "-alias", os.environ["KEY_ALIAS"],
                        "-storepass:env", "KEYSTORE_PASSWORD", "-file", str(certificate)], check=True, capture_output=True)
        if info(certificate)[1] != SIGNER:
            raise ValueError("ANDROID_KEYSTORE_BASE64 is not the signing key of the existing app")
    finally:
        certificate.unlink(missing_ok=True)
    if not re.fullmatch(r"[A-Za-z0-9_-]{20,200}", os.environ.get("TRUYENPLUS_DRIVE_API_KEY", "")):
        raise ValueError("TRUYENPLUS_DRIVE_API_KEY is missing or malformed")
    patch_version(CONFIG, name, code)
    print("Original signing certificate and Drive configuration verified; build version applied.")


def package(name, code, tools):
    apk = Path("android/build/outputs/apk/standard/debug/android-standard-arm64-v8a-debug.apk")
    def out(*command):
        return subprocess.check_output(list(map(str, command)), text=True, stderr=subprocess.STDOUT)
    manifest = out(tools / "aapt", "dump", "xmltree", apk, "AndroidManifest.xml")
    verify_metadata(out(tools / "aapt", "dump", "badging", apk), manifest,
                    out(tools / "apksigner", "verify", "--verbose", "--print-certs", apk), name, code)
    if "truyenplus.DRIVE_API_KEY" not in manifest or "releases/latest/download/update.json" not in manifest:
        raise ValueError("Required updater/Drive configuration missing")
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        for lib in ("libsea_g2p_rs.so", "libvieneu-nano-jni.so", "libsherpa-onnx-jni.so", "libonnxruntime.so"):
            if "lib/arm64-v8a/" + lib not in names:
                raise ValueError("Offline speech runtime missing: " + lib)
        if any(n.endswith((".onnx", ".tar.bz2")) or Path(n).name in ("visual-books.json", "reference-cover.jpg", "reference-meta.json") for n in names):
            raise ValueError("Test fixtures or voice weights must not ship in the APK")
        for dex in (n for n in names if re.fullmatch(r"classes\d*\.dex", n)):
            data = archive.read(dex)
            if any(marker in data for marker in (b"ReaderUiDeviceTest", b"ReaderTtsDeviceTest", b"ReaderGlassDeviceTest")):
                raise ValueError("QA code must not ship in the APK")
    dist = Path("dist/ci")
    dist.mkdir(parents=True, exist_ok=True)
    shipped = dist / f"TruyenPlus-v{name}-arm64-v8a.apk"
    shutil.copy2(apk, shipped)
    api = GitHub()
    previous = api.api("/releases/latest", missing_ok=True)
    revision = os.environ["GITHUB_SHA"]
    commits = out("git", "log", ((previous["tag_name"] + "..") if previous else "") + revision, "--format=- %s", "-30").strip()
    notes = dist / "release-notes.md"
    notes.write_text(f"Bản cập nhật tự động Truyện+ {name}.\n\n{commits}\n\nNguồn: `{revision}`.\nCài đè bản cũ để giữ thư viện và tiến độ.\n")
    subprocess.run(["python3", "scripts/create-update-manifest.py", "--apk", str(shipped), "--aapt", str(tools / "aapt"),
                    "--download-base-url", f"https://github.com/{api.repo}/releases/download/v{name}",
                    "--notes-file", str(notes), "--output", str(dist / "update.json")], check=True)
    files = [shipped, dist / "update.json"]
    (dist / "SHA256SUMS").write_text("".join(f"{info(p)[1]}  {p.name}\n" for p in files))
    print("Installable ARM64 APK, original signer, runtimes and update manifest verified.")


def publish(name):
    api = GitHub()
    sha = os.environ["GITHUB_SHA"]
    if not api.current(sha):
        print("A newer main revision exists; this build will remain an artifact only.")
        return
    tag = "v" + name
    dist = Path("dist/ci")
    manifest = json.loads((dist / "update.json").read_text())
    if manifest["packageName"] != PACKAGE or manifest["versionName"] != name:
        raise ValueError("Publication metadata mismatch")
    files = [dist / Path(urllib.parse.urlparse(manifest["assets"][0]["url"]).path).name, dist / "update.json", dist / "SHA256SUMS"]
    expected = {p.name: info(p) for p in files}
    release = api.api("/releases/tags/" + tag, missing_ok=True)
    if release and release_state(release, sha) == "done":
        asset = next(a for a in release["assets"] if a["name"] == "update.json")
        with download(asset["browser_download_url"]) as response:
            published = json.load(response)
        if (published["packageName"], published["versionName"], published["versionCode"]) != (PACKAGE, name, manifest["versionCode"]):
            raise ValueError("Published version does not match this workflow run")
        # A rebuild need not be byte-identical. Verify the original public assets,
        # rather than replacing them with newly signed bytes during a rerun.
        verify_public(api.repo, published)
        return
    release = release or api.api("/releases", "POST", {"tag_name": tag, "target_commitish": sha,
        "name": "Truyện+ " + name, "body": (dist / "release-notes.md").read_text(), "draft": True, "prerelease": False})
    for path in files:
        fresh = api.api("/releases/" + str(release["id"]))
        if release_state(fresh, sha) != "resume":
            raise ValueError("Refusing to change assets of a published release")
        existing = next((a for a in fresh["assets"] if a["name"] == path.name), None)
        if existing and (existing["size"], existing.get("digest")) == (expected[path.name][0], "sha256:" + expected[path.name][1]):
            continue
        if existing:
            api.api("/releases/assets/" + str(existing["id"]), "DELETE")
        url = release["upload_url"].split("{", 1)[0] + "?" + urllib.parse.urlencode({"name": path.name})
        api.api(url, "POST", binary=path.read_bytes())
    verify_assets(expected, api.api("/releases/" + str(release["id"]))["assets"])
    if not api.current(sha):
        print("Newer main revision detected before publication; leaving the verified draft unpublished.")
        return
    api.api("/releases/" + str(release["id"]), "PATCH", {"draft": False, "make_latest": "true"})
    verify_public(api.repo, manifest)


def verify_public(repo, manifest):
    for attempt in range(10):
        try:
            with download(f"https://github.com/{repo}/releases/latest/download/update.json") as response:
                public = json.load(response)
            if public != manifest:
                raise ValueError("Public updater has not refreshed yet")
            asset = public["assets"][0]
            digest = hashlib.sha256()
            size = 0
            with download(asset["url"]) as response:
                while block := response.read(1024 * 1024):
                    size += len(block)
                    digest.update(block)
            if (size, digest.hexdigest()) != (asset["sizeBytes"], asset["sha256"]):
                raise ValueError("Public APK integrity verification failed")
            print(f"Published and publicly verified https://github.com/{repo}/releases/tag/v{manifest['versionName']}")
            return
        except (OSError, ValueError):
            if attempt == 9:
                raise
            time.sleep(10)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("plan", "prepare", "package", "publish"))
    parser.add_argument("--name")
    parser.add_argument("--code", type=int)
    parser.add_argument("--tools", type=Path)
    args = parser.parse_args()
    if args.command != "plan" and not re.fullmatch(r"\d+\.\d+\.\d+", args.name or ""):
        parser.error("A numeric release --name is required")
    if args.command in ("prepare", "package") and not (args.code and 0 < args.code < 2_100_000_000):
        parser.error("A valid --code is required")
    if args.command == "plan":
        plan()
    elif args.command == "prepare":
        prepare(args.name, args.code)
    elif args.command == "package":
        if args.tools is None:
            parser.error("--tools is required")
        package(args.name, args.code, args.tools)
    else:
        publish(args.name)


if __name__ == "__main__":
    main()
