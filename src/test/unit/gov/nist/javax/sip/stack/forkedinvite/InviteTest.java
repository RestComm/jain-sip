/**
 * 
 */
package test.unit.gov.nist.javax.sip.stack.forkedinvite;

import java.util.HashSet;

import javax.sip.SipProvider;

import junit.framework.TestCase;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import static test.tck.TestHarness.assertTrue;
import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;
import test.tck.msgflow.callflows.TestAssertion;

/**
 * @author M. Ranganathan
 * 
 */
public class InviteTest extends TestCase {

    protected Shootist shootist;

    private static Logger logger = Logger.getLogger("test.tck");

    protected static final Appender console = new ConsoleAppender(new SimpleLayout());

//    private static int forkCount = 2;
    
    public static final String PREFERRED_SERVICE_VALUE="urn:urn-7:3gpp-service.ims.icsi.mmtel.gsma.ipcall"; 
   

    protected HashSet<Shootme> shootme = new HashSet<Shootme>();
    
    private static final int TIMEOUT = 60000;

  

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

    public void testInvite() throws Exception {
    	int forkCount = 2;
        try {
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();
            boolean sendRinging = true;
            int[] targetPorts = new int[forkCount];
            for  (int i = 0 ; i <  forkCount ; i ++ ) {
                int shootmePort = NetworkPortAssigner.retrieveNextPort();
                targetPorts[i] = shootmePort;
                Shootme shootme = new Shootme(shootmePort,sendRinging,4000 + (500 *i), 4000 + (500 *i));
                sendRinging = true;
                SipProvider shootmeProvider = shootme.createProvider();
                shootmeProvider.addSipListener(shootme);
                this.shootme.add(shootme);
            }
            shootist = new Shootist(shootistPort, proxyPort, "on", true, targetPorts);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);    
    
            this.proxy = new Proxy(proxyPort,forkCount,targetPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");
            
            this.shootist.sendInvite(forkCount);
            
            
            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            shootist.checkState();
            int ackCount = 0;
            for ( Shootme shootme: this.shootme) {
                 shootme.checkState();
                 if ( shootme.isAckSeen()) {
                     ackCount ++;
                 }
            }
            assertEquals("ACK count must be exactly 2", 2,ackCount);
        } finally {
            this.shootist.stop();
            for ( Shootme shootme: this.shootme) {
                shootme.stop();
            }
            this.proxy.stop();
        }
    }

    public void testInviteAutomaticDialogNonEnabled() throws Exception {
    	int forkCount = 2;
        try {
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();

            boolean sendRinging = true;
            int[] targetPorts = new int[forkCount];
            for  (int i = 0 ; i <  forkCount ; i ++ ) {
                int shootmePort = NetworkPortAssigner.retrieveNextPort();
                targetPorts[i] = shootmePort;
                Shootme shootme = new Shootme(shootmePort,sendRinging, 4000 + (100 *i), 4000 + (100 *i));
                sendRinging = true;
                SipProvider shootmeProvider = shootme.createProvider();
                shootmeProvider.addSipListener(shootme);
                this.shootme.add(shootme);
            }
            shootist = new Shootist(shootistPort, proxyPort, "off", true,targetPorts);        
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);
            
            this.proxy = new Proxy(proxyPort,forkCount, targetPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");
            
            this.shootist.sendInvite(forkCount);
            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            this.shootist.checkState();
            int ackCount = 0;
            for ( Shootme shootme: this.shootme) {
                 shootme.checkState();
                 if ( shootme.isAckSeen()) {
                     ackCount ++;
                 }
            }
            assertEquals("ACK count must be exactly 2", 2,ackCount);
        } finally {
            this.shootist.stop();
            for ( Shootme shootme: this.shootme) {
                shootme.stop();
            }
            this.proxy.stop();
        }
    }
    
