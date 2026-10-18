#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Deploy this fork's static site with the runner's short-lived credentials.

Protocol: https://docs.github.com/en/rest/pages/pages
Artifact provenance: https://docs.github.com/en/rest/actions/artifacts
OIDC: https://docs.github.com/en/actions/reference/security/oidc

This deliberately has no Node dependencies, alternate repository, PAT fallback,
or configurable deployment destination. Run it on the Ubuntu Pages runner.
"""
from dataclasses import dataclass, field
import json
import os
from pathlib import Path
import re
import signal
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener


REPOSITORY = "X2M7/trime"
API = "https://api.github.com/repos/" + REPOSITORY
PAGE_URL = "https://x2m7.github.io/trime/"
ENVIRONMENT = "github-pages"
TOTAL_SECONDS = 480
REQUEST_SECONDS = 20
CANCEL_SECONDS = 10
MAX_RESPONSE_BYTES = 1024 * 1024
PENDING = frozenset({
    "deployment_in_progress", "syncing_files", "finished_file_sync",
    "updating_pages", "purging_cdn", "deployment_attempt_error", "unknown_status", "not_found",
})
FAILED = frozenset({
    "deployment_cancelled", "deployment_failed", "deployment_content_failed",
    "deployment_lost",
})


class DeploymentError(Exception):
    """A diagnostic that contains no server body, credential, or OIDC URL."""


def positive_integer(value, name):
    if not isinstance(value, str) or not re.fullmatch(r"[1-9][0-9]{0,19}", value):
        raise DeploymentError("Invalid " + name + ".")
    return int(value)


def credential(value, name):
    if not isinstance(value, str) or not value or len(value) > 32768 or any(
        ord(character) <= 32 or ord(character) == 127 for character in value
    ):
        raise DeploymentError("Missing or invalid " + name + ".")
    return value


@dataclass(frozen=True)
class Context:
    sha: str
    run_id: int
    run_attempt: int
    artifact_id: int
    token: str = field(repr=False)
    oidc_url: str = field(repr=False)
    oidc_request_token: str = field(repr=False)

    @classmethod
    def from_environment(cls, env):
        if (env.get("GITHUB_ACTIONS") != "true" or
                env.get("GITHUB_REPOSITORY") != REPOSITORY or
                env.get("GITHUB_REF") != "refs/heads/develop"):
            raise DeploymentError("Pages deployment requires this fork's develop Actions run.")
        sha = env.get("GITHUB_SHA", "")
        if not re.fullmatch(r"[0-9a-f]{40}", sha):
            raise DeploymentError("Invalid GITHUB_SHA.")
        oidc_url = env.get("ACTIONS_ID_TOKEN_REQUEST_URL", "")
        try:
            parts = urlsplit(oidc_url)
            valid_url = (parts.scheme == "https" and parts.hostname is not None and
                         parts.hostname.endswith(".actions.githubusercontent.com") and
                         parts.username is None and parts.password is None and
                         parts.port is None and not parts.fragment and
                         parts.path.startswith("/") and bool(parts.query) and
                         all(ord(character) > 32 and ord(character) != 127
                             for character in oidc_url))
        except ValueError:
            valid_url = False
        if not valid_url:
            raise DeploymentError("Invalid Actions OIDC endpoint.")
        return cls(
            sha=sha,
            run_id=positive_integer(env.get("GITHUB_RUN_ID"), "GITHUB_RUN_ID"),
            run_attempt=positive_integer(env.get("GITHUB_RUN_ATTEMPT"), "GITHUB_RUN_ATTEMPT"),
            artifact_id=positive_integer(env.get("PAGES_ARTIFACT_ID"), "PAGES_ARTIFACT_ID"),
            token=credential(env.get("GITHUB_TOKEN"), "GITHUB_TOKEN"),
            oidc_url=oidc_url,
            oidc_request_token=credential(env.get("ACTIONS_ID_TOKEN_REQUEST_TOKEN"),
                                          "ACTIONS_ID_TOKEN_REQUEST_TOKEN"),
        )


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        # Neither the GitHub token nor the runner's OIDC credential may follow
        # redirects, including redirects to another GitHub-owned service.
        return None


def request_json(method, url, token, payload, timeout):
    """Bound the complete request, including DNS and slow response bodies."""
    if not hasattr(signal, "setitimer"):
        raise DeploymentError("The Pages HTTP client requires a POSIX runner.")
    headers = {
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "trime-fork-pages-deployer",
    }
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    if data is not None:
        headers["Content-Type"] = "application/json"

    def expired(signum, frame):
        raise DeploymentError("HTTP request timed out.")

    previous_handler = signal.signal(signal.SIGALRM, expired)
    previous_timer = signal.setitimer(signal.ITIMER_REAL, timeout)
    try:
        request = Request(url, data=data, headers=headers, method=method)
        with build_opener(NoRedirects()).open(request, timeout=timeout) as response:
            if response.status == 204:
                return {}
            raw = response.read(MAX_RESPONSE_BYTES + 1)
            if len(raw) > MAX_RESPONSE_BYTES:
                raise DeploymentError("HTTP response exceeded the size limit.")
            value = json.loads(raw)
            if not isinstance(value, dict):
                raise DeploymentError("Expected a JSON object from GitHub.")
            return value
    except HTTPError as error:
        # HTTPError contains a response body and the credential-bearing OIDC
        # request URL. Report only the numeric status, never the raw exception.
        status = error.code
        error.close()
        raise DeploymentError("HTTP request failed with status " + str(status) + ".") from None
    except (URLError, OSError, ValueError):
        raise DeploymentError("HTTP transport or JSON response failed.") from None
    finally:
        signal.setitimer(signal.ITIMER_REAL, 0)
        signal.signal(signal.SIGALRM, previous_handler)
        if previous_timer != (0.0, 0.0):
            signal.setitimer(signal.ITIMER_REAL, *previous_timer)


def matches_integer(value, expected):
    return type(value) is int and value == expected


def mapping(value):
    if not isinstance(value, dict):
        raise DeploymentError("GitHub returned malformed provenance metadata.")
    return value


class Deployer:
    def __init__(self, context, transport=request_json, clock=time.monotonic, sleep=time.sleep):
        self.context = context
        self.transport = transport
        self.clock = clock
        self.sleep = sleep
        self.deadline = clock() + TOTAL_SECONDS

    def request(self, method, path, payload=None):
        remaining = self.deadline - self.clock()
        if remaining <= 0:
            raise DeploymentError("Pages deployment deadline exceeded.")
        return self.transport(method, API + path, self.context.token, payload,
                              min(REQUEST_SECONDS, remaining))

    def verify_artifact(self):
        context = self.context
        run = self.request("GET", "/actions/runs/" + str(context.run_id))
        repository = mapping(run.get("repository"))
        head_repository = mapping(run.get("head_repository"))
        repository_id = repository.get("id")
        if (not matches_integer(run.get("id"), context.run_id) or
                not matches_integer(run.get("run_attempt"), context.run_attempt) or
                run.get("head_sha") != context.sha or run.get("head_branch") != "develop" or
                run.get("event") not in {"push", "workflow_dispatch"} or
                repository.get("full_name") != REPOSITORY or
                head_repository.get("full_name") != REPOSITORY or
                type(repository_id) is not int or repository_id <= 0 or
                not matches_integer(head_repository.get("id"), repository_id)):
            raise DeploymentError("Workflow provenance does not match this develop run.")
        artifact = self.request("GET", "/actions/artifacts/" + str(context.artifact_id))
        workflow = mapping(artifact.get("workflow_run"))
        size = artifact.get("size_in_bytes")
        if (not matches_integer(artifact.get("id"), context.artifact_id) or
                artifact.get("name") != "github-pages" or artifact.get("expired") is not False or
                type(size) is not int or not 0 < size <= 1024 * 1024 * 1024 or
                not matches_integer(workflow.get("id"), context.run_id) or
                not matches_integer(workflow.get("repository_id"), repository_id) or
                not matches_integer(workflow.get("head_repository_id"), repository_id) or
                workflow.get("head_sha") != context.sha or workflow.get("head_branch") != "develop"):
            raise DeploymentError("Pages artifact does not belong to this develop run.")

    def deploy(self):
        self.verify_artifact()
        context = self.context
        remaining = self.deadline - self.clock()
        if remaining <= 0:
            raise DeploymentError("Pages deployment deadline exceeded.")
        oidc = self.transport("GET", context.oidc_url, context.oidc_request_token, None,
                              min(REQUEST_SECONDS, remaining)).get("value")
        if not isinstance(oidc, str) or len(oidc) > 32768 or not re.fullmatch(
                r"[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+", oidc):
            raise DeploymentError("GitHub did not return a valid OIDC token.")
        deployment = self.request("POST", "/pages/deployments", {
            "artifact_id": context.artifact_id,
            "pages_build_version": context.sha,
            "oidc_token": oidc,
            "environment": ENVIRONMENT,
        })
        id_pattern = r"(?:[0-9a-f]{40}|[1-9][0-9]{0,19})"
        explicit_id = deployment.get("id")
        if type(explicit_id) is int and explicit_id > 0:
            explicit_id = str(explicit_id)
        if not isinstance(explicit_id, str) or not re.fullmatch(id_pattern, explicit_id):
            explicit_id = None
        status_url = deployment.get("status_url")
        status_match = None
        if isinstance(status_url, str):
            status_match = re.fullmatch(re.escape(API + "/pages/deployments/") +
                                       "(" + id_pattern + r")(?:/status)?", status_url)
        url_id = status_match.group(1) if status_match else None
        deployment_id = explicit_id or url_id
        if deployment_id is None:
            raise DeploymentError("GitHub returned an invalid Pages deployment ID.")
        path = "/pages/deployments/" + deployment_id
        try:
            # Do not follow server-supplied URLs with an Authorization header.
            if url_id != deployment_id or ("id" in deployment and explicit_id != url_id):
                raise DeploymentError("GitHub returned an unexpected Pages status endpoint.")
            if deployment.get("page_url") not in {
                    PAGE_URL, PAGE_URL.rstrip("/"), PAGE_URL.removeprefix("https://"),
                    PAGE_URL.removeprefix("https://").rstrip("/")}:
                raise DeploymentError("GitHub returned an unexpected Pages site URL.")
            while True:
                status = self.request("GET", path).get("status")
                if self.clock() >= self.deadline:
                    raise DeploymentError("Pages deployment deadline exceeded.")
                if status == "succeed":
                    return PAGE_URL
                if not isinstance(status, str):
                    raise DeploymentError("Pages deployment reported an unknown status.")
                if status in FAILED:
                    raise DeploymentError("Pages deployment reported a terminal failure.")
                if status not in PENDING:
                    raise DeploymentError("Pages deployment reported an unknown status.")
                self.sleep(max(0, min(5, self.deadline - self.clock())))
        except DeploymentError:
            try:
                self.transport("POST", API + path + "/cancel", context.token, None, CANCEL_SECONDS)
            except DeploymentError:
                print("Pages cancellation could not be confirmed.", file=sys.stderr)
            raise


def main(env=None):
    env = os.environ if env is None else env
    previous_handlers = {}

    def interrupted(signum, frame):
        raise DeploymentError("Pages deployment interrupted.")

    try:
        for signum in (signal.SIGINT, signal.SIGTERM):
            previous_handlers[signum] = signal.signal(signum, interrupted)
        page_url = Deployer(Context.from_environment(env)).deploy()
        output = env.get("GITHUB_OUTPUT")
        if output:
            with Path(output).open("a", encoding="utf-8", newline="\n") as stream:
                stream.write("page_url=" + page_url + "\n")
        print("Pages deployment succeeded: " + page_url)
        return 0
    except DeploymentError as error:
        print("Pages deployment failed: " + str(error), file=sys.stderr)
    except Exception:
        # Never expose server responses or a credential-bearing request object.
        print("Pages deployment failed unexpectedly.", file=sys.stderr)
    finally:
        for signum, handler in previous_handlers.items():
            signal.signal(signum, handler)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
