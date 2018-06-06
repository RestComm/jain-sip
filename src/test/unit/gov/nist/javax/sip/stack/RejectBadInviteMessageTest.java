package test.unit.gov.nist.javax.sip.stack;

import java.text.ParseException;
import java.util.Properties;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ReasonHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.javax.sip.stack.NioMessageProcessorFactory;
import junit.framework.TestCase;

public class RejectBadInviteMessageTest extends TestCase {

    public class Shootist implements SipListener {
        private SipStack sipStack;

        private SipProvider sipProvider;

        private AddressFactory addressFactory;

        private MessageFactory messageFactory;

        private HeaderFactory headerFactory;

        private ListeningPoint udpListeningPoint;

        private static final String myAddress = "127.0.0.1";

        private static final int myPort = 5070;

        private boolean sawTransactionUnavailableException = false;

        private boolean sawBadRequestResponse = false;

        @Override
        public void processRequest(RequestEvent requestEvent) {
            Request request = requestEvent.getRequest();
            ServerTransaction serverTransactionId = requestEvent.getServerTransaction();

            System.out.println("Request " + request.getMethod() + " received at " + sipStack.getStackName()
                    + " with server transaction id " + serverTransactionId);

            if (request.getMethod().equals(Request.INVITE)) {
                processInvite(requestEvent);
            }
        }

        public void processInvite(RequestEvent requestEvent) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            try {

                Response okResponse = messageFactory.createResponse(Response.OK, request);
                Address address = addressFactory.createAddress("Shootme <sip:" + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = headerFactory.createContactHeader(address);
                ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
                toHeader.setTag("4321"); // Application is supposed to set.
                okResponse.addHeader(contactHeader);
                ServerTransaction stx = requestEvent.getServerTransaction();
                if (stx == null) {
                    stx = sipProvider.getNewServerTransaction(request);
                    stx.sendResponse(okResponse);
                }
            } catch (TransactionUnavailableException ex) {
                this.sawTransactionUnavailableException = true;
                System.out.println("Saw TransactionUnavailableException");
            } catch (Exception ex) {
                System.err.println("Unexpected exception" + ex);
                fail("Unexpected Exception seen");
            }
        }

        @Override
        public void processResponse(ResponseEvent responseEvent) {
            Response response = responseEvent.getResponse();
            System.out.println("Response " + response.getStatusCode() + " received");

            if (response.getStatusCode() == Response.BAD_REQUEST) {
                this.sawBadRequestResponse = true;
                System.out.println("ReasonPhrase is " + response.getReasonPhrase());
                ReasonHeader reason = (ReasonHeader) response.getHeader(ReasonHeader.NAME);
                if (reason != null) {
                    System.out.println("ReasonHeader cause=" + reason.getCause() + ", text=" + reason.getText());
                } else {
                    System.out.println("ReasonHeader not available");
                }
            }
        }

        @Override
        public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
            System.out.println("dialogTerminatedEvent");
        }

        @Override
        public void processTimeout(TimeoutEvent timeoutEvent) {
            System.out.println("Got a timeout " + timeoutEvent.getClientTransaction());
        }

        @Override
        public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
            System.out.println("Transaction terminated event recieved");
        }

        @Override
        public void processIOException(IOExceptionEvent exceptionEvent) {
            System.out.println(
                    "IOException happened for " + exceptionEvent.getHost() + " port = " + exceptionEvent.getPort());
        }

        public void init() {
            SipFactory sipFactory = null;
            sipStack = null;
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            Properties properties = new Properties();
            // If you want to try TCP transport change the following to
            String transport = "udp";
            String peerHostPort = myAddress + ':' + myPort;
            properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/" + transport);
            // If you want to use UDP then uncomment this.
            properties.setProperty("javax.sip.STACK_NAME", "shootist");

            // The following properties are specific to nist-sip
            // and are not necessarily part of any other jain-sip
            // implementation.
            // You can set a max message size for tcp transport to
            // guard against denial of service attack.
            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "shootistdebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "shootistlog.txt");

            // Drop the client connection after we are done with the
            // transaction.
            properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
            // Set to 0 (or NONE) in your production code for max speed.
            // You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for
            // debug + traces.
            // Your code will limp at 32 but it is best for debugging.
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
            if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY",
                        NioMessageProcessorFactory.class.getName());
            }
            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);
                System.out.println("createSipStack " + sipStack);
            } catch (PeerUnavailableException e) {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                fail("Problem with setup");
            }

            try {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                udpListeningPoint = sipStack.createListeningPoint(myAddress, myPort, "udp");
                sipProvider = sipStack.createSipProvider(udpListeningPoint);
                Shootist listener = this;
                sipProvider.addSipListener(listener);

                String badRequest = "INVITE sip:LittleGuy@127.0.0.1:5070 SIP/2.0\r\n"
                        + "Call-ID: 7a3cd620346e3fa199cb397fe6b6fc16@127.0.0.1\r\n"
                        + "CSeq: 1 INVITE\r\n"
                        + "From: \"The Master Blaster\" <sip:BigGuy@here.com>;tag=12345\r\n"
                        + "To: \"The Little Blister\" <sip:LittleGuy@there.com>\r\n"
                        + "Via: SIP/2.0/UDP 127.0.0.1:5070 ;branch=z9hG4bKba9abfef1063941840d955a5e3e7dae0393734\r\n"
                        + "Max-Forwards: 70\r\n"
                        + "Content-Length: 0\r\n\r\n";

                System.out.println("Parsing message \n" + badRequest);
                Request request = null;
                try {
                    request = messageFactory.createRequest(badRequest);
                } catch (ParseException ex) {
                    System.out.println("Unexpected exception " + ex);
                    fail("Unexpected exception");
                }

                // send the request out.
                sipProvider.sendRequest(request);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("cannot create or send initial invite");
            }
        }

        public void terminate() {
            this.sipStack.stop();
        }
    }

    private Shootist shootist;

    @Override
    public void setUp() {
        this.shootist = new Shootist();
    }

    @Override
    public void tearDown() {
        shootist.terminate();
    }

    public void testSendInviteWithoutContactHeader() {
        this.shootist.init();
        try {
            Thread.sleep(500);
        } catch (Exception ex) {
        }

        assertTrue("Should see TransactionUnavailableException", shootist.sawTransactionUnavailableException);
        assertTrue("Should see 400 Bad Request response", shootist.sawBadRequestResponse);
    }
}
