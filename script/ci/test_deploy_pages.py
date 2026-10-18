#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Deployment provenance, failure, and credential boundaries; no network calls."""
from copy import deepcopy
from contextlib import redirect_stderr, redirect_stdout
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

import deploy_pages as pages


SHA = "a" * 40
ENV = {
    "GITHUB_ACTIONS": "true", "GITHUB_REPOSITORY": "X2M7/trime",
    "GITHUB_REF": "refs/heads/develop", "GITHUB_SHA": SHA,
    "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2", "PAGES_ARTIFACT_ID": "456",
    "GITHUB_TOKEN": "github-secret", "ACTIONS_ID_TOKEN_REQUEST_TOKEN": "runner-secret",
    "ACTIONS_ID_TOKEN_REQUEST_URL": "https://pipelines.actions.githubusercontent.com/job/idtoken?secret=query",
}
RUN = {
    "id": 123, "run_attempt": 2, "head_sha": SHA, "head_branch": "develop", "event": "push",
    "repository": {"id": 100, "full_name": "X2M7/trime"},
    "head_repository": {"id": 100, "full_name": "X2M7/trime"},
}
ARTIFACT = {
    "id": 456, "name": "github-pages", "expired": False, "size_in_bytes": 1000,
    "workflow_run": {"id": 123, "repository_id": 100, "head_repository_id": 100,
                     "head_sha": SHA, "head_branch": "develop"},
}
CREATED = {"id": SHA, "page_url": pages.PAGE_URL,
           "status_url": pages.API + "/pages/deployments/" + SHA}


class FakeService:
    def __init__(self, statuses=("deployment_in_progress", "succeed")):
        self.run = deepcopy(RUN)
        self.artifact = deepcopy(ARTIFACT)
        self.created = deepcopy(CREATED)
        self.statuses = iter(statuses)
        self.calls = []
        self.now = 0
        self.cancel_error = False

    def sleep(self, seconds):
        self.now += seconds

    def request(self, method, url, token, payload, timeout):
        self.calls.append((method, url, token, payload, timeout))
        if url == pages.API + "/actions/runs/123":
            return self.run
        if url == pages.API + "/actions/artifacts/456":
            return self.artifact
        if url == ENV["ACTIONS_ID_TOKEN_REQUEST_URL"]:
            return {"value": "oidc.secret.signature"}
        if method == "POST" and url == pages.API + "/pages/deployments":
            return self.created
        if url.endswith("/cancel"):
            if self.cancel_error:
                raise pages.DeploymentError("HTTP request timed out.")
            return {}
        if method == "GET" and url == CREATED["status_url"]:
            return {"status": next(self.statuses)}
        raise AssertionError("Unexpected fake request")

    def deployer(self):
        return pages.Deployer(pages.Context.from_environment(ENV), self.request,
                              lambda: self.now, self.sleep)


