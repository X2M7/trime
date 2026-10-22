#!/usr/bin/env python3
"""Create one terminal receipt for the four exact-head 4-OS cloud runs."""

import hashlib
import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "terminal-runs"
GH = Path("/home/xumin/Public/Trime/build/upstream19/gh-cli-2.102.0/gh")
PLATFORMS = {
    "build (ubuntu-26.04)",
    "build (macos-26)",
    "build (macos-26-intel)",
    "build (windows-2025)",
}
RUNS = {
    "fork-startup": {
        "repository": "X2M7/trime",
        "run": 39723620625,
        "head": "2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3",
        "event": "push",
        "expected_cache": "miss_then_saved",
    },
    "fork-t9": {
        "repository": "X2M7/trime",
        "run": 39723620640,
        "head": "9f86bc8f1d11d65d2e22e437f111a965bc2faba5",
        "event": "push",
        "expected_cache": "hit",
    },
    "upstream-2188-startup": {
        "repository": "osfans/trime",
        "run": 39723839224,
        "head": "2d2fbce70b449ec8b6560c844ca5ddbc2e08a4c3",
        "event": "pull_request",
        "expected_cache": "hit",
    },
    "upstream-2183-t9": {
        "repository": "osfans/trime",
        "run": 39723624948,
        "head": "9f86bc8f1d11d65d2e22e437f111a965bc2faba5",
        "event": "pull_request",
        "expected_cache": "hit",
    },
}


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def api(route, path):
    result = subprocess.run(
        [str(GH), "api", route], capture_output=True, timeout=60, check=False
    )
    path.write_bytes(result.stdout)
    path.with_suffix(path.suffix + ".stderr").write_bytes(result.stderr)
    if result.returncode:
        raise RuntimeError(f"GitHub API failed: {route}: {result.returncode}")
    return json.loads(result.stdout)


def step(job, name):
    found = [item for item in job["steps"] if item["name"] == name]
    assert len(found) == 1, (job["name"], name, found)
    return found[0]


OUT.mkdir(exist_ok=False)
receipt = {
    "schema": "trime-deferred-input-view-four-platform-runs/v1",
    "runs": {},
}
for label, expected in RUNS.items():
    folder = OUT / label
    folder.mkdir()
    prefix = f"repos/{expected['repository']}/actions/runs/{expected['run']}"
    run_path = folder / "run.json"
    jobs_path = folder / "jobs.json"
    artifacts_path = folder / "artifacts.json"
    run = api(prefix, run_path)
    jobs = api(prefix + "/jobs", jobs_path)
    artifacts = api(prefix + "/artifacts", artifacts_path)
    assert run["id"] == expected["run"]
    assert run["head_sha"] == expected["head"]
    assert run["event"] == expected["event"]
    assert run["status"] == "completed"
    assert run["conclusion"] == "success"
    assert len(jobs["jobs"]) == 4
    assert {job["name"] for job in jobs["jobs"]} == PLATFORMS
    job_receipts = []
    for job in jobs["jobs"]:
        assert job["head_sha"] == expected["head"]
        assert job["status"] == "completed"
        assert job["conclusion"] == "success"
        style = step(job, "Check code style")
        if job["name"] == "build (ubuntu-26.04)":
            assert style["conclusion"] == "success"
        else:
            assert style["conclusion"] == "skipped"
        assert step(job, "Calculate JNI cache hash")["conclusion"] == "success"
        assert step(job, "Fetch JNI cache")["conclusion"] == "success"
        fetch_submodules = step(job, "Fetch submodules")["conclusion"]
        add_cache = step(job, "Add JNI cache")["conclusion"]
        if expected["expected_cache"] == "hit":
            assert fetch_submodules == "skipped"
            assert add_cache == "skipped"
            cache = "hit"
        else:
            assert fetch_submodules == "success"
            assert add_cache == "success"
            cache = "miss_then_saved"
        assert step(job, "Build debug Trime")["conclusion"] == "success"
        assert step(job, "Upload APK artifact")["conclusion"] == "success"
        job_receipts.append(
            {
                "id": job["id"],
                "name": job["name"],
                "status": job["status"],
                "conclusion": job["conclusion"],
                "started_at": job["started_at"],
                "completed_at": job["completed_at"],
                "style": style["conclusion"],
                "build_debug": "success",
                "jni_cache": cache,
            }
        )
    receipt["runs"][label] = {
        "repository": expected["repository"],
        "run": expected["run"],
        "head": expected["head"],
        "event": expected["event"],
        "status": run["status"],
        "conclusion": run["conclusion"],
        "url": run["html_url"],
        "created_at": run["created_at"],
        "updated_at": run["updated_at"],
        "jobs": job_receipts,
        "artifact_count": len(artifacts["artifacts"]),
        "raw_bindings": [
            {"path": str(path), "sha256": sha256(path)}
            for path in (run_path, jobs_path, artifacts_path)
        ],
    }

