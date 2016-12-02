package test.unit.gov.nist.javax.sip.stack.subsnotify;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.MessageExt;
import gov.nist.javax.sip.message.ResponseExt;
import gov.nist.javax.sip.stack.NioMessageProcessorFactory;
import java.util.ArrayList;
import java.util.Properties;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
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
import javax.sip.Transaction;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a UAC template. Shootist is the guy that shoots and notifier is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */

public class Notifier implements SipListener {

    private static final Logger LOG = LogManager.getLogger(Notifier.class);

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static SipStack sipStack;

    private static String toTag;

    private int port;

    protected SipProvider udpProvider;

    protected int notifyCount = 0;

    private boolean handleSubscribe = true;

    protected static final String usageString = "java "
            + "examples.shootist.Shootist \n"
            + ">>>> is your class path set to the root?";

    private static void usage() {
        LOG.info(usageString);
        System.exit(0);
    }

    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        LOG.info("\n\nRequest " + request.getMethod()
                         + " received at " + sipStack.getStackName()
                         + " with server transaction id " + serverTransactionId
                         + " and dialog id " + requestEvent.getDialog());

        if (request.getMethod().equals(Request.SUBSCRIBE)) {
            processSubscribe(requestEvent, serverTransactionId);
        }
    }

    /**
     * Process the invite request.
     */
    public void processSubscribe(
            RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            LOG.info("notifier:  " + request);
            LOG.info("notifier : dialog = " + requestEvent.getDialog());
            if (handleSubscribe) {
                EventHeader eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
                if (eventHeader == null) {
                    LOG.info("Cannot find event header.... dropping request.");
                    return;
                }

                // Always create a ServerTransaction, best as early as possible in the code
                Response response = null;
                ServerTransaction st = requestEvent.getServerTransaction();
                if (st == null) {
                    st = sipProvider.getNewServerTransaction(request);
                }
                response = messageFactory.createResponse(202, request);
                ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                // Check if it is an initial SUBSCRIBE or a refresh / unsubscribe
                if (((MessageExt) request).getToHeader().getTag() == null) {
                    toTag = Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE));
                    toHeader.setTag(toTag);
                    // Sanity check: to header should not ahve a tag. Else the dialog
                }

                // Both 2xx response to SUBSCRIBE and NOTIFY need a Contact
                Address address = addressFactory.createAddress("Notifier <sip:127.0.0.1>");
                ((SipURI) address.getURI()).setPort(udpProvider.getListeningPoint("udp").getPort());
                ContactHeader contactHeader = headerFactory.createContactHeader(address);
                response.addHeader(contactHeader);

                // Expires header is mandatory in 2xx responses to SUBSCRIBE
                ExpiresHeader expires = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
                if (expires == null) {
                    expires = headerFactory.createExpiresHeader(30);// rather short
                }
                response.addHeader(expires);
    
             
                /*
                 * NOTIFY requests MUST contain a "Subscription-State" header with a
                 * value of "active", "pending", or "terminated". The "active" value
                 * indicates that the subscription has been accepted and has been
                 * authorized (in most cases; see section 5.2.). The "pending" value
                 * indicates that the subscription has been received, but that
                 * policy information is insufficient to accept or deny the
                 * subscription at this time. The "terminated" value indicates that
                 * the subscription is not active.
                 */

                Dialog dialog = sipProvider.getNewDialog(st);

                Address toAddress = ((ResponseExt) response).getFromHeader().getAddress();
                String toTag = ((ResponseExt) response).getFromHeader().getTag();
                Address fromAddress = ((ResponseExt) response).getToHeader().getAddress();
                String fromTag = ((ResponseExt) response).getToHeader().getTag();
                FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, fromTag);
                toHeader = headerFactory.createToHeader(toAddress, toTag);
                // Create a new Cseq header
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(
                        1L,
                        Request.NOTIFY);
                ArrayList viaHeaders = new ArrayList();
                String transport = "udp";
                int port = sipProvider.getListeningPoint(transport).getPort();
                ViaHeader viaHeader = headerFactory.createViaHeader("127.0.0.1",
                                                                    port, transport, null);

                // add via headers
                viaHeaders.add(viaHeader);

                MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
                SipURI requestURI = addressFactory.createSipURI(null, "127.0.0.1");
                requestURI.setPort(5060);

                CallIdHeader callIdHeader = ((ResponseExt) response).getCallIdHeader();

                // Create the request.
                Request notifyRequest = messageFactory.createRequest(requestURI,
                                                                     Request.NOTIFY, callIdHeader, cSeqHeader, fromHeader,
                                                                     toHeader, viaHeaders, maxForwards);

                // Mark the contact header, to check that the remote contact is updated
                ((SipURI) contactHeader.getAddress().getURI()).setParameter("id", "not");

                // Initial state is pending, second time we assume terminated (Expires==0)
                SubscriptionStateHeader sstate = headerFactory.createSubscriptionStateHeader(
                        SubscriptionStateHeader.PENDING);
                if (expires.getExpires() == 0) {
                    sstate = headerFactory.createSubscriptionStateHeader(
                            SubscriptionStateHeader.TERMINATED);
                }

                // Need a reason for terminated
                if (sstate.getState().equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)) {
                    sstate.setReasonCode("deactivated");
                }

                notifyRequest.addHeader(sstate);
                notifyRequest.setHeader(eventHeader);
                notifyRequest.setHeader(contactHeader);
                // notifyRequest.setHeader(routeHeader);
                ClientTransaction ct = udpProvider.getNewClientTransaction(notifyRequest);

                ct.sendRequest();
                LOG.info("NOTIFY Branch ID " +
                                 ((ViaHeader) request.getHeader(ViaHeader.NAME)).getParameter("branch"));
                LOG.info("notifier: got an Subscribe sending OK " + response);

                Thread.sleep(1000);

                st.sendResponse(response);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            // System.exit(0);
        }
    }

    public synchronized void processResponse(ResponseEvent responseReceivedEvent) {
        Response response = (Response) responseReceivedEvent.getResponse();
        Transaction tid = responseReceivedEvent.getClientTransaction();

        if (tid == null) {
            TestCase.assertTrue("retrans flag should be true", ((ResponseEventExt) responseReceivedEvent).isRetransmission());
        } else {
            TestCase.assertFalse("retrans flag should be false", ((ResponseEventExt) responseReceivedEvent).isRetransmission());
        }

        if (response.getStatusCode() != 200) {
            this.notifyCount--;
        } else {
            System.out.println("Notify Count = " + this.notifyCount);
        }
    }

    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }
        LOG.info("state = " + transaction.getState());
        LOG.info("dialog = " + transaction.getDialog());
        LOG.info("dialogState = "
                         + transaction.getDialog().getState());
        LOG.info("Transaction Time out");
    }

    private static void initFactories(int port) throws Exception {

        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();

        properties.setProperty("javax.sip.STACK_NAME", "notifier" + port);
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty(
                "gov.nist.javax.sip.DEBUG_LOG",
                "logs/notifierdebug_" + port + ".txt");
        properties.setProperty(
                "gov.nist.javax.sip.SERVER_LOG",
                "logs/notifierlog_" + port + ".txt");

        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
        if (System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
            LOG.info("\nNIO Enabled\n");
            properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
        }
        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            LOG.info("sipStack = " + sipStack);
        } catch (PeerUnavailableException e) {
            // could not find
            // gov.nist.jain.protocol.ip.sip.SipStackImpl
            // in the classpath
            e.printStackTrace();
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            System.exit(0);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public void createProvider() {

        try {

            ListeningPoint lp = sipStack.createListeningPoint("127.0.0.1",
                                                              this.port, "udp");

            this.udpProvider = sipStack.createSipProvider(lp);
            LOG.info("udp provider " + udpProvider);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
            ex.printStackTrace();
            usage();
        }
    }

    public Notifier(int port) {
        this.port = port;
    }


    public static Notifier createNotifier() throws Exception {
        int port = 5070;
        initFactories(port);
        Notifier notifier = new Notifier(port);
        notifier.createProvider();
        notifier.udpProvider.addSipListener(notifier);
        return notifier;
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {

        NotifyBefore202Test.fail("Unexpected IO Exception");
    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        // TODO Auto-generated method stub

    }

    public void tearDown() {
        this.sipStack.stop();
    }

    public void setHandleSubscribe(boolean b) {
        handleSubscribe = b;
    }
}