class DeploymentTests(unittest.TestCase):
    def test_success_binds_run_and_artifact_before_deploying_and_uses_separate_credentials(self):
        service = FakeService()
        self.assertEqual(service.deployer().deploy(), pages.PAGE_URL)
        self.assertEqual([call[1] for call in service.calls[:3]], [
            pages.API + "/actions/runs/123", pages.API + "/actions/artifacts/456",
            ENV["ACTIONS_ID_TOKEN_REQUEST_URL"],
        ])
        self.assertEqual(service.calls[2][2], "runner-secret")
        self.assertTrue(all(call[2] == "github-secret" for call in service.calls
                            if call[1].startswith(pages.API)))
        payload = service.calls[3][3]
        self.assertEqual(payload, {"artifact_id": 456, "pages_build_version": SHA,
                                  "oidc_token": "oidc.secret.signature", "environment": "github-pages"})
        self.assertFalse(any(call[1].endswith("/cancel") for call in service.calls))

    def test_official_status_url_only_fixture_and_transient_startup_states_succeed(self):
        # actions/deploy-pages' API fixture omits id; status_url carries the ID.
        for suffix in ("", "/status"):
            service = FakeService(["unknown_status", "not_found", "succeed"])
            service.created = {"status_url": CREATED["status_url"] + suffix,
                               "page_url": pages.PAGE_URL}
            with self.subTest(suffix=suffix):
                self.assertEqual(service.deployer().deploy(), pages.PAGE_URL)
            self.assertEqual(service.now, 10)
            self.assertFalse(any(call[1].endswith("/cancel") for call in service.calls))

    def test_inconsistent_or_missing_deployment_identity_is_rejected(self):
        for created in ({"page_url": pages.PAGE_URL},
                        {"status_url": "https://evil.example/deployments/" + SHA,
                         "page_url": pages.PAGE_URL},
                        {**CREATED, "id": "b" * 40}, {**CREATED, "id": True}):
            service = FakeService(["succeed"])
            service.created = created
            with self.subTest(created=created), self.assertRaises(pages.DeploymentError):
                service.deployer().deploy()
            self.assertFalse(any(call[0] == "GET" and "/pages/deployments/" in call[1]
                                 for call in service.calls))

    def test_foreign_or_malformed_environment_is_rejected_without_requests(self):
        for key, values in {
            "GITHUB_ACTIONS": ["false"], "GITHUB_REPOSITORY": ["osfans/trime"],
            "GITHUB_REF": ["refs/pull/1/merge", "refs/heads/main"],
            "GITHUB_SHA": ["a" * 39, "../secret"],
            "GITHUB_RUN_ID": ["0", "123\n"], "GITHUB_RUN_ATTEMPT": ["-1", "2.0"],
            "PAGES_ARTIFACT_ID": ["456/zip", ""], "GITHUB_TOKEN": ["", "secret\r\nheader"],
            "ACTIONS_ID_TOKEN_REQUEST_URL": [
                "https://evil.example/idtoken?secret=query",
                "https://pipelines.actions.githubusercontent.com.evil.example/idtoken?secret=query",
                "http://pipelines.actions.githubusercontent.com/idtoken?secret=query",
                "https://user@pipelines.actions.githubusercontent.com/idtoken?secret=query",
                "https://pipelines.actions.githubusercontent.com:443/idtoken?secret=query",
                ENV["ACTIONS_ID_TOKEN_REQUEST_URL"] + "#fragment",
            ],
        }.items():
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaises(pages.DeploymentError):
                    pages.Context.from_environment({**ENV, key: value})

    def test_old_or_foreign_run_cannot_fetch_oidc_or_create_deployment(self):
        for changes in ({"id": True}, {"run_attempt": 1}, {"head_sha": "b" * 40},
                        {"event": "pull_request"}, {"head_branch": "main"},
                        {"repository": {"id": 100, "full_name": "osfans/trime"}},
                        {"head_repository": {"id": 101, "full_name": "X2M7/trime"}}):
            service = FakeService()
            service.run.update(changes)
            with self.subTest(changes=changes), self.assertRaises(pages.DeploymentError):
                service.deployer().deploy()
            self.assertEqual(len(service.calls), 1)

    def test_wrong_expired_or_oversize_artifact_never_creates_deployment(self):
        for changes in ({"id": "456"}, {"expired": True}, {"expired": None},
                        {"name": "other"}, {"size_in_bytes": 0}, {"size_in_bytes": True},
                        {"size_in_bytes": 1024 ** 3 + 1},
                        {"workflow_run": {**ARTIFACT["workflow_run"], "id": 122}},
                        {"workflow_run": {**ARTIFACT["workflow_run"], "head_sha": "b" * 40}},
                        {"workflow_run": {**ARTIFACT["workflow_run"], "repository_id": 101}}):
            service = FakeService()
            service.artifact.update(changes)
            with self.subTest(changes=changes), self.assertRaises(pages.DeploymentError):
                service.deployer().deploy()
            self.assertEqual(len(service.calls), 2)

    def test_terminal_and_unknown_states_fail_and_attempt_cancellation(self):
        for status in (*pages.FAILED, "new_unknown_state", None, []):
            service = FakeService([status])
            with self.subTest(status=status), self.assertRaises(pages.DeploymentError):
                service.deployer().deploy()
            self.assertTrue(service.calls[-1][1].endswith("/cancel"))

    def test_timeout_is_not_masked_by_failed_cancellation_and_all_requests_are_bounded(self):
        service = FakeService(["deployment_in_progress"] * 4)
        service.cancel_error = True
        deployer = service.deployer()
        deployer.deadline = 6
        diagnostic = io.StringIO()
        with redirect_stderr(diagnostic), self.assertRaisesRegex(pages.DeploymentError, "deadline"):
            deployer.deploy()
        self.assertEqual(service.now, 6)
        self.assertTrue(service.calls[-1][1].endswith("/cancel"))
        self.assertEqual(service.calls[-1][4], pages.CANCEL_SECONDS)
        self.assertTrue(all(0 < call[4] <= pages.REQUEST_SECONDS for call in service.calls))
        self.assertNotIn("secret", diagnostic.getvalue())

    def test_untrusted_returned_urls_are_not_followed_and_deployment_is_cancelled(self):
        for changes in ({"status_url": "https://evil.example/steal"},
                        {"page_url": "https://x2m7.github.io/trime/\nother=output"},
                        {"page_url": "https://evil.example/"}):
            service = FakeService()
            service.created.update(changes)
            with self.subTest(changes=changes), self.assertRaises(pages.DeploymentError):
                service.deployer().deploy()
            self.assertTrue(service.calls[-1][1].endswith("/cancel"))
            self.assertTrue(all(call[1].startswith(pages.API) or
                                call[1] == ENV["ACTIONS_ID_TOKEN_REQUEST_URL"]
                                for call in service.calls))

    def test_action_output_is_written_only_after_confirmed_success(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            env = {**ENV, "GITHUB_OUTPUT": str(output)}
            service = FakeService(["deployment_failed"])
            with patch.object(pages, "Deployer", return_value=service.deployer()), \
                    redirect_stderr(io.StringIO()):
                self.assertEqual(pages.main(env), 1)
            self.assertFalse(output.exists())
            service = FakeService(["succeed"])
            with patch.object(pages, "Deployer", return_value=service.deployer()), \
                    redirect_stdout(io.StringIO()):
                self.assertEqual(pages.main(env), 0)
            self.assertEqual(output.read_text(), "page_url=" + pages.PAGE_URL + "\n")

    def test_registered_interrupt_handler_cancels_known_deployment_without_success_output(self):
        for signum in (pages.signal.SIGINT, pages.signal.SIGTERM):
            service = FakeService(["succeed"])
            original = {number: pages.signal.getsignal(number)
                        for number in (pages.signal.SIGINT, pages.signal.SIGTERM)}

            def interrupting_request(method, url, token, payload, timeout):
                if method == "GET" and url == CREATED["status_url"]:
                    pages.signal.getsignal(signum)(signum, None)
                return service.request(method, url, token, payload, timeout)

            deployer = pages.Deployer(pages.Context.from_environment(ENV), interrupting_request,
                                      lambda: service.now, service.sleep)
            with tempfile.TemporaryDirectory() as directory:
                output = Path(directory) / "output"
                stdout, stderr = io.StringIO(), io.StringIO()
                with self.subTest(signum=signum), patch.object(pages, "Deployer", return_value=deployer), \
                        redirect_stdout(stdout), redirect_stderr(stderr):
                    self.assertEqual(pages.main({**ENV, "GITHUB_OUTPUT": str(output)}), 1)
                self.assertFalse(output.exists())
                self.assertEqual(stdout.getvalue(), "")
                self.assertIn("interrupted", stderr.getvalue())
                self.assertNotIn("secret", stderr.getvalue())
            self.assertTrue(service.calls[-1][1].endswith("/cancel"))
            self.assertEqual({number: pages.signal.getsignal(number) for number in original}, original)


@unittest.skipUnless(hasattr(pages.signal, "setitimer"),
                     "POSIX HTTP timer; actual Pages deployment runs on Ubuntu")
class TransportTests(unittest.TestCase):
    def test_redirects_are_denied_and_http_errors_never_expose_credential_urls_or_bodies(self):
        handler = pages.NoRedirects()
        self.assertIsNone(handler.redirect_request(None, None, 302, "redirect", {},
                                                  "https://evil.example/secret"))
        error = HTTPError(ENV["ACTIONS_ID_TOKEN_REQUEST_URL"], 302, "secret response", {},
                          io.BytesIO(b"server-secret"))
        opener = unittest.mock.Mock()
        opener.open.side_effect = error
        with patch.object(pages, "build_opener", return_value=opener), \
                self.assertRaisesRegex(pages.DeploymentError, "status 302") as caught:
            pages.request_json("GET", ENV["ACTIONS_ID_TOKEN_REQUEST_URL"], "runner-secret", None, 1)
        self.assertNotIn("secret", str(caught.exception))
        self.assertNotIn("https", str(caught.exception))

    def test_oversize_nonobject_and_malformed_json_are_rejected(self):
        for raw in (b"x" * (pages.MAX_RESPONSE_BYTES + 1), b"[]", b"invalid-secret"):
            response = unittest.mock.MagicMock()
            response.status = 200
            response.read.return_value = raw
            opener = unittest.mock.MagicMock()
            opener.open.return_value.__enter__.return_value = response
            with self.subTest(size=len(raw)), \
                    patch.object(pages, "build_opener", return_value=opener), \
                    self.assertRaises(pages.DeploymentError) as caught:
                pages.request_json("GET", pages.API, "github-secret", None, 1)
            self.assertNotIn("secret", str(caught.exception))


if __name__ == "__main__":
    unittest.main()
