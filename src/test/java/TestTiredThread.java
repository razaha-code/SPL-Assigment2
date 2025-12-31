import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import scheduling.TiredThread;

public class TestTiredThread {

    private TiredThread worker;

    @BeforeEach
    public void setUp() {
        worker = new TiredThread(1, 1.0);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // לוודא שה־Thread לא נשאר חי בין טסטים
        if (worker != null && worker.isAlive()) {
            worker.shutdown();
            worker.join(1_000);
        }
    }

    // ---------------------------
    // newTask
    // ---------------------------

    @Test
    public void testNewTaskEnqueuesTaskWhenAvailable() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);

        Runnable task = () -> {
            ran.set(true);
            done.countDown();
        };

        worker.start();
        worker.newTask(task);

        assertTrue(done.await(1, TimeUnit.SECONDS),
                "Task should complete within timeout");
        assertTrue(ran.get(), "Task runnable should have been executed");
    }

    @Test
    public void testNewTaskThrowsWhenQueueFull() {
        // לא מפעילים את ה־Thread, כך שה־handoff לא נצרך – התור יתמלא
        Runnable task = () -> {};

        // התור בגודל 1 – המשימה הראשונה נכנסת
        worker.newTask(task);

        // המשימה השנייה אמורה לזרוק IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> worker.newTask(task),
                "newTask should throw when worker queue is full");
    }

    // ---------------------------
    // run (התנהגות הריצה)
    // ---------------------------

    @Test
    public void testRunExecutesTaskAndUpdatesBusyAndTimeUsed() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        Runnable task = () -> {
            taskStarted.countDown();
            try {
                // מחזיק את ה־Thread "עסוק" עד שנאפשר לו לסיים
                allowFinish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ran.set(true);
        };

        worker.start();
        worker.newTask(task);

        // מחכים שהמשימה תתחיל
        assertTrue(taskStarted.await(1, TimeUnit.SECONDS),
                "Task should start within timeout");
        assertTrue(worker.isBusy(), "Worker should be busy while running task");

        // עכשיו מאפשרים לסיים
        allowFinish.countDown();

        // ניתן קצת זמן
        Thread.sleep(100);
        assertTrue(ran.get(), "Task runnable should have finished");
        assertFalse(worker.isBusy(), "Worker should not be busy after finishing task");
        assertTrue(worker.getTimeUsed() > 0, "timeUsed should be > 0 after running a task");
        assertTrue(worker.getTimeIdle() >= 0, "timeIdle should be >= 0");

        // סוגרים בסוף עם shutdown כדי לצאת מהלולאה
        worker.shutdown();
        worker.join(1_000);
    }

    // ---------------------------
    // shutdown
    // ---------------------------

    @Test
    public void testShutdownStopsIdleWorker() throws Exception {
        worker.start();

        // נותנים לו רגע להיכנס ל-take() על התור
        Thread.sleep(50);

        worker.shutdown();
        worker.join(1_000);

        assertFalse(worker.isAlive(), "Worker should terminate after shutdown");
    }

    @Test
    public void testShutdownWhileBusyStopsAfterCurrentTask() throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        Runnable longTask = () -> {
            taskStarted.countDown();
            try {
                allowFinish.await(); // "עבודה ארוכה"
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ran.set(true);
        };

        worker.start();
        worker.newTask(longTask);

        // מחכים שהמשימה תתחיל
        assertTrue(taskStarted.await(1, TimeUnit.SECONDS),
                "Long task should start");

        // קוראים ל-shutdown בזמן שהוא עסוק
        worker.shutdown();

        // עכשיו מאפשרים למשימה לסיים
        allowFinish.countDown();

        // מחכים שה־Thread יסיים
        worker.join(1_000);

        assertTrue(ran.get(), "Long task should have completed before shutdown");
        assertFalse(worker.isAlive(), "Worker should be terminated after shutdown");
    }
}
