#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build CLI and bounded natural-exit checks without starting Gradle or a JVM."""
import contextlib
import io
import unittest
from unittest.mock import patch

import build


class BuildArgumentsTests(unittest.TestCase):
    def arguments(self, *extra):
        return build.arguments(['debug', '--sdk', '/sdk', '--jdk', '/jdk', '--output', '/output', *extra])

    def test_one_processor_is_the_default(self):
        with patch.object(build.os, 'cpu_count', return_value=32):
            self.assertEqual(self.arguments().processors, 1)

    def test_accepts_available_processors_up_to_sixteen(self):
        for cpus, accepted in ((4, (1, 4)), (32, (1, 16)), (None, (1,))):
            with self.subTest(cpus=cpus), patch.object(build.os, 'cpu_count', return_value=cpus):
                for count in accepted:
                    self.assertEqual(self.arguments('--processors', str(count)).processors, count)

    def test_rejects_processor_values_outside_actual_host_limit(self):
        for cpus, rejected in ((4, ('0', '-1', '5', '17', '1.5', 'many')), (32, ('17',)), (None, ('2',))):
            with self.subTest(cpus=cpus), patch.object(build.os, 'cpu_count', return_value=cpus):
                for value in rejected:
                    with self.subTest(value=value), contextlib.redirect_stderr(io.StringIO()):
                        with self.assertRaises(SystemExit) as caught:
                            self.arguments('--processors', value)
                        self.assertEqual(caught.exception.code, 2)

    def test_all_phases_keep_worker_warning_and_jvm_limits(self):
        for phase in ('debug', 'release', 'lint', 'format'):
            with self.subTest(phase=phase):
                command = build.build_command(phase, 1536, 4)
                self.assertIn('--no-daemon', command)
                self.assertIn('--max-workers=1', command)
                self.assertIn('--warning-mode=fail', command)
                self.assertIn('-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=640m '
                              '-XX:ActiveProcessorCount=4 -XX:ReservedCodeCacheSize=384m', command)


class NaturalExitTests(unittest.TestCase):
    def setUp(self):
        self.elapsed = 0
        self.sleeps = []
        self.addCleanup(patch.stopall)
        patch.object(build.time, 'monotonic', side_effect=lambda: self.elapsed).start()
        patch.object(build.time, 'sleep', side_effect=self.sleep).start()

    def sleep(self, seconds):
        self.assertGreater(seconds, 0)
        self.assertLessEqual(seconds, .2)
        self.sleeps.append(seconds)
        self.elapsed += seconds

    def test_exited_children_need_no_wait(self):
        self.assertEqual(build.wait_for_children(lambda: []), [])
        self.assertEqual(self.sleeps, [])

    def test_child_can_exit_naturally_after_old_ten_second_limit(self):
        def children():
            return [42] if self.elapsed < 12 else []
        self.assertEqual(build.wait_for_children(children), [])
        self.assertGreaterEqual(self.elapsed, 12)
        self.assertLess(self.elapsed, 13)

    def test_sixty_second_deadline_returns_live_children_for_cleanup(self):
        self.assertEqual(build.NATURAL_EXIT_WAIT_SECONDS, 60)
        self.assertEqual(build.wait_for_children(lambda: [42]), [42])
        self.assertEqual(self.elapsed, 60)

    def test_exit_observation_error_cannot_be_reported_as_success(self):
        def failed_observation():
            raise PermissionError('cannot inspect child')
        with self.assertRaises(PermissionError):
            build.wait_for_children(failed_observation)


if __name__ == '__main__':
    unittest.main()
