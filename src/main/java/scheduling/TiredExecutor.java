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
            workers[i].start();
            this.idleMinHeap.put(workers[i]);
        }
    }

    public void submit(Runnable task) { // לחזור לזה
        // TODO: submit a single task to be executed

            TiredThread worker = idleMinHeap.take();
            inFlight.incrementAndGet();
            Runnable wrapperTask = () -> {
                try {
                    worker.newTask(task);
                    
                } finally {
                    idleMinHeap.put(worker);
                    inFlight.decrementAndGet();
                    if (inFlight.get() == 0) {
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                }
            }


            
        
    }

    public  void submitAll(Iterable<Runnable> tasks) { // לחזור לזה
        // TODO: submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            this.submit(task);
        }

        synchronized (this) {   
            while (inFlight.get() > 0) {
                this.wait();
            }
        }
    }

    public  void shutdown() throws InterruptedException { // get back to
        // TODO
        idleMinHeap.clear();
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
