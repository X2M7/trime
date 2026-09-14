#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Run a single runtime probe with pinned APKs and full-duration log capture."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import time
import uuid

from runtime_audit import audit_log, device_audit_lock, filter_pid_log, instrumentation_passed, instrumentation_pid, log_after_marker, record_logcat


def api21_editor_bound(dump: str, pid: int) -> bool:
    """Window focus can precede both the editor connection and LatinIME startup."""
    header = "Input method service state for com.android.inputmethod.latin.LatinIME@"
    if dump.count(header) != 1:
        return False
    manager, service = dump.split(header)
    attributes = re.findall(
        r"(?m)^\s*inputType=(0x[0-9a-fA-F]+)\s+imeOptions=(0x[0-9a-fA-F]+)(?=\s|$)", service)
    if len(attributes) != 1:
        return False
    input_type, ime_options = (int(value, 16) for value in attributes[0])
    # API 21 adds IME_FLAG_NAVIGATE_NEXT when a laid-out editor has a next
    # focus target. Keep ACTION_SEND and reject every other action or flag.
    return all((
        "mCurMethodId=com.android.inputmethod.latin/.LatinIME" in manager,
        re.search(rf"mCurClient=ClientState\{{[^\n]*\bpid {pid}\}}", manager),
        "mInputShown=true" in manager,
        "mWindowVisible=true" in service,
        "mInputStarted=true mInputViewStarted=true" in service,
        "packageName=com.osfans.trime.debug " in service,
        input_type == 1 and ime_options in (0x4, 0x08000004),
    ))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--test-apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--probe", choices=("setup", "engine", "shutdown", "startupFailure", "feedback", "t02", "clip", "clipSave", "saf", "editors"), required=True)
    parser.add_argument("--saf-tree")
    parser.add_argument("--saf-revoke", action="store_true")
    parser.add_argument("--bind-ime-after-editor-focus", action="store_true",
                        help="API 21 editors only: select Trime after instrumentation creates its editor")
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
                "build_fingerprint": shell("getprop", "ro.build.fingerprint"), "packages": {},
                "source_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                "bind_ime_after_editor_focus": args.bind_ime_after_editor_focus}
    trime = package + "/com.osfans.trime.ime.core.TrimeInputMethodService"
    if args.bind_ime_after_editor_focus:
        if identity["api"] != "21" or args.probe != "editors":
            raise RuntimeError("Delayed IME binding is limited to the API 21 editor test")
        if shell("cat", f"/sdcard/Android/data/{package}/files/runtime-audit-dedicated") != "":
            raise RuntimeError("Requires an explicitly marked disposable installation")
        if shell("settings", "get", "secure", "default_input_method") != trime:
            raise RuntimeError("Select the tested Trime build before this probe")
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
    activation_marker = None
    with record_logcat(adb, args.output / "logcat-full.txt"), (args.output / "instrumentation.log").open("w") as log:
        command = adb + ["shell", "am", "instrument", "-w", "-r", *options,
                        package + ".test/com.osfans.trime.StartupResponsivenessInstrumentation"]
        if args.bind_ime_after_editor_focus:
            # API 21 can retain a stale service binding while instrumentation
            # replaces the IME process. This is setup, not a process-recovery test.
            process = None
            try:
                shell("input", "keyevent", "KEYCODE_HOME")
                shell("ime", "set", "com.android.inputmethod.latin/.LatinIME")
                process = subprocess.Popen(command, stdout=log, stderr=subprocess.STDOUT)
                start = time.monotonic()
                bound = False
                stable_binding = 0
                previous_observation = None
                observation_index = 0
                while process.poll() is None:
                    if time.monotonic() - start > 1000:
                        raise subprocess.TimeoutExpired(command, 1000)
                    output = (args.output / "instrumentation.log").read_text()
                    if not bound and "application_ready_wait_ms" in output:
                        windows = shell("dumpsys", "window", "windows", timeout=15)
                        focus = [line for line in windows.splitlines() if "mCurrentFocus=" in line]
                        focused = any("ClipEditActivity" in line for line in focus)
                        state = shell("dumpsys", "input_method") if focused else None
                        pid = instrumentation_pid(output)
                        ready = state is not None and api21_editor_bound(state, pid)
                        stable_binding = stable_binding + 1 if ready else 0
                        observation = (focus, state, pid)
                        if observation != previous_observation:
                            # Preserve the dumps already read by this gate. Only changed
                            # states need full snapshots; each poll records its outcome.
                            files = {}
                            for name, data in (("windows", windows), ("input_method", state)):
                                if data is not None:
                                    filename = f"api21-binding-{observation_index:04d}-{name}.txt"
                                    (args.output / filename).write_text(data)
                                    files[name] = {"file": filename, "sha256": hashlib.sha256(data.encode()).hexdigest()}
                            previous_observation = observation
                        with (args.output / "api21-binding-observations.jsonl").open("a") as observations:
                            observations.write(json.dumps({"index": observation_index,
                                                          "elapsed_seconds": time.monotonic() - start,
                                                          "pid": pid, "focused": focused,
                                                          "editor_ready": ready,
                                                          "stable_editor_observations": stable_binding,
                                                          "snapshots": files}) + "\n")
                        observation_index += 1
                        if focused:
                            if stable_binding >= 4:
                                (args.output / "api21-binding-before.txt").write_text(state)
                                # Audit Trime from BEFORE its activation, including service startup.
                                # LatinIME setup diagnostics remain in the full audit, not suppressed.
                                activation_marker = "TRIME-ACTIVATE-" + str(uuid.uuid4())
                                shell("log", "-t", "TrimeRuntimeAudit", activation_marker)
                                binding = {"elapsed_seconds": time.monotonic() - start,
                                           "stable_editor_observations": stable_binding,
                                           "activation_marker": activation_marker,
                                           "result": shell("ime", "set", trime)}
                                (args.output / "api21-test-binding.json").write_text(json.dumps(binding, indent=2) + "\n")
                                bound = True
                    time.sleep(.2)
                run = subprocess.CompletedProcess(command, process.wait())
                if not bound:
                    raise RuntimeError("Test editor did not meet the LatinIME binding gate; see api21-binding-observations.jsonl")
            finally:
                if process is not None and process.poll() is None:
                    process.kill()
                    process.wait(timeout=10)
                shell("ime", "set", trime)
        else:
            run = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=1000)
    output = (args.output / "instrumentation.log").read_text()
    glyph_evidence = None
    if args.probe == "editors":
        # Pull only the probe's own fixed artifacts, including on assertion
        # failure. Its nonce and content hashes reject stale private files.
        glyph_evidence = {"collected": False, "passed": False}
        try:
            identifiers = set(re.findall(
                r"INSTRUMENTATION_(?:STATUS|RESULT): fallback_glyph_run_id=([0-9a-f-]{36})",
                output))
            if len(identifiers) != 1:
                raise ValueError("Missing or ambiguous fallback glyph run identity")
            directory = args.output / "fallback-glyph"
            directory.mkdir()

            def pull_glyph(name):
                data = subprocess.check_output(
                    adb + ["exec-out", "run-as", package, "cat",
                           "files/runtime-audit/fallback-glyph/" + name], timeout=60)
                (directory / name).write_bytes(data)
                return data

            manifest = json.loads(pull_glyph("report.json"))
            if not isinstance(manifest, dict):
                raise ValueError("Invalid fallback glyph manifest")
            if manifest.get("run_id") != identifiers.pop():
                raise ValueError("Fallback glyph evidence belongs to a different probe run")
            expected = {f"{state}-font{scale}.{extension}"
                        for state in ("preparing", "failed") for scale in (1, 2)
                        for extension in ("png", "json")}
            attachments = manifest.get("attachments", {})
            if not isinstance(attachments, dict) or not set(attachments) <= expected:
                raise ValueError("Unexpected fallback glyph attachment names")
            for name in sorted(attachments):
                if hashlib.sha256(pull_glyph(name)).hexdigest() != attachments[name]:
                    raise ValueError("Fallback glyph attachment hash mismatch: " + name)
            if set(attachments) != expected:
                raise ValueError("Incomplete fallback glyph evidence")
            glyph_evidence.update(collected=True, passed=manifest.get("passed") is True,
                                  run_id=manifest["run_id"],
                                  report_sha256=hashlib.sha256((directory / "report.json").read_bytes()).hexdigest())
        except (ValueError, OSError, subprocess.SubprocessError) as error:
            glyph_evidence["error"] = str(error)
        (args.output / "fallback-glyph-collection.json").write_text(json.dumps(glyph_evidence, indent=2) + "\n")
    pid = instrumentation_pid(output)
    app_log = filter_pid_log((args.output / "logcat-full.txt").read_text(), pid)
    (args.output / "app-logcat.txt").write_text(app_log)
    audit = audit_log(app_log)
    (args.output / "runtime-audit.json").write_text(json.dumps(audit, indent=2) + "\n")
    tested_audit = audit
    if activation_marker:
        active_log = filter_pid_log(log_after_marker((args.output / "logcat-full.txt").read_text(), activation_marker), pid)
        (args.output / "tested-ime-logcat.txt").write_text(active_log)
        tested_audit = audit_log(active_log)
        (args.output / "tested-ime-audit.json").write_text(json.dumps(tested_audit, indent=2) + "\n")
    # Deliberate missing-theme/storage/provider faults still retain all warnings
    # for review. They never receive a warning-free label based on functional PASS.
    injects_faults = args.probe in ("engine", "startupFailure", "saf", "clipSave")
    assertions_passed = instrumentation_passed(run.returncode, output)
    passed = assertions_passed and tested_audit["capture_has_records"]
    if glyph_evidence is not None:
        passed = passed and glyph_evidence["collected"] and glyph_evidence["passed"]
    if not injects_faults:
        passed = passed and not tested_audit["known_project_diagnostics"]
    report = {"functional_passed": passed, "assertions_passed": assertions_passed,
              "diagnostic_scope": "before_trime_activation_through_completion" if activation_marker else "full_probe",
              "activation_marker": activation_marker, "warning_free": audit["warning_free"],
              "tested_ime_diagnostic_count": len(tested_audit["known_project_diagnostics"]),
              "full_probe_diagnostic_count": len(audit["known_project_diagnostics"]),
              "warning_count": len(audit["warnings"]), "injects_faults": injects_faults,
              "requires_log_review": bool(audit["warnings"] or audit["known_project_diagnostics"]), "pid": pid}
    if glyph_evidence is not None:
        report["fallback_glyph_evidence"] = glyph_evidence
    (args.output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report), flush=True)
    if not passed:
        raise RuntimeError("Probe failed; see retained instrumentation and runtime logs")


if __name__ == "__main__":
    main()