receipt["all_16_platform_jobs_success"] = True
receipt["service_diagnostics_report"] = {
    "path": str(ROOT / "service-cloud-diagnostics.json"),
    "sha256": sha256(ROOT / "service-cloud-diagnostics.json"),
}
receipt["source_combination_report"] = {
    "path": str(ROOT / "startup-t9-combination-equivalence.json"),
    "sha256": sha256(ROOT / "startup-t9-combination-equivalence.json"),
}
receipt["split_junit_control_report"] = {
    "path": str(ROOT / "control-independent-audit.json"),
    "sha256": sha256(ROOT / "control-independent-audit.json"),
}
receipt["cloud_apk_audits"] = {
    "startup": {
        "path": str(
            ROOT
            / "artifact-startup-ubuntu-12402467624"
            / "apk-audit"
            / "result.json"
        ),
        "sha256": sha256(
            ROOT
            / "artifact-startup-ubuntu-12402467624"
            / "apk-audit"
            / "result.json"
        ),
    },
    "t9": {
        "path": str(
            ROOT
            / "artifact-t9-ubuntu-12402430914"
            / "apk-audit"
            / "result.json"
        ),
        "sha256": sha256(
            ROOT
            / "artifact-t9-ubuntu-12402430914"
            / "apk-audit"
            / "result.json"
        ),
    },
}
receipt["startup_native_build_evidence"] = {
    "path": str(ROOT / "startup-native-build-evidence.json"),
    "sha256": sha256(ROOT / "startup-native-build-evidence.json"),
}
receipt["retained_non_artifact_audit_failures"] = [
    {
        "scope": "startup certificate expectation inherited from a separate debug job",
        "path": str(
            ROOT
            / "artifact-startup-ubuntu-12402467624"
            / "apk-audit-attempt1"
            / "attempt-result.json"
        ),
        "sha256": sha256(
            ROOT
            / "artifact-startup-ubuntu-12402467624"
            / "apk-audit-attempt1"
            / "attempt-result.json"
        ),
    },
    {
        "scope": "T9 submodule-status leading-space parser assumption",
        "path": str(
            ROOT
            / "artifact-t9-ubuntu-12402430914"
            / "apk-audit-attempt1"
            / "attempt-result.json"
        ),
        "sha256": sha256(
            ROOT
            / "artifact-t9-ubuntu-12402430914"
            / "apk-audit-attempt1"
            / "attempt-result.json"
        ),
    },
]
receipt["limits"] = [
    "Fork push runs compile exact heads; upstream pull_request runs compile their recorded synthetic merge refs while binding the requested head SHA.",
    "Cache hit/miss is derived from the workflow's conditional Fetch submodules/Add JNI cache steps and is additionally bound to Ubuntu raw logs.",
    "A cache miss is not, by itself, represented as proof that every native object was rebuilt.",
    "Cloud build success does not replace Android device qualification.",
]
receipt["passed"] = True
path = ROOT / "four-platform-terminal-receipt.json"
path.write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps({"passed": True, "jobs": 16, "report": str(path), "sha256": sha256(path)}))
