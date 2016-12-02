/**
 *
 */
package test.unit.gov.nist.javax.sip.stack.timeoutontermineted;

import javax.sip.SipProvider;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 *
 */
public class TimeoutOnTerminatedTest extends TestCase {

    private static final Logger LOG = LogManager.getLogger("test.tck");

    protected Shootist shootist;

    protected Shootme shootme;

    public TimeoutOnTerminatedTest() {

        super("timeoutontermineted");

    }

    @Override
    public void setUp() {

        try {
            super.setUp();
            shootist = new Shootist(5060, 5080);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);

            shootme = new Shootme(5080, 1000);

            SipProvider shootmeProvider = shootme.createProvider();
            shootmeProvider.addSipListener(shootme);

            LOG.debug("setup completed");

        } catch (Exception ex) {
            fail("unexpected exception ");
        }
    }

    @Override
    public void tearDown() {
        try {
            Thread.sleep(60000);

            this.shootist.checkState();

            this.shootme.checkState();

            this.shootist.stop();

            this.shootme.stop();

        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void testInvite() throws Exception {
        this.shootist.sendInvite();

    }

}
