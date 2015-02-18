package kvstore;


import java.util.ArrayList;
import java.util.List;

public class ThreadPool {

    /* Array of threads in the threadpool */
    public Thread threads[];
    public List<Runnable> jobs;

    /**
     * Constructs a Threadpool with a certain number of threads.
     *
     * @param size number of threads in the thread pool
     */
    public ThreadPool(int size) {
        threads = new Thread[size];
        jobs = new ArrayList<Runnable>();
        for (int i = 0; i < size; i++) {
            threads[i] = new WorkerThread(this);
            threads[i].start();
        }
    }

    /**
     * Add a job to the queue of jobs that have to be executed. As soon as a
     * thread is available, the thread will retrieve a job from this queue if
     * if one exists and start processing it.
     *
     * @param r job that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public synchronized  void addJob(Runnable r) throws InterruptedException {
        jobs.add(r);
        notifyAll();
    }

    /**
     * Block until a job is present in the queue and retrieve the job
     * @return A runnable task that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public synchronized Runnable getJob() throws InterruptedException {
        while (jobs.isEmpty()) {
            wait();
        }
        return jobs.remove(0);
    }

    /**
     * A thread in the thread pool.
     */
    public class WorkerThread extends Thread {

        public ThreadPool threadPool;

        /**
         * Constructs a thread for this particular ThreadPool.
         *
         * @param pool the ThreadPool containing this thread
         */
        public WorkerThread(ThreadPool pool) {
            threadPool = pool;
        }

        /**
         * Scan for and execute tasks.
         */
        @Override
        public void run() {
            while (true) {
                try {
                    Runnable newJob = threadPool.getJob();
                    newJob.run();
                } catch (InterruptedException ex) {
                    // ignore and try again.
                }
            }
        }
    }
}
