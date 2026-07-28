package io.thoth.server.common.scheduling

import io.thoth.server.common.extensions.toCron
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SchedulerTest {
    /** Far enough away that it never fires during a test, so only a manual launch can run the task */
    private val nextNewYear = "0 4 1 1 *".toCron()

    /** Starts the loop and waits until it actually consumes tasks. */
    private suspend fun CoroutineScope.startScheduler(scheduler: Scheduler = Scheduler()): Pair<Scheduler, Job> {
        val job = launch(Dispatchers.Default) { scheduler.start() }
        val ready = CompletableDeferred<Unit>()
        val probe = EventTask<Unit>("ready probe") { ready.complete(Unit) }
        withTimeout(5.seconds) {
            while (true) {
                try {
                    scheduler.dispatch(probe.build(Unit))
                    break
                } catch (e: IllegalStateException) {
                    delay(1.milliseconds)
                }
            }
            ready.await()
        }
        return scheduler to job
    }

    private suspend fun awaitQueue(
        scheduler: Scheduler,
        size: Int,
    ) = withTimeout(5.seconds) {
        while (scheduler.queue.size != size) {
            delay(1.milliseconds)
        }
        scheduler.queue
    }

    @Test
    fun `a failing task does not take the scheduler down`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val ran = CompletableDeferred<String>()
            val failing = EventTask<Unit>("failing") { throw IllegalStateException("task blew up") }
            val healthy = EventTask<Unit>("healthy") { ran.complete("ran") }

            scheduler.dispatch(failing.build(Unit))
            scheduler.dispatch(healthy.build(Unit))

            assertEquals("ran", withTimeout(5.seconds) { ran.await() })
            assertTrue(job.isActive, "the scheduler stopped running")
            job.cancel()
        }

    @Test
    fun `a failing task is not retried right away`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val runs = AtomicInteger()
            val failed = CompletableDeferred<Unit>()
            val task =
                CronTask("failing scan", nextNewYear) {
                    runs.incrementAndGet()
                    failed.complete(Unit)
                    throw IllegalStateException("scan blew up")
                }
            scheduler.schedule(task)
            scheduler.launchNow(task)
            withTimeout(5.seconds) { failed.await() }

            // Only the untouched cron execution is left, so the failure is not retried in a tight loop
            val queued = awaitQueue(scheduler, 1)
            assertTrue(
                queued.single().executeAt > LocalDateTime.now().plusHours(1),
                "next execution was ${queued.single().executeAt}",
            )
            delay(100.milliseconds)
            assertEquals(1, runs.get(), "the failing task ran more than once")
            job.cancel()
        }

    @Test
    fun `a manually launched job runs right away`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val ran = CompletableDeferred<Unit>()
            val task = CronTask("full scan", nextNewYear) { ran.complete(Unit) }
            scheduler.schedule(task)
            // Let the loop settle into waiting for the cron execution, otherwise it would pick the manual launch up
            // on its way there and the test could not tell whether launching wakes it
            delay(200.milliseconds)

            // Without the manual launch this would only run in the next january
            scheduler.launchNow(task)

            withTimeout(5.seconds) { ran.await() }
            job.cancel()
        }

    @Test
    fun `every launch queues an execution of its own next to the cron one`() {
        // No loop running, so nothing can consume the queue while it is inspected
        val scheduler = Scheduler()
        val task = CronTask("full scan", nextNewYear) {}
        scheduler.schedule(task)
        assertEquals(1, scheduler.queue.size, "scheduling queued more than one execution")

        repeat(3) { scheduler.launchNow(task) }

        assertEquals(4, scheduler.queue.size, "queue was ${scheduler.queue}")
        val soon = LocalDateTime.now().plusMinutes(1)
        assertEquals(3, scheduler.queue.count { it.executeAt < soon }, "not every launch queued an execution")
        // The cron execution is untouched by the launches
        assertEquals(1, scheduler.queue.count { it.executeAt > soon }, "the cron execution was replaced")
    }

    @Test
    fun `scheduling the same task twice does not double its cron executions`() {
        val scheduler = Scheduler()
        val task = CronTask("full scan", nextNewYear) {}

        repeat(3) { scheduler.schedule(task) }

        assertEquals(1, scheduler.queue.size, "queue was ${scheduler.queue}")
    }

    @Test
    fun `a launched execution does not leave a second cron execution behind`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val ran = CompletableDeferred<Unit>()
            val task = CronTask("full scan", nextNewYear) { ran.complete(Unit) }
            scheduler.schedule(task)
            scheduler.launchNow(task)
            withTimeout(5.seconds) { ran.await() }

            val queued = awaitQueue(scheduler, 1)
            assertTrue(
                queued.single().executeAt > LocalDateTime.now().plusHours(1),
                "a launched execution added a cron execution: $queued",
            )
            job.cancel()
        }

    @Test
    fun `an event dispatched while a task runs is not lost`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val order = CopyOnWriteArrayList<String>()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val slow =
                EventTask<Unit>("slow") {
                    entered.complete(Unit)
                    release.await()
                    order.add("slow")
                }
            val quick = EventTask<Unit>("quick") { order.add("quick") }

            scheduler.dispatch(slow.build(Unit))
            withTimeout(5.seconds) { entered.await() }
            // The loop is busy running the slow task, so it is not waiting on the wake-up channel
            scheduler.dispatch(quick.build(Unit))
            release.complete(Unit)

            withTimeout(5.seconds) {
                while (order.size < 2) {
                    delay(1.milliseconds)
                }
            }
            assertEquals(listOf("slow", "quick"), order.toList())
            job.cancel()
        }

    @Test
    fun `dispatching before the scheduler is started fails`() {
        val scheduler = Scheduler()
        val event = EventTask<Unit>("event") {}

        assertFailsWith<IllegalStateException> { scheduler.dispatch(event.build(Unit)) }
    }

    @Test
    fun `stop lets start return`() =
        runBlocking {
            val (scheduler, job) = startScheduler()

            scheduler.stop()

            withTimeout(5.seconds) { job.join() }
            assertTrue(job.isCompleted, "start did not return")
        }

    @Test
    fun `it can be started again after it was stopped`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val event = EventTask<Unit>("event") {}

            scheduler.stop()
            withTimeout(5.seconds) { job.join() }
            assertFailsWith<IllegalStateException>("dispatching worked while stopped") {
                scheduler.dispatch(event.build(Unit))
            }

            val ranAfterRestart = CompletableDeferred<Unit>()
            val afterRestart = EventTask<Unit>("after restart") { ranAfterRestart.complete(Unit) }
            val restarted = launch(Dispatchers.Default) { scheduler.start() }
            withTimeout(5.seconds) {
                while (true) {
                    try {
                        scheduler.dispatch(afterRestart.build(Unit))
                        break
                    } catch (e: IllegalStateException) {
                        delay(1.milliseconds)
                    }
                }
                ranAfterRestart.await()
            }
            restarted.cancel()
        }

    @Test
    fun `stopping it before it ran does not keep it from starting`() =
        runBlocking {
            val scheduler = Scheduler()
            scheduler.stop()

            val (_, job) = startScheduler(scheduler)

            assertTrue(job.isActive, "the scheduler stopped right away")
            job.cancel()
        }

    @Test
    fun `starting it while it runs does not start a second loop`() =
        runBlocking {
            val (scheduler, job) = startScheduler()
            val task = CronTask("full scan", nextNewYear) {}
            scheduler.schedule(task)

            // Returns instead of running a loop of its own, which would execute everything twice
            withTimeout(2.seconds) { scheduler.start() }

            assertTrue(job.isActive, "the first loop stopped")
            assertEquals(1, scheduler.queue.size, "queue was ${scheduler.queue}")
            job.cancel()
        }
}
