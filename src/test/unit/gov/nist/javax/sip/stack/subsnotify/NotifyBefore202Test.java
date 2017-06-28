package test.unit.gov.nist.javax.sip.stack.subsnotify;

import junit.framework.TestCase;
import static test.tck.TestHarness.assertTrue;
import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.TestAssertion;

public class NotifyBefore202Test  extends TestCase {
	Subscriber subscriber;
	Notifier   notifier;
        private static final int TIMEOUT = 60000;
	
	
	public void setUp() throws Exception {
		subscriber = Subscriber.createSubcriber();
		notifier = Notifier.createNotifier();
                subscriber.setNotifierPort(notifier.getPort());
                notifier.setSubscriberPort(subscriber.getPort());
		
	}
	
	
	public void testSendSubscribe() {
		subscriber.sendSubscribe();
	}
	
	/*
	 * Non Regression test for issue http://java.net/jira/browse/JSIP-374
	 */
	public void testInDialogSubscribe() throws InterruptedException {
		subscriber.setInDialogSubcribe(true);
		subscriber.sendSubscribe();
		assertTrue(
                    AssertUntil.assertUntil(new TestAssertion() {
                        @Override
                        public boolean assertCondition() {
                            return subscriber.checkState();
                        };
                    }, TIMEOUT)
                );
	}
	
	public void tearDown() throws Exception {		
		subscriber.tearDown();
		notifier.tearDown();
	}

}
