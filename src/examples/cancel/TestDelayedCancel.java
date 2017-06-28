/**
 *
 */
package examples.cancel;

import test.tck.msgflow.callflows.AssertUntil;

/**
 * @author M. Ranganathan
 *
 */
public class TestDelayedCancel extends AbstractCancelTest {

    private static final int TIMEOUT = 2000;
    public TestDelayedCancel() {
        super();
    }

    public void testCancelDelay() throws Exception {
        Shootist.sendDelayedCancel = true;
        shootist.sendInvite();
        assertTrue(AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT));
//        Thread.sleep(2000);
//        shootist.checkState();
    }
}
