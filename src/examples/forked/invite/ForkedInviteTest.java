/**
 *
 */
package examples.forked.invite;

import java.util.EventObject;
import java.util.Hashtable;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author M. Ranganathan
 *
 */
public class ForkedInviteTest extends TestCase implements SipListener {

    private static final Logger LOG = LogManager.getLogger(ForkedInviteTest.class);

    private Hashtable providerTable;

    protected Shootist shootist;

    private SipListener getSipListener(EventObject sipEvent) {
        SipProvider source = (SipProvider) sipEvent.getSource();
        SipListener listener = (SipListener) providerTable.get(source);
        assertTrue(listener != null);
        return listener;
    }

    public ForkedInviteTest() {

        try {
            ProtocolObjects.logFileDirectory = "logs/";
            ProtocolObjects.init("frokedinvite",true);
            providerTable = new Hashtable();
            shootist = new Shootist();
            SipProvider shootistProvider = shootist.createSipProvider();
            providerTable.put(shootistProvider, shootist);

            Shootme shootme = new Shootme(5080);
            SipProvider shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);

            shootme = new Shootme(5090);
            shootmeProvider = shootme.createProvider();
            providerTable.put(shootmeProvider, shootme);
            shootistProvider.addSipListener(this);
            shootmeProvider.addSipListener(this);


            Proxy proxy = new Proxy();
            SipProvider provider = proxy.createSipProvider();
            provider.setAutomaticDialogSupportEnabled(false);
            providerTable.put(provider, proxy);
            provider.addSipListener(this);


            ProtocolObjects.start();
        } catch (Exception ex) {
            fail("unexpected exception ");
        }
    }

    public void testSendForkedInvite() {
        try {
        this.shootist.sendInvite();
        Thread.sleep(20000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setUp() {

            try {
                //Runtime.getRuntime().exec("java examples.forked.invite.Proxy");
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected error initializing logging",
                        ex);
            }


    }

    public void tearDown() {
        try {
            ProtocolObjects.destroy();
            //LOG.removeAppender(appender);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }





    public void processRequest(RequestEvent requestEvent) {
        getSipListener(requestEvent).processRequest(requestEvent);

    }

    public void processResponse(ResponseEvent responseEvent) {
        getSipListener(responseEvent).processResponse(responseEvent);

    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        getSipListener(timeoutEvent).processTimeout(timeoutEvent);
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        fail("unexpected exception");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        getSipListener(transactionTerminatedEvent)
                .processTransactionTerminated(transactionTerminatedEvent);

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        getSipListener(dialogTerminatedEvent).processDialogTerminated(
                dialogTerminatedEvent);

    }

}
