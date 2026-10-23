package com.osfans.trime.util
import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import java.io.IOException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.TimeUnit
class CancellationCleanupDiagnostic : StringSpec({
  "actual cancelled await retains the original cleanup failure" {
    runBlocking {
      val pipe = object : BlockingPipe() { override fun close() = release() }
      val destroyFailure = IOException("Synthetic destruction failure")
      val process = PipeProcess(pipe, { throw destroyFailure })
      val reading = async(Dispatchers.Default) { process.consumeOutput { it.readLines() } }
      check(pipe.entered.await(5, TimeUnit.SECONDS))
      withTimeout(3_000L) { reading.cancelAndJoin() }
      val failure = shouldThrow<CancellationException> { reading.await() }
      val pending = ArrayDeque<Throwable>()
      val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
      pending.add(failure)
      while (pending.isNotEmpty()) {
        val next = pending.removeFirst()
        if (!seen.add(next)) continue
        println("NODE identity=${System.identityHashCode(next)} class=${next.javaClass.name} message=${next.message} cause=${next.cause?.let { System.identityHashCode(it) }} suppressed=${next.suppressed.map { System.identityHashCode(it) }} originalDestroy=${next === destroyFailure}")
        next.cause?.let(pending::add)
        pending.addAll(next.suppressed)
      }
      println("DESTROY originalIdentity=${System.identityHashCode(destroyFailure)} actualDestructions=${process.destructions.get()} reachable=${destroyFailure in seen}")
      failure.printStackTrace(System.out)
      (destroyFailure in seen) shouldBe true
      process.assertReleased()
    }
  }
})
