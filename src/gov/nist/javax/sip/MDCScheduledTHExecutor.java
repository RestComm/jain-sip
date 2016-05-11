package gov.nist.javax.sip;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.MDC;

public class MDCScheduledTHExecutor extends ScheduledThreadPoolExecutor {

    private static StackLogger logger = CommonLogger.getLogger(MDCScheduledTHExecutor.class);

    private static final String AFFINITY_THREAD_VAR = "AffTh";

    public MDCScheduledTHExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public MDCScheduledTHExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public MDCScheduledTHExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public MDCScheduledTHExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    class MDCFuture<V> implements RunnableScheduledFuture<V> {

        Runnable runnable;
        RunnableScheduledFuture<V> task;

        boolean done = false;

        public MDCFuture(Runnable r, RunnableScheduledFuture<V> task) {
            this.task = task;
            runnable = r;
        }

        @Override
        public boolean isPeriodic() {
            return task.isPeriodic();
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return task.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return task.compareTo(o);
        }

        public Runnable getRunnable() {
            return runnable;
        }

    }

    @Override
    public <V> RunnableScheduledFuture<V> decorateTask(
            Runnable r, RunnableScheduledFuture<V> task) {
        return new MDCFuture<V>(r, task);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
            if (r instanceof MDCFuture) {
                MDCFuture future = (MDCFuture) r;
                if (future.runnable instanceof MDCTask) {
                    MDCTask mTask = (MDCTask) future.runnable;
                    Map<String, String> mdcVars = mTask.getMDCVars();
                    if (mdcVars != null) {
                        for (String varKey : mdcVars.keySet()) {
                            MDC.remove(varKey);
                        }
                    } else {
                        MDC.remove(AFFINITY_THREAD_VAR);
                    }
                }
            }
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (logger.isLoggingEnabled(StackLogger.TRACE_INFO)) {
            if (r instanceof MDCFuture) {
                MDCFuture future = (MDCFuture) r;
                if (future.runnable instanceof MDCTask) {
                    MDCTask mTask = (MDCTask) future.runnable;
                    Map<String, String> mdcVars = mTask.getMDCVars();
                    if (mdcVars != null) {
                        for (String varKey : mdcVars.keySet()) {
                            MDC.put(varKey, mdcVars.get(varKey));
                        }
                    } else {
                        if (mTask.getThreadHash() != null) {
                            MDC.put(AFFINITY_THREAD_VAR, mTask.getThreadHash());
                        }
                    }
                }
            }
        }
        super.beforeExecute(t, r);
    }
}
