#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Draft upload, hash verification and promotion for X2M7/trime only.

The authenticated transport and immutable three-asset transition are retained
from the reviewed t9.12 publisher; version data now comes from release.properties.
Git pushes are separate and checked by remote_source before any release is made.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

from release import ROOT, digest, metadata as release_metadata, require
from stage import load_manifest, validate_published_history

META = release_metadata()
TAG, REPOSITORY, BRANCH = META['tag'], META['repository'], META['branch']
API = f"https://api.github.com/repos/{REPOSITORY}"
UPLOAD = f"https://uploads.github.com/repos/{REPOSITORY}"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, file_pointer, code, message, headers, new_url):
        return None


def download_host(url):
    parsed = urllib.parse.urlsplit(url)
    require(parsed.scheme == "https" and parsed.hostname in
            {"github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"} and
            not parsed.username and not parsed.password and parsed.port in (None, 443),
            "Refused an unexpected release download origin")


def valid_token(token):
    return isinstance(token, str) and bool(token) and not any(character.isspace() for character in token)


def credential(hosts_path=None):
    environment = dict(os.environ, GIT_TERMINAL_PROMPT="0")
    try:
        result = subprocess.run(["git", "credential", "fill"], input="protocol=https\nhost=github.com\n\n",
                                text=True, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                env=environment, timeout=20, check=False)
        fields = dict(line.split("=", 1) for line in result.stdout.splitlines() if "=" in line)
        if result.returncode == 0 and valid_token(fields.get("password")):
            return fields["password"]
    except (OSError, subprocess.TimeoutExpired):
        pass
    for name in ("GH_TOKEN", "GITHUB_TOKEN"):
        if valid_token(os.environ.get(name)):
            return os.environ[name]
    hosts = hosts_path if hosts_path is not None else Path.home() / ".config/gh/hosts.yml"
    if hosts.exists():
        require(hosts.is_file() and not hosts.is_symlink() and hosts.stat().st_size <= 65536,
                "GitHub CLI login record is not a bounded regular file")
        try:
            import yaml
            value = yaml.safe_load(hosts.read_text(encoding="utf-8"))
        except Exception:
            # YAML parser errors can quote the secret-bearing source line.
            raise RuntimeError("Unable to safely read GitHub CLI login record") from None
        github = value.get("github.com", {}) if isinstance(value, dict) else {}
        users = github.get("users", {}) if isinstance(github, dict) else {}
        account = users.get("X2M7", {}) if isinstance(users, dict) else {}
        if isinstance(account, dict) and valid_token(account.get("oauth_token")):
            return account["oauth_token"]
        if isinstance(github, dict) and github.get("user") == "X2M7" and valid_token(github.get("oauth_token")):
            return github["oauth_token"]
    # The t9.10 publisher used this exact existing account/keyring identity.
    # Capture stdout only in RAM; do not include tool stderr in diagnostics.
    try:
        legacy = subprocess.run(["secret-tool", "lookup", "service", "gh:github.com", "username", "X2M7"],
                                text=True, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, timeout=20, check=False)
        token = legacy.stdout.strip()
        if legacy.returncode == 0 and valid_token(token):
            return token
    except (OSError, subprocess.TimeoutExpired):
        pass
    raise ValueError("No usable token in existing Git/environment/CLI/Secret Service credentials")


class GitHub:
    def __init__(self, token):
        self.headers = {"Accept": "application/vnd.github+json", "Authorization": "Bearer " + token,
                        "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "Trime-verified-release-release"}
        self.opener = urllib.request.build_opener(NoRedirect())

    def request(self, method, suffix, body=None, upload=False, media=None, length=None):
        url = (UPLOAD if upload else API) + suffix
        headers = dict(self.headers)
        data = body if isinstance(body, bytes) or hasattr(body, "read") else (json.dumps(body).encode() if body is not None else None)
        if data is not None:
            headers["Content-Type"] = media or "application/json"
        if length is not None:
            headers["Content-Length"] = str(length)
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=60) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            # Avoid response bodies, URLs, or token-bearing request objects.
            raise RuntimeError(f"GitHub API rejected request (HTTP {error.code})") from None

    def get(self, suffix):
        return self.request("GET", suffix)

    def releases(self):
        rows = []
        for page in range(1, 101):
            batch = self.get(f"/releases?per_page=100&page={page}")
            require(isinstance(batch, list), "Malformed authenticated release inventory")
            rows.extend(batch)
            if len(batch) < 100:
                return rows
        raise ValueError("Release inventory exceeded the bounded page count")

    def download(self, asset_id, path, expected):
        headers = dict(self.headers, Accept="application/octet-stream")
        request = urllib.request.Request(API + f"/releases/assets/{asset_id}", headers=headers)
        try:
            response = self.opener.open(request, timeout=60)
        except urllib.error.HTTPError as error:
            require(error.code in (301, 302, 303, 307, 308), "Draft asset download failed")
            location = error.headers.get("Location", "")
            download_host(location)
            # A separate opener/request drops Authorization for every redirect.
            request = urllib.request.Request(location, headers={"User-Agent": "Trime-verified-release-download"})
            response = self.opener.open(request, timeout=60)
        with response:
            require(response.status == 200, "Asset download returned a non-success status")
            checksum, length = hashlib.sha256(), 0
            with path.open("xb") as stream:
                while True:
                    block = response.read(1024 * 1024)
                    if not block:
                        break
                    length += len(block)
                    require(length <= expected["bytes"], "Asset download exceeded frozen size")
                    checksum.update(block)
                    stream.write(block)
        require(length == expected["bytes"] and checksum.hexdigest() == expected["sha256"],
                "Downloaded asset differs from frozen bytes")


