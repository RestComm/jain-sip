package test.tck.msgflow.callflows.prack;

import javax.sip.SipListener;
import javax.sip.SipProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import test.tck.msgflow.callflows.ScenarioHarness;

/**
 *
 * Implements common setup and tearDown sequence for PRACK tests
 *
 * @author M. Ranganathan
 * @author Ivelin Ivanov
 *
 */
public abstract class AbstractPrackTestCase extends ScenarioHarness implements SipListener {

    private static final Logger LOG = LogManager.getLogger("test.tck");

    protected Shootist shootist;

    protected Shootme shootme;

    public AbstractPrackTestCase() {
        super("prack", true);
    }

    public void setUp() throws Exception {
        try {
            super.setUp();

            LOG.info("PrackTest: setup()");
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
            LOG.error("unexpected excecption ", ex);
            fail("unexpected exception");
        }
    }

    public void tearDown() throws Exception {
        try {
            Thread.sleep(2000);
            this.shootist.checkState();
            this.shootme.checkState();
            super.tearDown();
            Thread.sleep(1000);
            this.providerTable.clear();

            logTestCompleted();
        } catch (Exception ex) {
            LOG.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
        super.tearDown();
    }




}
