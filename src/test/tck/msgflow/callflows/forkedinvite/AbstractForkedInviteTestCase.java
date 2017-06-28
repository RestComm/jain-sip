/**
 *
 */
package test.tck.msgflow.callflows.forkedinvite;

import gov.nist.javax.sip.SipProviderImpl;

import java.io.IOException;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.helpers.NullEnumeration;

import test.tck.msgflow.callflows.ProtocolObjects;
import test.tck.msgflow.callflows.ScenarioHarness;

import junit.framework.TestCase;
import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;

/**
 * @author M. Ranganathan
 *
 */
public class AbstractForkedInviteTestCase extends ScenarioHarness implements
        SipListener {


    protected Shootist shootist;

    private static Logger logger = Logger.getLogger("test.tck");


    protected Shootme shootme;

    private Shootme shootme2;

    private static final int TIMEOUT = 8000;


    // private Appender appender;

    public AbstractForkedInviteTestCase() {

        super("forkedInviteTest", true);

        try {
            providerTable = new Hashtable();

        } catch (Exception ex) {
            logger.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void setUp() {

        try {
            super.setUp(false,1,3);
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();
            int shootmePort = NetworkPortAssigner.retrieveNextPort();
            int shootme2Port = NetworkPortAssigner.retrieveNextPort();
            int[] targets = {shootmePort, shootme2Port};
            
            shootist = new Shootist(shootistPort, proxyPort, super.getTiProtocolObjects(0));
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);

            this.shootme = new Shootme(shootmePort, getTiProtocolObjects(1));
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);



            this.shootme2 = new Shootme(shootme2Port, getTiProtocolObjects(2));
            shootmeProvider = shootme2.createProvider();
            providerTable.put(shootmeProvider, shootme2);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            Proxy proxy = new Proxy(proxyPort, getRiProtocolObjects(0), targets);
            SipProvider provider = proxy.createSipProvider();
            provider.setAutomaticDialogSupportEnabled(false);
            providerTable.put(provider, proxy);
            provider.addSipListener(this);

            super.start();
        } catch (Exception ex) {
            System.out.println(ex.toString());
            fail("unexpected exception ");
        }
    }




    public void tearDown() {
        try {
            assertTrue(
                    "Should see two distinct dialogs and Should see the original (default) dialog in the forked set",
                    AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT));
            assertTrue(
                    "Should see invite and Should see either an ACK or a BYE, or both",
                    AssertUntil.assertUntil(shootme.getAssertion(), TIMEOUT));
            assertTrue(
                    "Should see invite and Should see either an ACK or a BYE, or both",
                    AssertUntil.assertUntil(shootme2.getAssertion(), TIMEOUT));
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
