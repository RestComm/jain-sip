package test.tck.msgflow.callflows.prack;

import javax.sip.SipListener;
import javax.sip.SipProvider;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.ScenarioHarness;

/**
 *
 * Implements common setup and tearDown sequence for PRACK tests
 *
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 *
 */
public abstract class AbstractPrackTestCase extends ScenarioHarness implements
        SipListener {


    protected Shootist shootist;

    protected Shootme shootme;
    
    private static final int TIMEOUT = 2000;

    private static Logger logger = Logger.getLogger("test.tck");

    static {
        if (!logger.isAttached(console)) {
            logger.addAppender(console);
        }
    }

    public AbstractPrackTestCase() {
        super("prack", true);
    }

    public void setUp() throws Exception {
        try {
            super.setUp();

            logger.info("PrackTest: setup()");
            shootist = new Shootist(getTiProtocolObjects());
            SipProvider shootistProvider = shootist.createProvider();
            providerTable.put(shootistProvider, shootist);

            shootme = new Shootme(getRiProtocolObjects());
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);

            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            if (getTiProtocolObjects() != getRiProtocolObjects())
                getTiProtocolObjects().start();
            getRiProtocolObjects().start();
        } catch (Exception ex) {
            logger.error("unexpected excecption ", ex);
            fail("unexpected exception");
        }
    }

    public void tearDown() throws Exception {
        try {
            assertTrue(AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT));
            assertTrue(AssertUntil.assertUntil(shootme.getAssertion(), TIMEOUT));
            super.tearDown();
            Thread.sleep(1000);
            this.providerTable.clear();

            logTestCompleted();
        } catch (Exception ex) {
            logger.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
        super.tearDown();
    }




}
