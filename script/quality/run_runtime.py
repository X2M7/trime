#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Run a single runtime probe with pinned APKs and full-duration log capture."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

from runtime_audit import audit_log, device_audit_lock, filter_pid_log, instrumentation_passed, instrumentation_pid, record_logcat


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--test-apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--probe", choices=("setup", "engine", "shutdown", "startupFailure", "feedback", "t02", "clip", "clipSave", "saf"), required=True)
    parser.add_argument("--saf-tree")
    parser.add_argument("--saf-revoke", action="store_true")
    args = parser.parse_args()
    with device_audit_lock(args.serial):
        run(args)


def run(args):
    args.output.mkdir(parents=True, exist_ok=False)
    adb = [args.adb, "-s", args.serial]
    package = "com.osfans.trime.debug"

    def shell(*command, timeout=120):
        return subprocess.check_output(adb + ["shell", *command], text=True, timeout=timeout).strip()

    if shell("getprop", "ro.kernel.qemu") != "1":
        raise RuntimeError("Disposable emulator only")
    identity = {"serial": args.serial, "api": shell("getprop", "ro.build.version.sdk"),
                "device_abis": shell("getprop", "ro.product.cpu.abilist"), "probe": args.probe,
                "build_fingerprint": shell("getprop", "ro.build.fingerprint"), "packages": {}}
    for name, apk in ((package, args.apk), (package + ".test", args.test_apk)):
        installed = shell("pm", "path", name).removeprefix("package:")
        if not installed.startswith("/") or "\n" in installed:
            raise RuntimeError("Expected one APK per package")
        with apk.open("rb") as source:
            expected = hashlib.file_digest(source, "sha256").hexdigest()
        with tempfile.TemporaryFile() as source:
            subprocess.run(adb + ["exec-out", "cat", installed], stdout=source, check=True, timeout=120)
            source.seek(0)
            actual = hashlib.file_digest(source, "sha256").hexdigest()
        if actual != expected:
            raise RuntimeError(f"Installed APK mismatch: {name}")
        identity["packages"][name] = {"apk_sha256": actual, "installed_path": installed}
        (args.output / f"{name}.txt").write_text(shell("dumpsys", "package", name))
    (args.output / "identity.json").write_text(json.dumps(identity, indent=2) + "\n")
    options = [] if args.probe == "engine" else ["-e", args.probe, "true"]
    if args.saf_tree:
        options += ["-e", "safTree", args.saf_tree]
    if args.saf_revoke:
        options += ["-e", "safRevoke", "true"]
    with record_logcat(adb, args.output / "logcat-full.txt"), (args.output / "instrumentation.log").open("w") as log:
        run = subprocess.run(adb + ["shell", "am", "instrument", "-w", "-r", *options,
            package + ".test/com.osfans.trime.StartupResponsivenessInstrumentation"],
            stdout=log, stderr=subprocess.STDOUT, timeout=1000)
    output = (args.output / "instrumentation.log").read_text()
    pid = instrumentation_pid(output)
    app_log = filter_pid_log((args.output / "logcat-full.txt").read_text(), pid)
    (args.output / "app-logcat.txt").write_text(app_log)
    audit = audit_log(app_log)
    (args.output / "runtime-audit.json").write_text(json.dumps(audit, indent=2) + "\n")
    # Deliberate missing-theme/storage/provider faults still retain all warnings
    # for review. They never receive a warning-free label based on functional PASS.
    injects_faults = args.probe in ("engine", "startupFailure", "saf", "clipSave")
    passed = instrumentation_passed(run.returncode, output) and audit["capture_has_records"]
    if not injects_faults:
        passed = passed and not audit["known_project_diagnostics"]
    report = {"functional_passed": passed, "warning_free": audit["warning_free"],
              "warning_count": len(audit["warnings"]), "injects_faults": injects_faults,
              "requires_log_review": bool(audit["warnings"] or audit["known_project_diagnostics"]), "pid": pid}
    (args.output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report), flush=True)
    if not passed:
        raise RuntimeError("Probe failed; see retained instrumentation and runtime logs")


if __name__ == "__main__":
    main()