def release_snapshot(row):
    return {key: row.get(key) for key in ("id", "tag_name", "name", "body", "draft", "prerelease", "target_commitish", "published_at")} | {
        "assets": sorted(({key: asset.get(key) for key in ("id", "name", "size", "digest", "content_type")} for asset in row.get("assets", [])), key=lambda item: item["id"])}


def remote_source(client, commit):
    branch = client.get(f"/git/ref/heads/{BRANCH}")
    require(branch.get("object", {}).get("type") == "commit" and branch["object"].get("sha") == commit,
            "Remote develop does not equal frozen publication commit")
    tag = client.get("/git/ref/tags/" + urllib.parse.quote(TAG, safe=""))
    require(tag.get("object", {}).get("type") == "tag", "Publication tag must be an annotated Git tag")
    peeled = client.get("/git/tags/" + tag["object"]["sha"])
    require(peeled.get("tag") == TAG and peeled.get("object", {}).get("type") == "commit" and
            peeled["object"].get("sha") == commit, "Annotated tag points to a different publication commit")
    return {"branch_sha": commit, "annotated_tag_sha": tag["object"]["sha"], "tag_commit": commit}


def metadata(row, commit, body):
    require(row.get("tag_name") == TAG and row.get("name") == f"Trime {TAG}" and row.get("body") == body and
            row.get("target_commitish") == commit and row.get("prerelease") is False,
            "Remote release metadata differs from frozen publication")
    require(type(row.get("id")) is int and row["id"] > 0 and type(row.get("draft")) is bool,
            "Invalid remote release identity")


def inventory(row, expected, exact=False):
    assets = row.get("assets")
    require(isinstance(assets, list), "Missing remote asset inventory")
    actual = {}
    for asset in assets:
        name = asset.get("name")
        require(name in expected and name not in actual, "Unexpected or duplicate remote asset")
        item = expected[name]
        media = "application/vnd.android.package-archive" if name.endswith(".apk") else "application/gzip"
        require(asset.get("size") == item["bytes"] and asset.get("digest") == "sha256:" + item["sha256"] and
                asset.get("state") == "uploaded" and asset.get("content_type") == media and
                type(asset.get("id")) is int and asset["id"] > 0, "Remote asset differs from frozen bytes or type")
        actual[name] = asset
    require(not exact or set(actual) == set(expected), "Remote release does not contain exactly three expected assets")
    return actual


def write_new(path, value):
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)
        stream.write("\n")


def poll(fetch, check):
    """Retry only idempotent reads while GitHub indexes an accepted mutation."""
    for attempt, delay in enumerate((0, 0.5, 1, 2, 4)):
        if delay:
            time.sleep(delay)
        value = fetch()
        try:
            check(value)
            return value
        except ValueError:
            if attempt == 4:
                raise


