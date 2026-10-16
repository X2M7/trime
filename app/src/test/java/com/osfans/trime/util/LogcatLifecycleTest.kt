/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LogcatLifecycleTest :
    StringSpec({
        "snapshot with no PID has no empty argument and releases its child" {
            runBlocking {
                val process = PipeProcess()
                var command = emptyList<String>()
                val logcat = Logcat(null, 35) {
                    command = it
                    process
                }
                logcat.getLogAsync().await().getOrThrow() shouldBe listOf("one", "two")
                command shouldBe listOf("logcat", "-v", "time", "-d")
                process.assertReleased()
            }
        }

        "a failed snapshot command returns failure instead of empty success" {
            runBlocking {
                val process = PipeProcess(java.io.ByteArrayInputStream(ByteArray(0)), exitCode = 1)
                val logcat = Logcat(null, 35) { process }
                val result = logcat.getLogAsync().await()
                result.isFailure shouldBe true
                result.exceptionOrNull()?.message shouldBe "Unable to read logcat snapshot"
                process.assertReleased()
            }
        }

        "API21 filters full time records without unsupported command options" {
            runBlocking {
                val process = PipeProcess(
                    java.io.ByteArrayInputStream(
                        (
                            "10-16 10:34:40.797 W/Test( 123): own warning\n" +
                                "10-16 10:34:40.798 E/Else( 1234): another PID\n" +
                                "not a record Test( 123): payload\n" +
                                "10-16 10:34:40.799 E/Test( 123): own error\n"
                            ).toByteArray(),
                    ),
                )
                var command = emptyList<String>()
                val logcat = Logcat(123, 21) {
                    command = it
                    process
                }
                logcat.getLogAsync().await().getOrThrow() shouldBe listOf(
                    "10-16 10:34:40.797 W/Test( 123): own warning",
                    "10-16 10:34:40.799 E/Test( 123): own error",
                )
                command shouldBe listOf("logcat", "-v", "time", "-d")
                process.assertReleased()
            }
        }

        "clear command waits for completion and closes all streams" {
            runBlocking {
                val process = PipeProcess()
                val logcat = Logcat(null, 35) { process }
                withTimeout(5_000L) { logcat.clearLog().join() }
                process.assertReleased()
            }
        }

        "stop during process creation releases that late child and allows another stream" {
            runBlocking {
                val entered = CountDownLatch(1)
                val finishCreation = CountDownLatch(1)
                val first = PipeProcess()
                val secondPipe = BlockingPipe()
                val second = PipeProcess(secondPipe, secondPipe::release, naturalExit = false)
                val starts = AtomicInteger()
                val logcat = Logcat(null, 35) {
                    if (starts.incrementAndGet() == 1) {
                        entered.countDown()
                        check(finishCreation.await(5, TimeUnit.SECONDS))
                        first
                    } else {
                        second
                    }
                }
                val oldJob = logcat.initLogFlow()
                check(entered.await(5, TimeUnit.SECONDS))
                logcat.shutdownLogFlow()
                val newJob = logcat.initLogFlow()
                check(secondPipe.entered.await(5, TimeUnit.SECONDS))
                finishCreation.countDown()
                withTimeout(5_000L) { oldJob.join() }
                first.assertReleased()
                // The old stream's completion must not erase the newly running stream's slot.
                newJob.isActive shouldBe true
                logcat.shutdownLogFlow()
                withTimeout(5_000L) { newJob.join() }
                second.assertReleased()
                starts.get() shouldBe 2
            }
        }

        "a completed stream can start again without keeping a stale process" {
            runBlocking {
                val children = mutableListOf<PipeProcess>()
                val logcat = Logcat(null, 35) { PipeProcess().also { children.add(it) } }
                withTimeout(5_000L) { logcat.initLogFlow().join() }
                withTimeout(5_000L) { logcat.initLogFlow().join() }
                children.size shouldBe 2
                children.forEach { it.assertReleased() }
            }
        }
    })
