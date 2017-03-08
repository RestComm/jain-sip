/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
package gov.nist.javax.sip.stack;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class KeyedSemaphore {

    private final ConcurrentHashMap<String, ReentrantLock> map = new ConcurrentHashMap<String, ReentrantLock>();
    private static final StackLogger logger = CommonLogger.getLogger(KeyedSemaphore.class);

    public void leaveIOCriticalSection(String key) {
        Lock creationLock = map.get(key);
        if (creationLock != null) {
            creationLock.unlock();
            if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                logger.logDebug("sem unlocked:" + creationLock);
            }
        }
    }

    /**
     * It is assumed the current thread has locked the lock previously. Under
     * this assumption is safer to unlock after removal, so any blocked thread
     * on waiting removed block may proceed without timeout.
     * 
     * Only remove if no other threads are already waiting on this lock, otherwise
     * unexpected behaviour may arise by locking one lock object, and trying to release
     * another instance of lock for the same key.
     *
     * @param key
     */
    public void remove(String key) {
        ReentrantLock myLock = map.get(key);
        if (myLock != null &&
                !myLock.hasQueuedThreads() && myLock.isHeldByCurrentThread()) {
            map.remove(key);
            logger.logDebug("sem removed:" + myLock);
            //This lock is no longer reacheable
            //remove all locks since this thread wont be able to unlock those anymore.
            while (myLock.isHeldByCurrentThread() && myLock.getHoldCount() > 0) {
                logger.logDebug("unlocking after remove:" + myLock);
                myLock.unlock();
            }            
        }//if there are other threads waiting on this semaphore let them reuse it
        //so unwanted side effects are prevented
    }

    public void enterIOCriticalSection(String key) throws IOException {
        // http://dmy999.com/article/34/correct-use-of-concurrenthashmap
        Lock creationLock = map.get(key);
        if (creationLock == null) {
            ReentrantLock newCreationLock = new ReentrantLock(true);
            creationLock = map.putIfAbsent(key, newCreationLock);
            if (creationLock == null) {
                creationLock = newCreationLock;
                if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                    logger.logDebug("new Semaphore added for key: " + key);
                }
            }
        }

        try {
            boolean retval = creationLock.tryLock(10, TimeUnit.SECONDS);
            if (!retval) {
                throw new IOException("Could not acquire IO Semaphore'" + key
                        + "' after 10 seconds -- giving up ");
            }
        } catch (InterruptedException e) {
            throw new IOException("exception in acquiring sem");
        }
    }

    public int getNumberOfSemaphores() {
        return map.size();
    }
}
