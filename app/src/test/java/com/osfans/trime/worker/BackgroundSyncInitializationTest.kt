// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.worker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BackgroundSyncInitializationTest :
    StringSpec({
        "work manager is unavailable while locked or from a device-protected context" {
            canInitializeWorkManager(23, userUnlocked = false, deviceProtectedContext = false) shouldBe true
            canInitializeWorkManager(24, userUnlocked = false, deviceProtectedContext = true) shouldBe false
            canInitializeWorkManager(35, userUnlocked = true, deviceProtectedContext = true) shouldBe false
            canInitializeWorkManager(35, userUnlocked = true, deviceProtectedContext = false) shouldBe true
        }

        "repeated start and force-start paths initialize once" {
            val calls = AtomicInteger()
            val initializer = RetryableInitializer()

            repeat(5) {
                initializer.ensureInitialized { calls.incrementAndGet() }
            }

            calls.get() shouldBe 1
        }

        "failed initialization remains retryable" {
            val calls = AtomicInteger()
            val initializer = RetryableInitializer()

            shouldThrow<IllegalStateException> {
                initializer.ensureInitialized {
                    calls.incrementAndGet()
                    error("first attempt failed")
                }
            }
            initializer.ensureInitialized { calls.incrementAndGet() }
            initializer.ensureInitialized { calls.incrementAndGet() }

            calls.get() shouldBe 2
        }

        "concurrent callers cannot double-initialize" {
            val calls = AtomicInteger()
            val initializer = RetryableInitializer()
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(8)
            try {
                val futures =
                    (1..32).map {
                        executor.submit {
                            start.await()
                            initializer.ensureInitialized { calls.incrementAndGet() }
                        }
                    }
                start.countDown()
                futures.forEach { it.get(5, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }

            calls.get() shouldBe 1
        }
    })
