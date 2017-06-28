/**
 * 
 */
package test.unit.gov.nist.javax.sip.stack.forkedinvitedialogtimeout;

import java.util.HashSet;

import javax.sip.SipProvider;

import junit.framework.TestCase;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;

/**
 * @author M. Ranganathan
 * 
 */
public class InviteTest extends TestCase {

    protected Shootist shootist;

    private static Logger logger = Logger.getLogger("test.tck");

    protected static final Appender console = new ConsoleAppender(new SimpleLayout());

    private static int forkCount = 2;
    
    private static final int TIMEOUT = 185000;   

    protected HashSet<Shootme> shootme = new HashSet<Shootme>();

  

    private Proxy proxy;

    // private Appender appender;

    public InviteTest() {

        super("forkedInviteTest");

    }

    public void setUp() {

        try {
            super.setUp();
            

        } catch (Exception ex) {
            fail("unexpected exception ");
        }
    }

    public void tearDown() {
        try {
            
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("unexpected exception", ex);
            fail("unexpected exception ");
        }
    }

    public void testSendInviteDialogTimeoutEventSeen() throws Exception {
        try {
            int shootitsPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();
            int shootmeUa1Port = NetworkPortAssigner.retrieveNextPort();
            int shootmeUa2Port = NetworkPortAssigner.retrieveNextPort();
            int[] uaPorts = new int[2];
            uaPorts[0] = shootmeUa1Port;
            uaPorts[1] = shootmeUa2Port;
            
            shootist = new Shootist(shootitsPort, proxyPort, "on", true, uaPorts);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);

            Shootme shootmeUa = new Shootme(shootmeUa1Port, true, 4000, 4000);
            SipProvider shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            shootmeUa = new Shootme(shootmeUa2Port, true, 5000, 4000);
            shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            this.proxy = new Proxy(proxyPort, forkCount, uaPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");

            this.shootist.sendInvite(forkCount);

            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            this.shootist.checkStateForDialogTimeoutEvent();
            int ackCount = 0;
            for (Shootme shootme : this.shootme) {
                shootme.checkState();
                if (shootme.isAckSeen()) {
                    ackCount++;
                }
            }
            assertEquals("ACK count must be exactly 1", 1, ackCount);
        } finally {
            this.shootist.stop();
            for (Shootme shootme : this.shootme) {
                shootme.stop();
            }
            if (this.proxy != null) {
                this.proxy.stop();
            }
        }
    }

 
    public void testSendInviteEarlyDialogTimeoutEventSeen() throws Exception {
        try {
            int shootitsPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();
            int shootmeUa1Port = NetworkPortAssigner.retrieveNextPort();
            int shootmeUa2Port = NetworkPortAssigner.retrieveNextPort();
            int[] uaPorts = new int[2];
            uaPorts[0] = shootmeUa1Port;
            uaPorts[1] = shootmeUa2Port;
            
            shootist = new Shootist(shootitsPort, proxyPort, "on", true, uaPorts);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);

            Shootme shootmeUa = new Shootme(shootmeUa1Port, true, 4000, 4000);
            SipProvider shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            // Never send an OK. Just send RINGING.
            shootmeUa = new Shootme(shootmeUa2Port, true, 5000, -1);
            shootmeProvider = shootmeUa.createProvider();
            shootmeProvider.addSipListener(shootmeUa);
            this.shootme.add(shootmeUa);

            this.proxy = new Proxy(proxyPort, forkCount, uaPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");

            this.shootist.sendInvite(forkCount);

            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            this.shootist.checkStateForDialogTimeoutEvent();
            int ackCount = 0;
            for (Shootme shootme : this.shootme) {
                shootme.checkState();
                if (shootme.isAckSeen()) {
                    ackCount++;
                }
            }
            assertEquals("ACK count must be exactly 1", 1, ackCount);
        } finally {
            this.shootist.stop();
            for (Shootme shootme : this.shootme) {
                shootme.stop();
            }
            if ( this.proxy != null ) {
                this.proxy.stop();
            }
        }
    }
}
