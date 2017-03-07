package test.unit.gov.nist.javax.sip.stack;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.stack.KeyedSemaphore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.junit.Assert;

/**
 * This test is trying to mimic NioHandler usage to find concurrency issues.
 * 
 * Specially for openoutgoingconnect scenarios, all all possible branches
 * @author jaime
 */
public class KeyedSemaphoreTest extends TestCase{

    private static final int THREAD_NUM = 20;
    private static final int ASSERT_WAIT = 500;
    private static final StackLogger logger = CommonLogger.getLogger(KeyedSemaphoreTest.class);

    public KeyedSemaphoreTest() {
    }

    class SunnyConnect extends OpenOutgoing {

        public SunnyConnect(KeyedSemaphore s, String k) {
            super(s, k);
        }

        @Override
        public Boolean call() throws Exception {
            boolean entered = false;
            try {
                logger.logDebug("beforeEnter");
                sem.enterIOCriticalSection(key);
                entered = true;
                logger.logDebug("afterEnter");
                //simulate blocking connect
                Thread.sleep(10);
                put();
            } finally {
                if (entered) {
                    logger.logDebug("beforeLeave");
                    sem.leaveIOCriticalSection(key);
                    logger.logDebug("afterLeave");
                }
            }
            return true;
        }
    }

    class FaultyConnect extends OpenOutgoing {

        public FaultyConnect(KeyedSemaphore s, String k) {
            super(s, k);
        }

        @Override
        public Boolean call() throws Exception {
            boolean entered = false;
            try {
                logger.logDebug("beforeEnter");
                sem.enterIOCriticalSection(key);
                entered = true;
                logger.logDebug("afterEnter");
                //simulate blocking connect
                Thread.sleep(100);
                //simulate connect error
                throw new Exception();
            } catch (Exception e) {
                //simulate blocking connect retry
                Thread.sleep(10);
                put();
            } finally {
                if (entered) {
                    sem.remove(key);
                    sem.leaveIOCriticalSection(key);
                    logger.logDebug("afterLeave");
                }
            }
            return true;
        }
    }

    abstract class OpenOutgoing implements Callable<Boolean> {

        protected KeyedSemaphore sem;
        protected String key;

        OpenOutgoing(KeyedSemaphore s, String k) {
            sem = s;
            key = k;
        }

        protected void remove() throws Exception {
            boolean entered = false;
            logger.logDebug("enter Remove");
            try {
                logger.logDebug("beforeEnter");
                sem.enterIOCriticalSection(key);
                logger.logDebug("afterEnter");
                entered = true;
                //simulate some quick job
                Thread.sleep(1);                
                sem.remove(key);
            } finally {
                if (entered) {
                    logger.logDebug("beforeLeave");
                    sem.leaveIOCriticalSection(key);
                    logger.logDebug("afterLeave");
                }
            }
            logger.logDebug("exit Remove");            
        }

        protected void put() throws Exception {
            boolean entered = false;
            try {
                logger.logDebug("beforeputEnter");
                sem.enterIOCriticalSection(key);
                logger.logDebug("afterputEnter");
                entered = true;
                //simulate some quick job
                Thread.sleep(1);
            } finally {
                if (entered) {
                    logger.logDebug("beforeputLeave");
                    sem.leaveIOCriticalSection(key);
                    logger.logDebug("afterputLeave");
                }
            }
            logger.logDebug("exitPut");
        }

    }

    public void testSunnyOnly() throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(THREAD_NUM);
        KeyedSemaphore sem = new KeyedSemaphore();
        final String key = "key";
        List<Callable<Boolean>> tasks = new ArrayList();
        for (int i = 0; i < THREAD_NUM; i++) {
            tasks.add(new SunnyConnect(sem, key));
        }
        List<Future<Boolean>> invokeAll = newFixedThreadPool.invokeAll(tasks);
        newFixedThreadPool.awaitTermination(ASSERT_WAIT, TimeUnit.MILLISECONDS);
        for (Future<Boolean> futAux : invokeAll) {
            Assert.assertTrue(futAux.get(ASSERT_WAIT, TimeUnit.MILLISECONDS));
        }
        Assert.assertEquals(1, sem.getNumberOfSemaphores());
    }

    public void testRainyOnly() throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(THREAD_NUM);
        final KeyedSemaphore sem = new KeyedSemaphore();
        final String key = "key";
        List<Callable<Boolean>> tasks = new ArrayList();
        for (int i = 0; i < THREAD_NUM; i++) {
            tasks.add(new FaultyConnect(sem, key));
        }
        List<Future<Boolean>> invokeAll = newFixedThreadPool.invokeAll(tasks);
        newFixedThreadPool.awaitTermination(ASSERT_WAIT, TimeUnit.MILLISECONDS);
        for (Future<Boolean> futAux : invokeAll) {
            Assert.assertTrue(futAux.get(ASSERT_WAIT, TimeUnit.MILLISECONDS));
        }
        Assert.assertEquals(0, sem.getNumberOfSemaphores());
    }
}
