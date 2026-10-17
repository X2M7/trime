#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Release boundary tests using tiny real Git repositories and APK ZIPs.

Android tools are substituted only for the APK inspector's external protocol;
these tests do not claim cryptographic signature or device upgrade coverage.
"""
import importlib.util
import json
from pathlib import Path
import struct
import subprocess
import tarfile
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile


SPEC = importlib.util.spec_from_file_location("trime_release", Path(__file__).with_name("release.py"))
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)

CERT = "c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2"
PROPERTIES = (
    "formatVersion=1\n"
    "versionName=3.3.13-t9.17\n"
    "versionCode=20261122\n"
    "previousVersionCode=20261121\n"
    "repository=X2M7/trime\n"
    "branch=develop\n"
    "applicationId=com.osfans.trime.debug\n"
    f"certificateSha256={CERT}\n"
    "abis=arm64-v8a,x86_64\n"
)


def git(folder, *args):
    result = subprocess.run(
        ["git", "-c", "protocol.file.allow=always", *args], cwd=folder,
        capture_output=True, check=True,
    )
    return result.stdout.decode().strip()


def initialize(folder):
    folder.mkdir()
    git(folder, "init", "-q")
    git(folder, "config", "user.name", "Release boundary test")
    git(folder, "config", "user.email", "release-test@example.invalid")
    git(folder, "config", "commit.gpgsign", "false")


def commit(folder, message="test source"):
    git(folder, "add", "--all")
    git(folder, "commit", "-qm", message)


def elf(abi="arm64-v8a"):
    data = bytearray(64)
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", data, 18, 183 if abi == "arm64-v8a" else 62)
    return bytes(data)


class MetadataTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "release.properties"
        self.path.write_text(PROPERTIES)

    def test_accepts_monotonic_release_identity(self):
        result = release.metadata(self.path)
        self.assertEqual(result["versionCode"], 20261122)
        self.assertEqual(result["tag"], "v3.3.13-t9.17")

    def test_rejects_duplicate_unknown_or_incomplete_properties(self):
        for text in (
            PROPERTIES + "versionCode=20261123\n",
            PROPERTIES + "unexpected=1\n",
            PROPERTIES.replace("formatVersion=1\n", ""),
        ):
            with self.subTest(text=text):
                self.path.write_text(text)
                with self.assertRaises(ValueError):
                    release.metadata(self.path)

    def test_cannot_retarget_publisher_repository_or_branch(self):
        for old, new in (("X2M7/trime", "osfans/trime"), ("branch=develop", "branch=main")):
            with self.subTest(new=new):
                self.path.write_text(PROPERTIES.replace(old, new))
                with self.assertRaises(ValueError):
                    release.metadata(self.path)

    def test_old_20261101_cannot_be_reenabled_by_lowering_previous_code(self):
        self.path.write_text(PROPERTIES.replace("20261122", "20261101").replace("20261121", "20261100"))
        with self.assertRaises(ValueError):
            release.metadata(self.path)

    def test_rejects_equal_or_overflow_version_codes(self):
        for code in ("20261121", "2100000001", "-1", "020261122"):
            with self.subTest(code=code):
                self.path.write_text(PROPERTIES.replace("versionCode=20261122", "versionCode=" + code))
                with self.assertRaises(ValueError):
                    release.metadata(self.path)

    def test_rejects_ambiguous_abi_or_certificate_metadata(self):
        for old, new in (("abis=arm64-v8a,x86_64", "abis=x86_64,arm64-v8a"),
                         ("abis=arm64-v8a,x86_64", "abis=arm64-v8a,arm64-v8a"),
                         (CERT, "abcd")):
            with self.subTest(new=new):
                self.path.write_text(PROPERTIES.replace(old, new))
                with self.assertRaises(ValueError):
                    release.metadata(self.path)


class ApkInspectionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        props = self.root / "release.properties"
        props.write_text(PROPERTIES)
        self.meta = release.metadata(props)
        self.apk = self.root / "input.apk"
        self.output = self.root / "inspection"
        self.output.mkdir()
        self.badging = (
            "package: name='com.osfans.trime.debug' versionCode='20261122' "
            "versionName='3.3.13-t9.17'\nminSdkVersion:'21'\ntargetSdkVersion:'37'\n"
        )
        self.signature = (
            "Verifies\nVerified using v1 scheme (JAR signing): true\n"
            "Verified using v2 scheme (APK Signature Scheme v2): true\n"
            "Number of signers: 1\n"
            f"Signer #1 certificate SHA-256 digest: {CERT}\n"
        )
        self.signature_stderr = b""
        self.write_apk()

    def write_apk(self, machine="arm64-v8a", entries=None):
        with zipfile.ZipFile(self.apk, "w") as archive:
            for name, data in (entries or {
                "lib/arm64-v8a/librime.so": elf(machine),
                "assets/rime/test.dict.yaml": b"version: 1\n",
            }).items():
                archive.writestr(name, data)

    def tool_output(self, argv, cwd=None, output=None):
        args = [str(arg) for arg in argv]
        if "badging" in args:
            raw, err = self.badging.encode(), b""
        elif "verify" in args:
            raw, err = self.signature.encode(), self.signature_stderr
        elif "zipalign" in args[0]:
            raw, err = b"", b""
        else:
            raise AssertionError("Unexpected Android tool call: " + str(args))
        output.with_suffix(".stdout").write_bytes(raw)
        output.with_suffix(".stderr").write_bytes(err)
        return raw

    def inspect(self):
        with patch.object(release, "command", side_effect=self.tool_output):
            return release.inspect_apk(self.apk, "arm64-v8a", self.meta,
                                       self.root / "sdk", self.root / "java", self.output)

    def test_accepts_actual_aapt_min_sdk_spelling(self):
        result = self.inspect()
        self.assertEqual(result["min_sdk"], 21)
        self.assertEqual(result["certificate_sha256"], CERT)
        self.assertEqual(list(result["native_libraries"]), ["lib/arm64-v8a/librime.so"])

    def test_rejects_apk_with_different_package_version_name_or_code(self):
        original = self.badging
        for old, new in (("com.osfans.trime.debug", "com.osfans.trime"),
                         ("20261122", "20261101"), ("3.3.13-t9.17", "3.3.13-t9.16")):
            with self.subTest(new=new):
                self.badging = original.replace(old, new)
                with self.assertRaises(ValueError):
                    self.inspect()

    def test_rejects_debuggable_apk_even_if_named_release(self):
        self.badging += "application-debuggable\n"
        with self.assertRaises(ValueError):
            self.inspect()

    def test_rejects_wrong_certificate_or_multiple_signers(self):
        original = self.signature
        for text in (original.replace(CERT, "a" * 64),
                     original.replace("Number of signers: 1", "Number of signers: 2"),
                     original.replace("Number of signers: 1", "Number of signers: 10")):
            with self.subTest(signature=text):
                self.signature = text
                with self.assertRaises(ValueError):
                    self.inspect()

    def test_rejects_missing_android5_signature_and_signing_diagnostics(self):
        original = self.signature
        for text in (original.replace("JAR signing): true", "JAR signing): false"),
                     original + "WARNING: unsigned ZIP entry\n"):
            with self.subTest(signature=text):
                self.signature = text
                with self.assertRaises(ValueError):
                    self.inspect()
        self.signature = original
        self.signature_stderr = b"verification warning\n"
        with self.assertRaises(ValueError):
            self.inspect()

    def test_rejects_native_machine_disguised_by_abi_directory(self):
        self.write_apk(machine="x86_64")
        with self.assertRaises(ValueError):
            self.inspect()

    def test_rejects_extra_native_abi(self):
        self.write_apk(entries={"lib/arm64-v8a/librime.so": elf(),
                                "lib/x86_64/librime.so": elf("x86_64")})
        with self.assertRaises(ValueError):
            self.inspect()

    def test_short_or_unknown_elf_is_a_validation_error(self):
        for raw, abi in ((b"", "arm64-v8a"), (b"\x7fELF\x02", "arm64-v8a"),
                         (elf()[:19], "arm64-v8a"), (elf(), "unknown-abi")):
            with self.subTest(size=len(raw), abi=abi):
                with self.assertRaises(ValueError):
                    release.check_elf(raw, abi)


class SourceArchiveTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / "main"
        initialize(self.root)
        (self.root / "release.properties").write_text(PROPERTIES)
        (self.root / "app.txt").write_bytes(b"exact source\n")
        commit(self.root)

    def record(self):
        source = release.repositories(self.root)
        record = dict(inspection_passed=True, metadata=release.metadata(self.root / "release.properties"),
                      build_git_sha=source[""], repositories=source)
        path = self.base / "record.json"
        path.write_text(json.dumps(record))
        return path

    def archive(self, record=None):
        destination = self.base / "source.tar.gz"
        result = release.source_archive(record or self.record(), destination, self.root)
        with tarfile.open(destination) as archive:
            members = {member.name.split("/", 1)[1]: member for member in archive.getmembers()}
            content = {name: archive.extractfile(member).read() for name, member in members.items() if member.isfile()}
        return result, members, content

    def test_rejects_modified_tracked_or_untracked_source(self):
        (self.root / "app.txt").write_text("changed\n")
        with self.assertRaises(ValueError):
            release.repositories(self.root)
        git(self.root, "checkout", "--", "app.txt")
        (self.root / "extra.java").write_text("class Extra {}\n")
        with self.assertRaises(ValueError):
            release.repositories(self.root)

    def test_ignored_build_outputs_do_not_contaminate_source_archive(self):
        (self.root / ".gitignore").write_text("build/\n")
        commit(self.root)
        (self.root / "build").mkdir()
        (self.root / "build" / "not-source.bin").write_bytes(b"generated")
        _, members, content = self.archive()
        self.assertNotIn("build/not-source.bin", members)
        self.assertEqual(content["app.txt"], b"exact source\n")

    def test_export_attributes_cannot_drop_or_substitute_git_source(self):
        (self.root / ".gitattributes").write_text("private.txt export-ignore\nsubstitution.txt export-subst\n")
        (self.root / "private.txt").write_bytes(b"required source\n")
        (self.root / "substitution.txt").write_bytes(b"$Format:%H$\n")
        commit(self.root)
        _, _, content = self.archive()
        self.assertEqual(content.get("private.txt"), b"required source\n")
        self.assertEqual(content.get("substitution.txt"), b"$Format:%H$\n")

    def test_preserves_committed_symlink_as_symlink(self):
        (self.root / "alias.txt").symlink_to("app.txt")
        commit(self.root)
        _, members, _ = self.archive()
        self.assertTrue(members["alias.txt"].issym())
        self.assertEqual(members["alias.txt"].linkname, "app.txt")

    def test_recursive_submodule_files_present_and_dirty_checkout_rejected(self):
        child = self.base / "child"
        initialize(child)
        (child / "native.c").write_text("int native_value = 7;\n")
        commit(child)
        git(self.root, "submodule", "add", "-q", str(child), "deps/native")
        commit(self.root)
        _, _, content = self.archive()
        self.assertEqual(content["deps/native/native.c"], b"int native_value = 7;\n")
        (self.root / "deps/native/native.c").write_text("int native_value = 8;\n")
        with self.assertRaises(ValueError):
            release.repositories(self.root)

    def test_only_exact_generated_plugin_links_are_allowed(self):
        # Match the real gitlink layout and .gitmodules dirty-ignore settings:
        # repositories() must still inspect each child and reject other dirt.
        modules = ("librime", "librime-lua", "librime-lua-deps",
                   "librime-octagram", "librime-predict")
        for name in modules:
            child = self.base / name
            initialize(child)
            (child / "source.txt").write_text(name + "\n")
            if name == "librime":
                (child / "plugins").mkdir()
                (child / "plugins/README.md").write_text("Generated plugin links\n")
            commit(child)
            relative = "app/src/main/jni/" + name
            git(self.root, "submodule", "add", "-q", str(child), relative)
            if name in ("librime", "librime-lua"):
                git(self.root, "config", "-f", ".gitmodules", "submodule." + relative + ".ignore", "dirty")
        commit(self.root)
        before = release.repositories(self.root)
        for relative, target in release.GENERATED_LINKS.items():
            (self.root / relative).symlink_to(self.root / target, target_is_directory=True)
        self.assertEqual(len(release.GENERATED_LINKS), 4)
        self.assertEqual(release.repositories(self.root), before)
        for relative, target in release.GENERATED_LINKS.items():
            link = self.root / relative
            with self.subTest(path=relative, mutation="wrong target"):
                link.unlink()
                link.symlink_to(self.root / "app.txt")
                with self.assertRaises(ValueError):
                    release.repositories(self.root)
            with self.subTest(path=relative, mutation="regular file"):
                link.unlink()
                link.write_text("Not a generated symlink\n")
                with self.assertRaises(ValueError):
                    release.repositories(self.root)
            link.unlink()
            link.symlink_to(self.root / target, target_is_directory=True)
        extra = self.root / "app/src/main/jni/librime/plugins/unreviewed-plugin"
        extra.symlink_to(self.root / "app/src/main/jni/librime-predict", target_is_directory=True)
        with self.assertRaises(ValueError):
            release.repositories(self.root)

    def test_changed_application_source_after_freeze_rejected(self):
        record = self.record()
        (self.root / "app.txt").write_text("new implementation\n")
        commit(self.root)
        with self.assertRaises(ValueError):
            self.archive(record)

    def test_publication_documentation_commit_keeps_build_and_publication_identity(self):
        record = self.record()
        build = json.loads(record.read_text())["build_git_sha"]
        (self.root / "docs").mkdir()
        (self.root / "docs" / "release.html").write_text("publication record\n")
        commit(self.root)
        _, _, content = self.archive(record)
        manifest = json.loads(content["BUILD-MANIFEST.json"])
        self.assertEqual(manifest["build_git_sha"], build)
        self.assertEqual(manifest["publication_git_sha"], git(self.root, "rev-parse", "HEAD"))


class BuildRecordTests(unittest.TestCase):
    """A successful tool exit alone cannot attest an APK's source identity."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / "source"
        initialize(self.root)
        (self.root / "release.properties").write_text(PROPERTIES)
        commit(self.root)
        self.meta = release.metadata(self.root / "release.properties")
        self.sources = release.repositories(self.root)
        self.build_dir = self.base / "successful-build"
        self.build_dir.mkdir()
        self.apks = self.build_dir / "apks"
        self.apks.mkdir()
        for abi in self.meta["abis"]:
            (self.apks / f"trime-{abi}-release.apk").write_bytes(b"frozen " + abi.encode())
        self.env = self.build_dir / "environment.json"
        self.env.write_text(json.dumps({"BUILD_COMMIT_HASH": self.sources[""],
                                        "BUILD_VERSION_NAME": self.meta["versionName"]}))
        self.command = self.build_dir / "command.json"
        self.command.write_text('["./gradlew", ":app:assembleRelease"]\n')
        self.log = self.build_dir / "command.log"
        self.log.write_text("BUILD SUCCESSFUL\n")
        self.config = self.build_dir / "BuildConfig.java"
        self.config.write_text(f'BUILD_COMMIT_HASH = "{self.sources[""]}";\n'
                               f'VERSION_NAME = "{self.meta["versionName"]}";\nDEBUG = false;\n')
        self.source_record = self.build_dir / "source.json"
        self.source_record.write_text(json.dumps(self.sources))
        self.result = {
            "phase": "release", "exit_code": 0, "stopped_reason": None,
            "source_unchanged": True, "build_git_sha": self.sources[""],
            "environment": release.binding(self.env), "command": release.binding(self.command),
            "log": release.binding(self.log), "build_config": release.binding(self.config),
            "source": release.binding(self.source_record),
            "metadata": release.binding(self.root / "release.properties"),
            "apks": [release.binding(path) for path in sorted(self.apks.glob("*.apk"))],
        }
        self.result_path = self.build_dir / "result.json"
        self.args = SimpleNamespace(output=self.base / "inspection", apks=self.apks,
                                    build_result=self.result_path, sdk=self.base / "sdk", java=self.base / "java")

    def record(self):
        self.result_path.write_text(json.dumps(self.result))
        real_repositories = release.repositories
        with patch.object(release, "ROOT", self.root), \
                patch.object(release, "metadata", return_value=self.meta), \
                patch.object(release, "repositories", side_effect=lambda: real_repositories(self.root)), \
                patch.object(release, "inspect_apk", side_effect=lambda path, abi, *_: {
                    "apk": release.binding(path), "abi": abi, "assets": {"dictionary": "same"}}):
            return release.build_record(self.args)

    def test_frozen_successful_build_provenance_is_retained(self):
        row = self.record()
        self.assertEqual(row["build_git_sha"], self.sources[""])
        self.assertEqual(row["build_result"], release.binding(self.result_path))
        self.assertFalse(row["device_acceptance"])
        self.assertFalse(row["published"])

    def test_success_log_cannot_override_failed_or_interrupted_build(self):
        self.result["exit_code"] = 1
        with self.assertRaises(ValueError):
            self.record()

    def test_build_from_old_source_is_rejected(self):
        self.result["build_git_sha"] = "0" * 40
        with self.assertRaises(ValueError):
            self.record()

    def test_incomplete_freeze_is_rejected_despite_gradle_success(self):
        self.result["freeze_error"] = "Unable to copy BuildConfig"
        with self.assertRaises(ValueError):
            self.record()

    def test_recursive_source_map_cannot_be_substituted(self):
        self.source_record.write_text(json.dumps(dict(self.sources, **{"deps/native": "0" * 40})))
        self.result["source"] = release.binding(self.source_record)
        with self.assertRaises(ValueError):
            self.record()

    def test_build_result_cannot_bind_unrelated_metadata_file(self):
        other = self.build_dir / "unrelated-release.properties"
        other.write_text(PROPERTIES.replace("versionCode=20261122", "versionCode=20261123"))
        self.result["metadata"] = release.binding(other)
        with self.assertRaises(ValueError):
            self.record()

    def test_build_environment_with_different_version_is_rejected(self):
        env = json.loads(self.env.read_text())
        env["BUILD_VERSION_NAME"] = "3.3.13-t9.16"
        self.env.write_text(json.dumps(env))
        self.result["environment"] = release.binding(self.env)
        with self.assertRaises(ValueError):
            self.record()

    def test_generated_debug_build_config_is_rejected(self):
        self.config.write_text(self.config.read_text().replace("DEBUG = false", "DEBUG = true"))
        self.result["build_config"] = release.binding(self.config)
        with self.assertRaises(ValueError):
            self.record()

    def test_frozen_apk_replacement_is_rejected(self):
        next(self.apks.glob("*.apk")).write_bytes(b"replacement APK")
        with self.assertRaises(ValueError):
            self.record()

    def test_changed_generated_config_is_rejected_even_if_semantics_still_match(self):
        with self.config.open("a") as stream:
            stream.write("// changed after freeze\n")
        with self.assertRaises(ValueError):
            self.record()


class PagesTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "release.properties").write_text(PROPERTIES)
        (self.root / "docs").mkdir()
        (self.root / "docs/index.template.html").write_text(
            "<p>@@VERSION_NAME@@ @@VERSION_CODE@@ @@APPLICATION_ID@@</p>"
            '<a href="@@RELEASE_URL@@">@@TAG@@</a><p>@@RELEASE_STATUS@@</p>'
        )

    def test_generated_page_uses_fixed_fork_release_without_claiming_publication(self):
        with patch.object(release, "ROOT", self.root):
            release.pages()
            release.pages(check=True)
        data = json.loads((self.root / "docs/release.json").read_text(encoding="utf-8"))
        self.assertEqual(data["release_url"], "https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.17")
        self.assertIn("线上发布状态", data["status"])
        self.assertNotIn("已发布", data["status"])

    def test_utf8_pages_round_trip_with_windows_legacy_text_defaults(self):
        template = "<p>“虚构中文版本”\u2014@@VERSION_NAME@@</p><p>@@RELEASE_STATUS@@</p>"
        (self.root / "docs/index.template.html").write_bytes(template.encode("utf-8"))
        real_read, real_write = Path.read_text, Path.write_text

        def legacy_read(path, *args, **kwargs):
            if not args and "encoding" not in kwargs:
                kwargs["encoding"] = "cp1252"
            return real_read(path, *args, **kwargs)

        def legacy_write(path, data, *args, **kwargs):
            if not args and "encoding" not in kwargs:
                kwargs["encoding"] = "cp1252"
            return real_write(path, data, *args, **kwargs)

        with patch.object(release, "ROOT", self.root), \
                patch.object(Path, "read_text", new=legacy_read), \
                patch.object(Path, "write_text", new=legacy_write):
            release.pages()
            release.pages(check=True)

        page = (self.root / "docs/index.html").read_bytes().decode("utf-8")
        data = json.loads((self.root / "docs/release.json").read_bytes())
        self.assertIn("“虚构中文版本”\u20143.3.13-t9.17", page)
        self.assertIn(release.page_data(release.metadata(self.root / "release.properties"))["status"], page)
        self.assertEqual(data, release.page_data(release.metadata(self.root / "release.properties")))

    def test_stale_generated_page_or_unknown_template_token_is_rejected(self):
        with patch.object(release, "ROOT", self.root):
            release.pages()
            (self.root / "docs/index.html").write_text("stale release\n")
            with self.assertRaises(ValueError):
                release.pages(check=True)
            (self.root / "docs/index.template.html").write_text("@@UNKNOWN_TOKEN@@")
            with self.assertRaises(ValueError):
                release.pages()


if __name__ == "__main__":
    unittest.main()