def publish(args, manifest, paths):
    records = args.records.resolve()
    assets_directory = next(iter(paths.values())).parent
    require(not records.is_relative_to(assets_directory), "Publication records must stay outside public-assets")
    records.mkdir(parents=True, exist_ok=False)
    client = GitHub(credential())
    # Account validation uses the exact GitHub API origin, without redirects.
    user_request = urllib.request.Request("https://api.github.com/user", headers=client.headers)
    with client.opener.open(user_request, timeout=60) as response:
        require(json.load(response).get("login") == "X2M7", "Credential belongs to a different GitHub account")
    commit = manifest["publication_git_sha"]
    source = remote_source(client, commit)
    before = client.releases()
    validate_published_history(before, manifest)
    write_new(records / "release-inventory-before.json", before)
    same = [row for row in before if row.get("tag_name") == TAG]
    body = Path(manifest["release_notes"]["path"])
    body = (args.manifest.resolve().parent / body).resolve() if not body.is_absolute() else body
    body = body.read_text(encoding="utf-8")
    if args.resume_release_id is None:
        require(not same, "Same-tag release already exists; resume only with its explicit numeric ID")
        release = client.request("POST", "/releases", {"tag_name": TAG, "target_commitish": commit,
                                 "name": f"Trime {TAG}", "body": body, "draft": True,
                                 "prerelease": False, "make_latest": "false", "generate_release_notes": False})
        require(release.get("draft") is True and release.get("assets") == [], "New release is not an empty draft")
    else:
        require(len(same) == 1 and same[0].get("id") == args.resume_release_id, "Resume ID does not match the unique same-tag release")
        release = client.get(f"/releases/{args.resume_release_id}")
        require(release.get("draft") is True, "This tool resumes drafts only; published releases are immutable here")
    metadata(release, commit, body)
    write_new(records / "release-created-or-resumed.json", release)
    expected = manifest["public_assets"]
    for name, asset in paths.items():
        current = inventory(release, expected)
        if name not in current:
            require(digest(asset) == expected[name]["sha256"], "Asset changed immediately before upload")
            media = "application/vnd.android.package-archive" if name.endswith(".apk") else "application/gzip"
            with asset.open("rb") as stream:
                uploaded = client.request("POST", f"/releases/{release['id']}/assets?" + urllib.parse.urlencode({"name": name}),
                                          stream, upload=True, media=media, length=expected[name]["bytes"])
            require(uploaded.get("name") == name, "Upload response names a different asset")
        def uploaded_visible(row):
            metadata(row, commit, body)
            require(row["draft"] is True, "Release left draft state during uploads")
            require(name in inventory(row, expected), "Uploaded asset is not yet visible")
        release = poll(lambda: client.get(f"/releases/{release['id']}"), uploaded_visible)
    draft = inventory(release, expected, exact=True)
    downloads = records / "draft-downloads"
    downloads.mkdir()
    for name, asset in draft.items():
        client.download(asset["id"], downloads / name, expected[name])
    # Revalidate local bytes, source maps, receipts and remote pointers immediately
    # before the one public-state transition.
    rechecked, _ = load_manifest(args.manifest)
    require(rechecked == manifest, "Manifest changed during publication")
    require(remote_source(client, commit) == source, "Remote source changed during publication")
    validate_published_history(client.releases(), manifest)
    release = client.get(f"/releases/{release['id']}")
    metadata(release, commit, body)
    require(release["draft"] is True, "Release is no longer a draft before publication")
    inventory(release, expected, exact=True)
    write_new(records / "draft-verified.json", release)
    client.request("PATCH", f"/releases/{release['id']}", {"draft": False, "prerelease": False, "make_latest": "true"})
    def published_visible(row):
        metadata(row, commit, body)
        require(row["draft"] is False, "Stable release publication is not yet visible")
        inventory(row, expected, exact=True)
    release = poll(lambda: client.get(f"/releases/{release['id']}"), published_visible)
    published = inventory(release, expected, exact=True)
    def latest_visible(row):
        require(row.get("id") == release["id"] and row.get("tag_name") == TAG and
                row.get("draft") is False and row.get("prerelease") is False, "Latest does not yet identify the stable release")
    latest = poll(lambda: client.get("/releases/latest"), latest_visible)
    downloads = records / "published-downloads"
    downloads.mkdir()
    for name, asset in published.items():
        client.download(asset["id"], downloads / name, expected[name])
    after = client.releases()
    prior = {row["id"]: release_snapshot(row) for row in before if row.get("tag_name") != TAG}
    retained = {row["id"]: release_snapshot(row) for row in after if row.get("tag_name") != TAG}
    require(prior == retained, "Existing releases or assets changed during publication")
    require(remote_source(client, commit) == source, "Remote branch/tag changed after publication")
    same = [row for row in after if row.get("tag_name") == TAG]
    require(len(same) == 1 and same[0].get("id") == release["id"], "Final inventory does not identify exactly one release")
    release = client.get(f"/releases/{release['id']}")
    metadata(release, commit, body)
    require(release["draft"] is False, "Release no longer has published state")
    inventory(release, expected, exact=True)
    latest = client.get("/releases/latest")
    require(latest.get("id") == release["id"] and latest.get("tag_name") == TAG and
            latest.get("draft") is False and latest.get("prerelease") is False, "Final Latest pointer changed after downloads")
    write_new(records / "release-published.json", release)
    write_new(records / "latest-after.json", latest)
    result = {"passed": True, "tag": TAG, "repository": REPOSITORY, "release_id": release["id"],
              "release_url": release.get("html_url"), "manifest_sha256": digest(args.manifest),
              "publication_git_sha": commit, "build_git_sha": manifest["build_git_sha"],
              "stable": True, "latest": True, "assets": sorted(expected), "uploaded_asset_count": 3,
              "draft_downloads_verified": True, "published_downloads_verified": True,
              "existing_releases_unchanged": True, "credentials_persisted": False}
    write_new(records / "publication-receipt.json", result)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('plan', 'publish'))
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--records', type=Path, required=True)
    args = parser.parse_args()
    args.resume_release_id = None  # Never alter or overwrite an existing tag/release.
    manifest, paths = load_manifest(args.manifest)
    if args.command == 'plan':
        print(json.dumps(dict(tag=TAG, repository=REPOSITORY, assets=list(paths),
                             latest=True, credentials_loaded=False), indent=2))
    else:
        print(json.dumps(publish(args, manifest, paths), indent=2))


if __name__ == '__main__':
    main()
