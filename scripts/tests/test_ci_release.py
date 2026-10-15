import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
import io
import json
import os

spec = importlib.util.spec_from_file_location("ci_release", Path(__file__).parents[1] / "ci_release.py")
ci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ci)


class VersionTest(unittest.TestCase):
    def test_failed_runs_leave_gaps_without_reusing_versions(self):
        self.assertEqual(ci.version_for_run("2.0.46", 100, 1), ("2.0.47", 101))
        self.assertEqual(ci.version_for_run("2.0.46", 100, 3), ("2.0.49", 103))

    def test_rerun_is_deterministic_and_invalid_numbers_fail(self):
        self.assertEqual(ci.version_for_run("2.0.46", 100, 2), ci.version_for_run("2.0.46", 100, 2))
        for run in (0, -1, 2_100_000_000):
            with self.assertRaises(ValueError):
                ci.version_for_run("2.0.46", 100, run)

    def test_patch_is_consistent_and_rejects_ambiguous_config(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ProjectConfig.kt"
            path.write_text('const val versionName = "2.0.46"\nconst val versionCode = 100\n')
            ci.patch_version(path, "2.0.48", 102)
            self.assertIn('versionName = "2.0.48"', path.read_text())
            self.assertIn('versionCode = 102', path.read_text())
            path.write_text(path.read_text() + 'const val versionCode = 2\n')
            with self.assertRaises(ValueError):
                ci.patch_version(path, "2.0.49", 103)


class PublicationSafetyTest(unittest.TestCase):
    def test_superseded_commit_never_creates_a_release(self):
        api = Mock()
        api.current.return_value = False
        with patch.object(ci, "GitHub", return_value=api), patch.dict(os.environ, {"GITHUB_SHA": "abc"}):
            ci.publish("2.0.47")
        api.api.assert_not_called()

    def test_published_rerun_verifies_original_bytes_without_overwriting(self):
        api = Mock(repo="phanan04/truyenplus")
        api.current.return_value = True
        api.api.return_value = {"draft": False, "target_commitish": "abc", "assets": [{"name": "update.json", "browser_download_url": "https://example.org/update.json"}]}
        published = {"packageName": ci.PACKAGE, "versionName": "2.0.47", "versionCode": 101, "assets": [{"url": "https://example.org/old.apk", "sha256": "old"}]}
        rebuilt = {**published, "assets": [{"url": "https://example.org/app.apk", "sha256": "new"}]}
        old_directory = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                dist = Path("dist/ci"); dist.mkdir(parents=True)
                (dist / "update.json").write_text(json.dumps(rebuilt))
                (dist / "app.apk").write_bytes(b"new rebuild")
                (dist / "SHA256SUMS").write_text("new checksums")
                with patch.object(ci, "GitHub", return_value=api), patch.dict(os.environ, {"GITHUB_SHA": "abc"}), patch.object(ci, "download", return_value=io.BytesIO(json.dumps(published).encode())), patch.object(ci, "verify_public") as verify:
                    ci.publish("2.0.47")
                verify.assert_called_once_with("phanan04/truyenplus", published)
                api.api.assert_called_once_with("/releases/tags/v2.0.47", missing_ok=True)
            finally:
                os.chdir(old_directory)

    def test_only_own_draft_can_be_resumed(self):
        own = {"draft": True, "target_commitish": "abc"}
        self.assertEqual(ci.release_state(own, "abc"), "resume")
        self.assertEqual(ci.release_state({**own, "draft": False}, "abc"), "done")
        for draft in (True, False):
            with self.assertRaises(ValueError):
                ci.release_state({"draft": draft, "target_commitish": "different"}, "abc")

    def test_corrupt_or_missing_uploaded_assets_prevent_publication(self):
        expected = {"app.apk": (128, "a" * 64)}
        good = [{"name": "app.apk", "size": 128, "digest": "sha256:" + "a" * 64}]
        ci.verify_assets(expected, good)
        for assets in ([], [{**good[0], "size": 127}], [{**good[0], "digest": "sha256:" + "b" * 64}]):
            with self.assertRaises(ValueError):
                ci.verify_assets(expected, assets)

    def test_wrong_signer_package_and_test_only_apk_are_blocked(self):
        badge = "package: name='ir.kazemcodes.infinityreader.debug' versionCode='101' versionName='2.0.47'\nsdkVersion:'26'\nnative-code: 'arm64-v8a'"
        cert = "Verified using v2 scheme (APK Signature Scheme v2): true\nSigner #1 certificate SHA-256 digest: " + ci.SIGNER
        ci.verify_metadata(badge, "", cert, "2.0.47", 101)
        for b, manifest, signer in (
            (badge.replace(".debug'", ".debug.qa'"), "", cert),
            (badge, "android:testOnly(0x01010272)=(type 0x12)0xffffffff", cert),
            (badge, "", cert.replace(ci.SIGNER, "b" * 64)),
            (badge.replace("versionCode='101'", "versionCode='100'"), "", cert),
        ):
            with self.assertRaises(ValueError):
                ci.verify_metadata(b, manifest, signer, "2.0.47", 101)

    def test_debuggable_runtime_is_not_published_as_an_update(self):
        badge = "package: name='ir.kazemcodes.infinityreader.debug' versionCode='101' versionName='2.0.47'\nsdkVersion:'26'\nnative-code: 'arm64-v8a'"
        cert = "Verified using v2 scheme (APK Signature Scheme v2): true\nSigner #1 certificate SHA-256 digest: " + ci.SIGNER
        with self.assertRaisesRegex(ValueError, "debuggable"):
            ci.verify_metadata(badge, "android:debuggable(0x0101000f)=(type 0x12)0xffffffff", cert, "2.0.47", 101)


if __name__ == "__main__":
    unittest.main()
