import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import scheduling.TiredExecutor;

public class TiredExecutorTest {

    private static final int NUM_THREADS = 3;
    private TiredExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TiredExecutor(NUM_THREADS);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ---------------------------
    // 1. Constructor
    // ---------------------------


    // ---------------------------
    // 2. submit
    // ---------------------------

    @Test
    void testSubmitSingleTaskRuns() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        executor.submit(() -> {
            ran.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS),
                "Task should complete within timeout");
        assertTrue(ran.get(), "Task runnable should have been executed");
    }

    @Test
    void testSubmitMultipleTasksAllRun() throws Exception {
        int numTasks = 10;
        CountDownLatch latch = new CountDownLatch(numTasks);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < numTasks; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS),
                "All tasks should complete");
        assertEquals(numTasks, counter.get(),
                "All submitted tasks should have been executed");
    }

    // ---------------------------
    // 3. shutdown
    // ---------------------------

    @Test
    void testShutdownWaitsForRunningTasksAndStopsWorkers() throws Exception {
        // משתמשים ב־executor נפרד כדי לא להתנגש עם @AfterEach
        TiredExecutor localExec = new TiredExecutor(NUM_THREADS);

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        // משימה "ארוכה"
        localExec.submit(() -> {
            taskStarted.countDown();
            try {
                allowFinish.await(); // מחכה עד שנאפשר לה לסיים
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ran.set(true);
        });

        // לוודא שהמשימה התחילה לפני shutdown
        assertTrue(taskStarted.await(1, TimeUnit.SECONDS),
                "Long task should start before shutdown");

        // מריצים shutdown על ה־Executor ב־Thread נפרד
        Thread shutdownThread = new Thread(() -> {
            try {
                localExec.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        shutdownThread.start();

        // נותנים קצת זמן כדי לוודא שה־shutdown "מחכה" על המשימה
        Thread.sleep(100);
        assertTrue(shutdownThread.isAlive(),
                "shutdown should be waiting while task is still running");

        // עכשיו נותנים למשימה לסיים
        allowFinish.countDown();

        shutdownThread.join(1_000);
        assertFalse(shutdownThread.isAlive(),
                "shutdown should finish after task completes");
        assertTrue(ran.get(),
                "Task should have completed before shutdown returned");

        // אחרי shutdown – משימות חדשות לא אמורות לרוץ
        CountDownLatch afterLatch = new CountDownLatch(1);
        localExec.submit(afterLatch::countDown);

        assertFalse(
                afterLatch.await(300, TimeUnit.MILLISECONDS),
                "Tasks submitted after shutdown should not be executed"
        );
    }
}
