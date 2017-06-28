/**
 *
 */
package examples.cancel;

import test.tck.msgflow.callflows.AssertUntil;

/**
 * @author M. Ranganathan
 *
 */
public class TestCancel extends AbstractCancelTest {

    private static final int TIMEOUT = 5000;
    public TestCancel() {
        super();
    }


    public void testCancelNoDelay() throws Exception {
        Shootist.sendDelayedCancel = false;
        shootist.sendInvite();
        assertTrue(AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT));
//        Thread.sleep(5000);
//        shootist.checkState();

    }
}
