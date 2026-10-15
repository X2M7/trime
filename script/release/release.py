#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Inspect and freeze release inputs. Never derives versionCode from a clock.

Only this module and Gradle read release.properties. No credentials or remote
writes are needed for metadata, APK inspection, or recursive source packaging.
"""
import argparse
import gzip
import hashlib
import html
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import struct
import subprocess
import tarfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FIELDS = {"formatVersion", "versionName", "versionCode", "previousVersionCode",
          "repository", "branch", "applicationId", "certificateSha256", "abis"}
MACHINES = {"arm64-v8a": (2, 183), "x86_64": (2, 62), "armeabi-v7a": (1, 40), "x86": (1, 3)}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def metadata(path=None):
    values = {}
    for line in (path or ROOT / "release.properties").read_text().splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        require(re.fullmatch(r"[A-Za-z][A-Za-z0-9]*=[A-Za-z0-9.,/_-]+", line), "Use plain key=value metadata")
        key, value = line.split("=", 1)
        require(key not in values, "Duplicate release property: " + key)
        values[key] = value
    require(set(values) == FIELDS and values["formatVersion"] == "1", "Unknown release metadata format")
    require(values["repository"] == "X2M7/trime" and values["branch"] == "develop", "Only X2M7/trime:develop may publish")
    require(re.fullmatch(r"[0-9]+(?:\.[0-9]+){2}(?:-[A-Za-z0-9.-]+)?", values["versionName"]), "Invalid versionName")
    require(re.fullmatch(r"[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)+", values["applicationId"]), "Invalid package name")
    require(re.fullmatch(r"[0-9a-f]{64}", values["certificateSha256"]), "Invalid signing certificate")
    for key in ("versionCode", "previousVersionCode"):
        require(re.fullmatch(r"[1-9][0-9]*", values[key]), "Invalid versionCode")
        values[key] = int(values[key])
    # All 26 public APKs were inspected on 2026-10-15. Never reset this floor to a date.
    require(20261121 <= values["previousVersionCode"] < values["versionCode"] <= 2100000000, "versionCode must exceed the published maximum")
    values["abis"] = values["abis"].split(",")
    require(values["abis"] == ["arm64-v8a", "x86_64"], "This release channel ships ARM64 and x86_64 separately")
    values["tag"] = "v" + values["versionName"]
    return values


def digest(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def binding(path):
    path = Path(path).resolve(strict=True)
    before = path.stat()
    result = dict(path=str(path), bytes=before.st_size, sha256=digest(path))
    after = path.stat()
    require((before.st_ino, before.st_size, before.st_mtime_ns, before.st_ctime_ns) ==
            (after.st_ino, after.st_size, after.st_mtime_ns, after.st_ctime_ns), "Input changed: " + str(path))
    return result


def verify_binding(row):
    require(binding(row["path"]) == {k: row[k] for k in ("path", "bytes", "sha256")}, "Frozen input changed")


def write_json(path, value):
    with Path(path).open("x", encoding="utf-8") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)
        stream.write("\n")


def command(argv, cwd=ROOT, output=None):
    result = subprocess.run([str(x) for x in argv], cwd=cwd, capture_output=True, timeout=180, check=False)
    if output:
        output.with_suffix(".stdout").write_bytes(result.stdout)
        output.with_suffix(".stderr").write_bytes(result.stderr)
        write_json(output.with_suffix(".command.json"), {"argv": [str(x) for x in argv], "exit_code": result.returncode})
    require(result.returncode == 0, "Command failed: " + str(argv[0]))
    return result.stdout


def git(*args, cwd=ROOT):
    return command(["git", *args], cwd)


def repositories(root=ROOT):
    """Traverse gitlinks at each pinned commit, including nested modules."""
    result = {}
    def visit(folder, prefix):
        require(not git("status", "--porcelain", "--untracked-files=normal", cwd=folder), "Uncommitted source: " + prefix)
        sha = git("rev-parse", "HEAD", cwd=folder).decode().strip()
        result[prefix] = sha
        for line in git("ls-tree", "-rz", "HEAD", cwd=folder).split(b"\0"):
            if not line:
                continue
            info, name = line.split(b"\t", 1)
            mode, kind, oid = info.decode().split()
            if kind == "commit":
                child = name.decode()
                require(git("rev-parse", "HEAD", cwd=folder / child).decode().strip() == oid, "Submodule checkout differs: " + child)
                visit(folder / child, (prefix + "/" if prefix else "") + child)
    visit(root, "")
    return result


def check_elf(raw, abi):
    require(abi in MACHINES and len(raw) >= 20, "Invalid ELF header or ABI")
    require(raw[:4] == b"\x7fELF" and raw[5] == 1, "Expected little-endian ELF")
    require((raw[4], struct.unpack_from("<H", raw, 18)[0]) == MACHINES[abi], "ELF machine differs from ABI directory")


def inspect_apk(path, abi, meta, sdk, java, output):
    before = binding(path)
    tools = sdk / "build-tools/36.0.0"
    badging = command([tools / "aapt2", "dump", "badging", path], output=output / (abi + "-badging")).decode()
    lines = badging.splitlines()
    package_lines = [line for line in lines if line.startswith("package: ")]
    require(len(package_lines) == 1, "APK package header not unique")
    package = dict(re.findall(r"(\w+)='([^']*)'", package_lines[0]))
    require((package.get("name"), package.get("versionName"), package.get("versionCode")) ==
            (meta["applicationId"], meta["versionName"], str(meta["versionCode"])), "APK differs from release.properties")
    require("application-debuggable" not in lines, "Debuggable APK cannot enter the release channel")
    require(re.findall(r"(?m)^(?:sdkVersion|minSdkVersion):'([0-9]+)'$", badging) == ["21"], "Android 5.0 support changed")
    signature = command([java, "-Xmx256m", "-jar", tools / "lib/apksigner.jar", "verify", "--verbose", "--print-certs", path],
                        output=output / (abi + "-signature")).decode()
    require(not re.search(r"\b(?:WARNING|ERROR)\b", signature, re.I) and
            not (output / (abi + "-signature.stderr")).read_bytes(), "Signature verification diagnostics remain")
    require(re.findall(r"(?m)^Signer #1 certificate SHA-256 digest: ([a-f0-9]+)$", signature) == [meta["certificateSha256"]] and
            re.findall(r"(?m)^Number of signers: ([0-9]+)$", signature) == ["1"], "Unexpected signer")
    for scheme in ("v1 scheme (JAR signing)", "v2 scheme (APK Signature Scheme v2)"):
        require("Verified using " + scheme + ": true" in signature, "Required signing scheme missing")
    command([tools / "zipalign", "-c", "-P", "16", "4", path], output=output / (abi + "-alignment"))
    libraries, resources = {}, {}
    with zipfile.ZipFile(path) as apk:
        names = apk.namelist()
        require(len(names) == len(set(names)), "Duplicate APK entries")
        native = [name for name in names if name.startswith("lib/") and name.endswith(".so")]
        require(native and {name.split("/")[1] for name in native} == {abi}, "Unexpected APK native ABI")
        for name in native:
            raw = apk.read(name)
            check_elf(raw, abi)
            libraries[name] = hashlib.sha256(raw).hexdigest()
        for name in names:
            if name.startswith("assets/") and not name.endswith("/"):
                resources[name] = hashlib.sha256(apk.read(name)).hexdigest()
    require(binding(path) == before, "APK changed during inspection")
    return dict(apk=before, abi=abi, package=package["name"], version_name=package["versionName"],
                version_code=int(package["versionCode"]), debuggable=False, min_sdk=21,
                certificate_sha256=meta["certificateSha256"], native_libraries=libraries, assets=resources,
                public_name=f"trime-{meta['tag']}-{abi}.apk")


def build_record(args):
    meta = metadata()
    require(not args.output.exists(), "Use a fresh record directory")
    args.output.mkdir(parents=True)
    source = repositories()
    build_binding = binding(args.build_result)
    build = json.loads(args.build_result.read_text())
    require(binding(args.build_result) == build_binding, "Build result changed while reading")
    require(build["phase"] == "release" and build["exit_code"] == 0 and build["stopped_reason"] is None and
            build["source_unchanged"] and build["build_git_sha"] == source[""] and not build.get("freeze_error"), "APK build does not match current source")
    for key in ("environment", "command", "log", "build_config", "source", "metadata"):
        verify_binding(build[key])
    require(json.loads(Path(build["source"]["path"]).read_text()) == source and
            build["metadata"] == binding(ROOT / "release.properties"), "Recorded build inputs differ")
    env = json.loads(Path(build["environment"]["path"]).read_text())
    require(env["BUILD_COMMIT_HASH"] == source[""] and env["BUILD_VERSION_NAME"] == meta["versionName"], "Build identity differs")
    config = Path(build["build_config"]["path"]).read_text()
    require(f'BUILD_COMMIT_HASH = "{source[""]}";' in config and
            f'VERSION_NAME = "{meta["versionName"]}";' in config and 'DEBUG = false;' in config,
            "Generated Release BuildConfig differs")
    artifacts = []
    for abi in meta["abis"]:
        found = list(args.apks.glob(f"*-{abi}-release.apk"))
        require(len(found) == 1, "Need one Release APK for " + abi)
        require(binding(found[0]) in build["apks"], "APK is not the frozen successful build output")
        artifacts.append(inspect_apk(found[0], abi, meta, args.sdk, args.java, args.output))
    require(artifacts[0]["assets"] == artifacts[1]["assets"], "ABI builds contain different data assets")
    dictionaries = []
    for module in source:
        folder = ROOT / module
        for name in git("ls-files", "-z", cwd=folder).decode().split("\0"):
            if name.endswith(".dict.yaml") and (folder / name).is_file():
                p = folder / name
                declared = re.search(r"(?m)^version:\s*(.+)$", p.read_text())
                dictionaries.append(dict(path=str(p.relative_to(ROOT)), sha256=digest(p),
                                         declared_version=declared.group(1) if declared else None))
    require(source == repositories(), "Source changed while recording")
    require(binding(args.build_result) == build_binding, "Build result changed during inspection")
    record = dict(format_version=1, metadata=meta, build_git_sha=source[""], repositories=source,
                  dictionaries=dictionaries, apks=artifacts, inspection_passed=True,
                  build_result=build_binding, device_acceptance=False, published=False)
    write_json(args.output / "build-record.json", record)
    return record


def source_archive(record_path, destination, root=ROOT):
    record = json.loads(record_path.read_text())
    require(record["inspection_passed"] is True and record["metadata"] == metadata(root / "release.properties"), "Invalid build record")
    current = repositories(root)
    build = record["build_git_sha"]
    # Publication may add only release documentation after the APK freeze.
    if current[""] != build:
        changed = git("diff", "--name-only", build, current[""], cwd=root).decode().splitlines()
        require(changed and all(p.startswith(("doc/releases/", "script/quality/VALIDATION-", "docs/")) or
                               p in ("README.md", "README_sc.md", "README_tc.md") for p in changed), "Source differs from tested build")
    require({k: v for k, v in current.items() if k} == {k: v for k, v in record["repositories"].items() if k}, "Submodules differ from build")
    manifest = dict(record, publication_git_sha=current[""], source_repositories=current)
    prefix = "trime-" + record["metadata"]["tag"] + "/"
    entries = {}
    require(not destination.exists(), "Never overwrite a source archive")
    with destination.open("xb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0, filename="") as compressed, tarfile.open(fileobj=compressed, mode="w|") as output:
        for relative, sha in current.items():
            # Use Git objects directly: export-ignore/export-subst must not omit
            # or rewrite any committed source. One persistent cat-file per repo.
            tree = git("ls-tree", "-rz", sha, cwd=root / relative)
            process = subprocess.Popen(['git', 'cat-file', '--batch'], cwd=root / relative,
                                       stdin=subprocess.PIPE, stdout=subprocess.PIPE)
            try:
                for row in tree.split(b'\0'):
                    if not row:
                        continue
                    info, local = row.split(b'\t', 1)
                    mode, kind, oid = info.decode().split()
                    if kind == 'commit':
                        continue
                    require(kind == 'blob' and mode in ('100644', '100755', '120000'), 'Unsupported Git entry')
                    process.stdin.write(oid.encode() + b'\n')
                    process.stdin.flush()
                    actual_oid, actual_kind, size = process.stdout.readline().decode().split()
                    size = int(size)
                    require(actual_oid == oid and actual_kind == 'blob' and size <= 64 * 1024 * 1024, 'Invalid Git blob')
                    payload = process.stdout.read(size)
                    require(len(payload) == size and process.stdout.read(1) == b'\n', 'Incomplete Git blob')
                    require(hashlib.sha1(b'blob ' + str(size).encode() + b'\0' + payload).hexdigest() == oid, 'Git blob hash differs')
                    name = (relative + "/" if relative else "") + local.decode()
                    require(not PurePosixPath(name).is_absolute() and ".." not in PurePosixPath(name).parts and name not in entries, "Unsafe source path")
                    member = tarfile.TarInfo(prefix + name)
                    member.mtime = 0
                    member.mode = 0o755 if mode == '100755' else 0o644
                    if mode == '120000':
                        member.type, member.linkname = tarfile.SYMTYPE, payload.decode()
                    else:
                        member.size = len(payload)
                    entries[name] = dict(sha256=hashlib.sha256(payload).hexdigest(), bytes=len(payload),
                                         kind="symlink" if member.issym() else "file")
                    output.addfile(member, None if member.issym() else io.BytesIO(payload))
            finally:
                process.stdin.close()
                process.stdout.close()
                if process.poll() is None:
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
            require(process.returncode == 0, 'Git object reader failed')
        for name, value in (("BUILD-MANIFEST.json", manifest), ("SOURCE-FILES.json", entries)):
            require(name not in entries, "Reserved offline manifest name already exists")
            payload = (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()
            info = tarfile.TarInfo(prefix + name)
            info.mode, info.size, info.mtime = 0o644, len(payload), 0
            output.addfile(info, io.BytesIO(payload))
    # Re-open the shipped bytes and verify every recorded file and symlink target.
    seen = {}
    with tarfile.open(destination, "r:gz") as source:
        for member in source:
            name = member.name.removeprefix(prefix)
            if name in entries:
                payload = member.linkname.encode() if member.issym() else source.extractfile(member).read()
                seen[name] = dict(sha256=hashlib.sha256(payload).hexdigest(), bytes=len(payload),
                                  kind="symlink" if member.issym() else "file")
    require(entries == seen and current == repositories(root), "Source archive verification failed")
    return dict(archive=binding(destination), git_files=len(entries), repositories=current)


def page_data(meta):
    return {"version_name": meta["versionName"], "version_code": meta["versionCode"],
            "tag": meta["tag"], "repository": meta["repository"], "branch": meta["branch"],
            "application_id": meta["applicationId"], "certificate_sha256": meta["certificateSha256"],
            "abis": meta["abis"], "release_url": f"https://github.com/{meta['repository']}/releases/tag/{meta['tag']}",
            "source_url": f"https://github.com/{meta['repository']}/tree/{meta['branch']}",
            "status": "候选版本；是否已发布及验收范围以对应 Release 为准"}


def pages(check=False):
    meta = metadata()
    data = page_data(meta)
    tokens = {"VERSION_NAME": meta["versionName"], "TAG": meta["tag"], "VERSION_CODE": str(meta["versionCode"]),
              "APPLICATION_ID": meta["applicationId"], "CERTIFICATE_SHA256": meta["certificateSha256"],
              "ABI_LIST": " / ".join(meta["abis"]), "SOURCE_URL": data["source_url"],
              "RELEASE_URL": data["release_url"], "RELEASE_STATUS": data["status"]}
    page = (ROOT / "docs/index.template.html").read_text()
    for key, value in tokens.items():
        page = page.replace("@@" + key + "@@", html.escape(value, quote=True))
    require(not re.search(r"@@[A-Z_]+@@", page), "Unresolved Pages template token")
    rendered_data = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    for name, content in (("index.html", page), ("release.json", rendered_data)):
        target = ROOT / "docs" / name
        if check:
            require(target.exists() and target.read_text() == content, "Regenerate Pages from release.properties: " + name)
        else:
            target.write_text(content)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    info = commands.add_parser("metadata")
    info.add_argument("--check-pages", action="store_true")
    commands.add_parser("pages")
    record = commands.add_parser("record")
    for name in ("apks", "sdk", "java", "output", "build-result"):
        record.add_argument("--" + name, type=Path, required=True)
    source = commands.add_parser("source")
    source.add_argument("--record", type=Path, required=True)
    source.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "metadata":
        result = metadata()
        if args.check_pages:
            pages(check=True)
    elif args.command == "pages":
        pages()
        result = page_data(metadata())
    elif args.command == "record":
        result = build_record(args)
    else:
        result = source_archive(args.record, args.output)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
