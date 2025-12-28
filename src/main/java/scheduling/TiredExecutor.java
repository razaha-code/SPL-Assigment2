package scheduling;

import static java.lang.String.format;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    

    public TiredExecutor(int numThreads) {
        // TODO
        this.workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            double randomFatigueFactor = 0.5 + (Math.random() * 1.0); // (0.5 to 1.5)
            this.workers[i] = new TiredThread(i, randomFatigueFactor);
            this.idleMinHeap.put(workers[i]);
        }
        this.start();
    }
    
    private void start() {
        for (TiredThread worker : workers) {
            worker.start();
        }
    }

    public void submit(Runnable task) {
    TiredThread worker;
    try {
        // 1. השגת עובד פנוי (חוסם אם אין)
        worker = idleMinHeap.take();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
    }

    inFlight.incrementAndGet(); // סימון שמשימה יצאה לדרך

    // 2. יצירת המעטפה (Wrapper)
    Runnable wrapperTask = () -> {
        try {
            // התיקון: מריצים את המשימה האמיתית כאן!
            task.run(); 
        } finally {
            // החזרת העובד ועדכון מונים
            idleMinHeap.put(worker);
            
            if (inFlight.decrementAndGet() == 0) {
                // הערה: וודא שזה אותו האובייקט ש-submitAll מחכה עליו!
                // אם submitAll מחכה על 'this', אז זה מעולה.
                synchronized (this) { 
                    this.notifyAll();
                }
            }
        }
    };

    worker.newTask(wrapperTask);
}
    public  void submitAll(Iterable<Runnable> tasks) { // לחזור לזה
        // TODO: submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            this.submit(task);
        }

        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // במקרה של הפרעה בזמן המתנה, משחזרים את הסטטוס ויוצאים
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // Step 1: Send shutdown signal (poison pill) to all workers
        for (TiredThread worker : workers) {
            worker.shutdown();
        }

        // Step 2: Wait for all workers to finish their current tasks and terminate
        for (TiredThread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                // If the main thread is interrupted while waiting, preserve the status
                Thread.currentThread().interrupt();
                // We might choose to break here or continue trying to join others.
                // Re-throwing or breaking is safer to respect the interrupt.
                throw e; 
            }
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        String ans = "";

        for (TiredThread worker : workers) {
            // collect stats
           ans += format("Worker %d: Time Used = %d ns, Time Idle = %d ns, Fatigue = %.2f\n",
                   worker.getWorkerId(),
                   worker.getTimeUsed(),
                   worker.getTimeIdle(),
                   worker.getFatigue());

        }
        return ans;
    }
}
