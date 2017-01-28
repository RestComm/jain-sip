package gov.nist.javax.sip;

import gov.nist.core.CommonLogger;
import gov.nist.core.NamingThreadFactory;
import gov.nist.core.StackLogger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadAffinityExecutor implements ScheduledExecutorService {

    private static StackLogger logger = CommonLogger.getLogger(ThreadAffinityExecutor.class);

    private final List<MDCScheduledTHExecutor> executors;
    private final AtomicInteger nextThread = new AtomicInteger(0);

    public ThreadAffinityExecutor(int corePoolSize) {
        executors = new ArrayList();
        NamingThreadFactory namingThreadFactory = new NamingThreadFactory("AffinityJAIN");
        for (int i = 0; i < corePoolSize; i++) {
            executors.add(new MDCScheduledTHExecutor(1, namingThreadFactory));
        }
        schedulePurgeTaskIfNeeded();
    }

    private synchronized int retrieveNextThread() {
        int nThread;
        nThread = nextThread.incrementAndGet();
        if (nThread >= executors.size()) {
            nThread = 0;
            nextThread.set(0);
        }
        if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
            logger.logDebug("Next thread id=" + nThread);
        }
        return nThread;
    }

    private int calculateAffinityThread(Runnable runnable) {
        int affThreadIndex = 0;
        if (runnable instanceof ThreadAffinityIdentifier) {
            ThreadAffinityIdentifier tTask = (ThreadAffinityIdentifier) runnable;
            Object tHash = tTask.getThreadHash();
            if (tHash != null) {
                affThreadIndex = Math.abs(tHash.hashCode() % executors.size());
                if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                    logger.logDebug("Runnable assigned to thread (" + tHash + "," + affThreadIndex + ")");
                }
            } else {
                affThreadIndex = retrieveNextThread();
            }
        } else {
            affThreadIndex = retrieveNextThread();
        }
        return affThreadIndex;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        int affThread = calculateAffinityThread(command);
        return executors.get(affThread).schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        int affThread = calculateAffinityThread(command);
        return executors.get(affThread).scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        int affThread = calculateAffinityThread(command);
        return executors.get(affThread).scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        for (ScheduledExecutorService serv : executors) {
            serv.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isShutdown() {
        return executors.get(0).isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executors.get(0).isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executors.get(0).awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        int affThread = calculateAffinityThread(task);
        return executors.get(affThread).submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        int affThread = calculateAffinityThread(task);
        return executors.get(affThread).submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void execute(Runnable command) {
        int affThread = calculateAffinityThread(command);
        executors.get(affThread).execute(command);
    }

    private void schedulePurgeTaskIfNeeded() {
        //TODO
        /*int purgePeriod = Integer.parseInt(sipStackImpl.getConfigurationProperties().getProperty("gov.nist.javax.sip.timers.SCHEDULED_EXECUTOR_PURGE_DELAY", "1"));
        if (purgePeriod > 0) {
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                            logger.logDebug("Purging canceled timer tasks...");
                        }
                        for (MDCScheduledTHExecutor serv : executors) {
                            serv.purge();
                        }
                        if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                            logger.logDebug("Purging canceled timer tasks completed.");
                        }
                    } catch (Exception e) {
                        logger.logError("failed to execute purge", e);
                    }
                }
            };
            scheduleWithFixedDelay(r, purgePeriod, purgePeriod, TimeUnit.MINUTES);
        }*/
    }
}
