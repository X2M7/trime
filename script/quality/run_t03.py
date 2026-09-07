#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Run T03 on an already prepared API 29+ emulator, restoring display overrides."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--case", choices=["360", "412", "tablet", "landscape", "large", "landscape-360", "landscape-large"])
    parser.add_argument("--geometry-only", action="store_true", help="Skip repeated gesture semantics; retain both themes, hand modes and height checks")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    base = [args.adb, "-s", args.serial]

    def adb(*argv, timeout=60):
        return subprocess.check_output(base + list(argv), timeout=timeout).decode().strip()

    def shell(*argv, timeout=60):
        return adb("shell", *argv, timeout=timeout)

    def screenshots():
        return [name for name in shell("run-as", package, "ls", "cache").splitlines()
                if re.fullmatch(r"t03-[A-Za-z0-9_.-]+\.png", name)]

    if shell("getprop", "ro.hardware") not in {"ranchu", "goldfish"}:
        raise RuntimeError("Refusing to change display settings on a physical device")
    if int(shell("getprop", "ro.build.version.sdk")) < 29:
        raise RuntimeError("Probe requires API 29+")
    package = "com.osfans.trime.debug"
    runner = package + ".test/com.osfans.trime.StartupResponsivenessInstrumentation"
    original = {name: shell("wm", name) for name in ("size", "density")}
    font = shell("settings", "get", "system", "font_scale")
    (args.output / "original-display.json").write_text(json.dumps({**original, "font_scale": font}, indent=2) + "\n")
    (args.output / "installed-package.txt").write_text(shell("dumpsys", "package", package) + "\n")
    cases = {
        "360": ("720x1600", "1.0"),
        "412": ("824x1800", "1.0"),
        "tablet": ("1280x1600", "1.0"),
        "landscape": ("1600x824", "1.0"),
        "large": ("824x1800", "2.0"),
        "landscape-360": ("1600x720", "1.0"),
        "landscape-large": ("1600x824", "2.0"),
    }
    report = {}
    try:
        for name, (size, scale) in cases.items():
            if args.case and args.case != name:
                continue
            target = args.output / name
            target.mkdir(exist_ok=True)
            # Only probe-owned screenshots are removed; dictionaries and other app data are untouched.
            for filename in screenshots():
                shell("run-as", package, "rm", "cache/" + filename)
            shell("wm", "size", size)
            shell("wm", "density", "320")
            shell("settings", "put", "system", "font_scale", scale)
            print(f"Running {name}: {size}, density 320, font {scale}", flush=True)
            with (target / "instrumentation.log").open("w") as log:
                options = ["-e", "t03GeometryOnly", "true"] if args.geometry_only else []
                completed = subprocess.run(
                    base + ["shell", "am", "instrument", "-w", "-r", "-e", "t03", "true"] + options + [runner],
                    stdout=log, stderr=subprocess.STDOUT, timeout=1000, check=False,
                )
            output = (target / "instrumentation.log").read_text()
            passed = completed.returncode == 0 and "INSTRUMENTATION_RESULT: passed=true" in output and "INSTRUMENTATION_CODE: -1" in output
            report[name] = {"passed": passed, "size": size, "density": 320, "font_scale": scale, "geometry_only": args.geometry_only, "screenshots": {}}
            for filename in screenshots():
                with (target / filename).open("wb") as png:
                    subprocess.run(base + ["exec-out", "run-as", package, "cat", "cache/" + filename], stdout=png, check=True, timeout=60)
                report[name]["screenshots"][filename] = hashlib.sha256((target / filename).read_bytes()).hexdigest()
            preferences = ET.fromstring(shell("run-as", package, "cat", "shared_prefs/" + package + "_preferences.xml"))
            process = preferences.find("./int[@name='general__pid']")
            if process is None:
                raise RuntimeError("Missing tested application PID")
            pid = int(process.attrib["value"])
            app_log = shell("logcat", "-d", "-v", "threadtime", f"--pid={pid}", timeout=120)
            (target / "app-logcat.txt").write_text(app_log + "\n")
            diagnostics = [line for line in app_log.splitlines()
                           if "requestLayout() improperly" in line or ("ThemeUtils" in line and "AppCompat" in line)]
            passed = passed and not diagnostics
            report[name].update({"passed": passed, "app_pid": pid, "layout_diagnostics": diagnostics})
            (target / "logcat.txt").write_text(shell("logcat", "-d", "-v", "threadtime", timeout=120) + "\n")
            (args.output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
            print(f"{name}: {'PASS' if passed else 'FAIL'}", flush=True)
            if not passed:
                raise RuntimeError(f"Probe failed; see {target / 'instrumentation.log'}")
    finally:
        for name, output in original.items():
            match = re.search(r"Override (?:size|density): (\S+)", output)
            shell("wm", name, match.group(1) if match else "reset")
        if font == "null":
            shell("settings", "delete", "system", "font_scale")
        else:
            shell("settings", "put", "system", "font_scale", font)


if __name__ == "__main__":
    main()