    public void testInviteAutomaticDialogNonEnabledForkSecond() throws Exception {
    	int forkCount = 2;
        try {
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();             

            boolean sendRinging = true;
            int[] targetPorts = new int[forkCount];
            for  (int i = 0 ; i <  forkCount ; i ++ ) {
                int shootmePort = NetworkPortAssigner.retrieveNextPort();
                targetPorts[i]=shootmePort;
                Shootme shootme = new Shootme(shootmePort,sendRinging, 4000 - (500 *i), 4000 - (500 *i));
                sendRinging = true;
                SipProvider shootmeProvider = shootme.createProvider();
                shootmeProvider.addSipListener(shootme);
                this.shootme.add(shootme);
            }
            shootist = new Shootist(shootistPort, proxyPort, "off", false,targetPorts);        
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);
            
            this.proxy = new Proxy(proxyPort,forkCount,targetPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");
            
            this.shootist.sendInvite(forkCount);
            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            this.shootist.checkState();
            int ackCount = 0;
            for ( Shootme shootme: this.shootme) {
                 shootme.checkState();
                 if ( shootme.isAckSeen()) {
                     ackCount ++;
                 }
            }
            assertEquals("ACK count must be exactly 2", 2,ackCount);
        } finally {
            this.shootist.stop();
            for ( Shootme shootme: this.shootme) {
                shootme.stop();
            }
            this.proxy.stop();
        }
    }
    
    public void testInviteAutomaticDialogNonEnabledOKFromSecondForkFirst() throws Exception {
    	int forkCount = 2;
        try {
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();             

            boolean sendRinging = true;
            int[] targetPorts = new int[forkCount];
            for  (int i = 0 ; i <  forkCount ; i ++ ) {
                int shootmePort = NetworkPortAssigner.retrieveNextPort();
                targetPorts[i]=shootmePort;
                Shootme shootme = new Shootme(shootmePort,sendRinging, 4000 + (100 *i), 4000 - (100 *i));
                sendRinging = true;
                SipProvider shootmeProvider = shootme.createProvider();
                shootmeProvider.addSipListener(shootme);
                this.shootme.add(shootme);
            }
            shootist = new Shootist(shootistPort, proxyPort, "off", true,targetPorts);        
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);            
            
            this.proxy = new Proxy(proxyPort,forkCount, targetPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");
            
            this.shootist.sendInvite(forkCount);
            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            this.shootist.checkState();
            int ackCount = 0;
            for ( Shootme shootme: this.shootme) {
                 shootme.checkState();
                 if ( shootme.isAckSeen()) {
                     ackCount ++;
                 }
            }
            assertEquals("ACK count must be exactly 2", 2,ackCount);
        } finally {
            this.shootist.stop();
            for ( Shootme shootme: this.shootme) {
                shootme.stop();
            }
            this.proxy.stop();
        }
    }
    
    /**
     * Checking if when the flag is not enabled and a 200 ok response comes before
     * the app code has called createNewDialog doesn't create a dialog 
     */
    public void testAutomaticDialogNonEnabledRaceCondition() throws Exception {
    	int forkCount = 2;
        try {
            int shootistPort = NetworkPortAssigner.retrieveNextPort();
            int proxyPort = NetworkPortAssigner.retrieveNextPort();             

            boolean sendRinging = true;
            forkCount = 1;
            int[] targetPorts = new int[forkCount];
            for  (int i = 0 ; i <  forkCount ; i ++ ) {
                int shootmePort = NetworkPortAssigner.retrieveNextPort();
                targetPorts[i] = shootmePort;
                Shootme shootme = new Shootme(shootmePort,sendRinging, 4000 + (500 *i), 4000 + (500 *i));
                sendRinging = true;
                SipProvider shootmeProvider = shootme.createProvider();
                shootmeProvider.addSipListener(shootme);
                this.shootme.add(shootme);
            }
            shootist = new Shootist(shootistPort, proxyPort, "off", false,targetPorts); 
            shootist.setCreateDialogAfterRequest(true);
            SipProvider shootistProvider = shootist.createSipProvider();
            shootistProvider.addSipListener(shootist);
            
            this.proxy = new Proxy(proxyPort,forkCount, targetPorts);
            SipProvider provider = proxy.createSipProvider();
            provider.addSipListener(proxy);
            logger.debug("setup completed");
            
            this.shootist.sendInvite(0);
            AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT);
            this.shootist.checkState();
            int ackCount = 0;
            for ( Shootme shootme: this.shootme) {
                 if ( shootme.isAckSeen()) {
                     ackCount ++;
                 }
            }
            assertEquals("ACK count must be exactly 0", 0,ackCount);
        } finally {
            this.shootist.stop();
            for ( Shootme shootme: this.shootme) {
                shootme.stop();
            }
            this.proxy.stop();
        }
    }

    
}
