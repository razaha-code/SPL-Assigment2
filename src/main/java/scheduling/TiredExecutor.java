package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import static java.lang.String.format;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        this.workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            this.workers[i] = new TiredThread(i,0);
            this.idleMinHeap.put(workers[i]);
        }
    }

    public synchronized void submit(Runnable task) { // לחזור לזה
        // TODO
        while(idleMinHeap.isEmpty()) {
                wait();
            }
            Thread worker = idleMinHeap.poll();
            worker.newTask(task);
            inFlight.incrementAndGet();
        
    }

    public synchronized void submitAll(Iterable<Runnable> tasks) { // לחזור לזה
        // TODO: submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            while(idleMinHeap.isEmpty()) {
                wait();
            }
            this.submit(task);
        }
    }

    public synchronized void shutdown() throws InterruptedException { // get back to
        // TODO
        this.notifyAll();
        for (TiredThread worker : workers) {
            worker.shutdown();
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
