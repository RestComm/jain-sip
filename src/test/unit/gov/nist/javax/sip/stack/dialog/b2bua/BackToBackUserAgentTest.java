package test.unit.gov.nist.javax.sip.stack.dialog.b2bua;

import junit.framework.TestCase;
import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;

public class BackToBackUserAgentTest extends TestCase {
    
    private Shootist shootist;
    private BackToBackUserAgent b2bua;
    private Shootme shootme;
    private static final int TIMEOUT = 3000;

    @Override 
    public void setUp() throws Exception {
        int shootistPort = NetworkPortAssigner.retrieveNextPort();
        int b2bPort = NetworkPortAssigner.retrieveNextPort();
        int b2bPort2 = NetworkPortAssigner.retrieveNextPort();        
        int shootmePort = NetworkPortAssigner.retrieveNextPort();        
        this.shootist = new Shootist(shootistPort,b2bPort);
        this.b2bua  = new BackToBackUserAgent(b2bPort,b2bPort2);
        b2bua.setTargetPort(shootmePort);
        this.shootme = new Shootme(shootmePort,true,100);
        
    }
    
    public void testSendInvite() {
        this.shootist.sendInvite();
    }
    
    @Override 
    public void tearDown() throws Exception {
        assertTrue(
                "Should see BYE response for ACKED Dialog"
                + " and InviteOK seen",
                AssertUntil.assertUntil(shootist.getAssertion(), TIMEOUT));
        assertTrue(
                "Should see invite"
                + " and Should see BYE",
                AssertUntil.assertUntil(shootme.getAssertion(), TIMEOUT));
        
        super.tearDown();
    }
    

}
