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
	private final ConcurrentHashMap<String, Lock> map = new ConcurrentHashMap<String, Lock>();
	private static final StackLogger logger = CommonLogger.getLogger(KeyedSemaphore.class);
	
    public void leaveIOCriticalSection(String key) {
        Lock creationLock = map.get(key);
        if (creationLock != null) {
            creationLock.unlock();
        }
    }
    
    public void remove(String key) {
        map.remove(key);
    }   
    
    public void enterIOCriticalSection(String key) throws IOException {
        // http://dmy999.com/article/34/correct-use-of-concurrenthashmap
        Lock creationLock = map.get(key);
        if(creationLock == null) {
            Lock newCreationLock = new ReentrantLock(true);
            creationLock = map.putIfAbsent(key, newCreationLock);
            if(creationLock == null) {
                creationLock = newCreationLock;       
                if (logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
                    logger.logDebug("new Semaphore added for key " + key);
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
}
