package test.tck.msgflow.callflows.recroute;

import java.util.Hashtable;

import javax.sip.SipListener;
import javax.sip.SipProvider;

import org.apache.log4j.Logger;

import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;
import test.tck.msgflow.callflows.ScenarioHarness;

/**
 * @author M. Ranganathan
 * @author Jeroen van Bemmel
 *
 */
public class AbstractRecRouteTestCase extends ScenarioHarness implements
        SipListener {


    protected Shootist shootist;

    private static Logger logger = Logger.getLogger("test.tck");


    protected Shootme shootme;

    private Proxy proxy;
    
    private static final int TIMEOUT = 5000;

    static {
        if ( !logger.isAttached(console))
            logger.addAppender(console);
    }

    // private Appender appender;

    public AbstractRecRouteTestCase() {

        super("TCPRecRouteTest", true);

        try {
            providerTable = new Hashtable();

        } catch (Exception ex) {
            logger.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void setUp() {

        try {
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int shootmePort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();
            super.setUp(false);
            shootist = new Shootist(shootistPort, proxyPort, getTiProtocolObjects());
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);
            shootistProvider.addSipListener(this);

            this.shootme = new Shootme(shootmePort, getTiProtocolObjects());
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootmeProvider.addSipListener(this);

            this.proxy = new Proxy(proxyPort, shootmePort, getRiProtocolObjects());
            SipProvider provider = proxy.createSipProvider();
            //provider.setAutomaticDialogSupportEnabled(false);
            providerTable.put(provider, proxy);
            provider.addSipListener(this);

            getTiProtocolObjects().start();
            if (getTiProtocolObjects() != getRiProtocolObjects())
                getRiProtocolObjects().start();
        } catch (Exception ex) {
            logger.error("Unexpected exception",ex);
            fail("unexpected exception ");
        }
    }


    public void tearDown() {
        try {
            assertTrue(
                    "Should see an INFO",
                    AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT));
            assertTrue(AssertUntil.assertUntil(shootme.getAssertion(), TIMEOUT));
            assertTrue(
                    "INVITE should be seen by proxy"
                    + "and Should see two INFO messages"
                    + "and BYE should be seen by proxy"
                    + "and ACK should be seen by proxy",
                    AssertUntil.assertUntil(proxy.getAssertion(), TIMEOUT));
            super.tearDown();
            Thread.sleep(2000);
            this.providerTable.clear();

            super.logTestCompleted();
        } catch (Exception ex) {
            logger.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }



}
