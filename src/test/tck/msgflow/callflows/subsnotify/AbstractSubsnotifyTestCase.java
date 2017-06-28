/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), and others.
* This software is has been contributed to the public domain.
* As a result, a formal license is not needed to use the software.
*
* This software is provided "AS IS."
* NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
*
*/
package test.tck.msgflow.callflows.subsnotify;

import javax.sip.SipListener;
import javax.sip.SipProvider;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;
import test.tck.msgflow.callflows.ScenarioHarness;

/**
 *
 * Implements common setup and tearDown sequence for Subsnotify tests
 *
 *
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 *
 */
public abstract class AbstractSubsnotifyTestCase extends ScenarioHarness implements
        SipListener {


    protected Notifier notifier1;

    protected Subscriber subscriber;

    private Notifier notifier2;

    protected Forker forker;

    private static Logger logger = Logger.getLogger("test.tck");
    
    private static final int TIMEOUT = 5000;
    
    protected int forkerPort;

    static {
        if (!logger.isAttached(console)) {

            logger.addAppender(console);

        }
    }

    public AbstractSubsnotifyTestCase() {
        super("subsnotify", true);
    }

    public void setUp() throws Exception {
        try {
            super.setUp(1,3);

            logger.info("SubsNotifyTest: setup()");
            int notifier1Port = NetworkPortAssigner.retrieveNextPort();
            int notifier2Port = NetworkPortAssigner.retrieveNextPort();
            forkerPort = NetworkPortAssigner.retrieveNextPort();
            int subcriberPort = NetworkPortAssigner.retrieveNextPort();
            
            notifier1 = new Notifier(getTiProtocolObjects(0));
            SipProvider notifier1Provider = notifier1.createProvider(notifier1Port);
            providerTable.put(notifier1Provider, notifier1);

            notifier2 = new Notifier(getTiProtocolObjects(1));
            SipProvider notifier2Provider = notifier2.createProvider(notifier2Port);
            providerTable.put(notifier2Provider, notifier2);

            forker = new Forker(getRiProtocolObjects(0));
            SipProvider forkerProvider = forker.createProvider(forkerPort, notifier1Port, notifier2Port);
            providerTable.put(forkerProvider, forker);

            subscriber = new Subscriber(getTiProtocolObjects(2));
            SipProvider subscriberProvider = subscriber.createProvider(subcriberPort);
            providerTable.put(subscriberProvider, subscriber);

            notifier1Provider.addSipListener(this);
            notifier2Provider.addSipListener(this);
            forkerProvider.addSipListener(this);
            subscriberProvider.addSipListener(this);

            super.start();
        } catch (Exception ex) {
            logger.error("unexpected excecption ", ex);
            fail("unexpected exception");
        }
    }

    public void tearDown() throws Exception {
        try {
            assertTrue(AssertUntil.assertUntil(subscriber.getAssertion(), TIMEOUT));
            assertTrue(
                    "Did not see subscribe",
                    AssertUntil.assertUntil(notifier1.getAssertion(), TIMEOUT));
            assertTrue(
                    "Did not see subscribe",
                    AssertUntil.assertUntil(notifier2.getAssertion(), TIMEOUT));
            Thread.sleep(2000);
            super.tearDown();
            this.providerTable.clear();
            logTestCompleted();
        } catch (Exception ex) {
            logger.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
        super.tearDown();
    }






}
