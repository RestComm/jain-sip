package test.unit.gov.nist.javax.sip.stack.forkedinvite482;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.header.ContactHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import test.tck.TestHarness;
import test.tck.msgflow.callflows.ProtocolObjects;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */

public class Shootme   implements SipListener {

    private static final Logger LOG = LogManager.getLogger(Shootme.class);

    private static final String myAddress = "127.0.0.1";

    private Hashtable serverTxTable = new Hashtable();

    private SipProvider sipProvider;

    private int myPort ;

    private static String unexpectedException = "Unexpected exception ";

    private ProtocolObjects protocolObjects;

    private boolean inviteSeen;

    private boolean byeSeen;

    private boolean ackSeen;

    private boolean actAsNonRFC3261UAS;

    /**
     * Causes this UAS to act as a non-RFC3261 UAS, i.e. does not set a to-tag
     */
    public void setNonRFC3261( boolean b ) {
        this.actAsNonRFC3261UAS = b;
    }

    class MyTimerTask extends TimerTask {
        RequestEvent  requestEvent;
        // String toTag;
        ServerTransaction serverTx;

        public MyTimerTask(RequestEvent requestEvent,ServerTransaction tx) {
            LOG.info("MyTimerTask ");
            this.requestEvent = requestEvent;
            // this.toTag = toTag;
            this.serverTx = tx;

        }

        public void run() {
            sendInviteOK(requestEvent,serverTx);
        }

    }



    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        LOG.info("\n\nRequest " + request.getMethod()
                + " received at " + protocolObjects.sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.CANCEL)) {
            processCancel(requestEvent, serverTransactionId);
        }

    }

    public void processResponse(ResponseEvent responseEvent) {
    }

    /**
     * Process the ACK request. Send the bye and complete the call flow.
     */
    public void processAck(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        LOG.info("shootme: got an ACK! ");
        LOG.info("Dialog = " + requestEvent.getDialog());
        if ( requestEvent.getDialog() != null ) {
            LOG.info("Dialog State = " + requestEvent.getDialog().getState());
        } else {
            LOG.debug("No dialog !!");
        }

        this.ackSeen = true;
    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            LOG.info("shootme: got an Invite sending Trying");
            // LOG.info("shootme: " + request);

            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                LOG.info("null server tx -- getting a new one");
                st = sipProvider.getNewServerTransaction(request);
            }

            LOG.info("getNewServerTransaction : " + st);

            String txId = ((ViaHeader)request.getHeader(ViaHeader.NAME)).getBranch();
            this.serverTxTable.put(txId, st);

            // Create the 100 Trying response.
            Response response = protocolObjects.messageFactory.createResponse(Response.TRYING,
                    request);
                ListeningPoint lp = sipProvider.getListeningPoint(protocolObjects.transport);
            int myPort = lp.getPort();

            Address address = protocolObjects.addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");

            // Add a random sleep to stagger the two OK's for the benifit of implementations
            // that may not be too good about handling re-entrancy.
            int timeToSleep = (int) ( Math.random() * 1000);

            Thread.sleep(timeToSleep);

            st.sendResponse(response);

            Response ringingResponse = protocolObjects.messageFactory.createResponse(Response.RINGING,
                    request);
            ContactHeader contactHeader = protocolObjects.headerFactory.createContactHeader(address);
            response.addHeader(contactHeader);
            ToHeader toHeader = (ToHeader) ringingResponse.getHeader(ToHeader.NAME);
            String toTag = actAsNonRFC3261UAS ? null : new Integer((int) (Math.random() * 10000)).toString();
            if (!actAsNonRFC3261UAS) toHeader.setTag(toTag); // Application is supposed to set.
            ringingResponse.addHeader(contactHeader);
            st.sendResponse(ringingResponse);
            Dialog dialog =  st.getDialog();
            dialog.setApplicationData(st);

            this.inviteSeen = true;

            new Timer().schedule(new MyTimerTask(requestEvent,st/*,toTag*/), 1000);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void sendInviteOK(RequestEvent requestEvent, ServerTransaction inviteTid) {
        try {
            LOG.info("sendInviteOK: " + inviteTid);
            if (inviteTid.getState() != TransactionState.COMPLETED) {
                LOG.info("shootme: Dialog state before OK: "
                        + inviteTid.getDialog().getState());

                SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                Request request = requestEvent.getRequest();
                Response okResponse = protocolObjects.messageFactory.createResponse(Response.OK,
                        request);
                    ListeningPoint lp = sipProvider.getListeningPoint(protocolObjects.transport);
                int myPort = lp.getPort();

                Address address = protocolObjects.addressFactory.createAddress("Shootme <sip:"
                        + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = protocolObjects.headerFactory
                        .createContactHeader(address);
                okResponse.addHeader(contactHeader);
                inviteTid.sendResponse(okResponse);
                LOG.info("shootme: Dialog state after OK: "
                        + inviteTid.getDialog().getState());
                TestHarness.assertEquals( DialogState.CONFIRMED , inviteTid.getDialog().getState() );
            } else {
                LOG.info("semdInviteOK: inviteTid = " + inviteTid + " state = " + inviteTid.getState());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {
        Request request = requestEvent.getRequest();
        try {
            LOG.info("shootme:  got a bye sending OK.");
            LOG.info("shootme:  dialog = " + requestEvent.getDialog());
            LOG.info("shootme:  dialogState = " + requestEvent.getDialog().getState());
            Response response = protocolObjects.messageFactory.createResponse(200, request);
            if ( serverTransactionId != null) {
                serverTransactionId.sendResponse(response);
            }
            LOG.info("shootme:  dialogState = " + requestEvent.getDialog().getState());

            this.byeSeen = true;


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);

        }
    }

    public void processCancel(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {
        Request request = requestEvent.getRequest();
        SipProvider sipProvider = (SipProvider)requestEvent.getSource();
        try {
            LOG.info("shootme:  got a cancel. " );
            // Because this is not an In-dialog request, you will get a null server Tx id here.
            if (serverTransactionId == null) {
                serverTransactionId = sipProvider.getNewServerTransaction(request);
            }
            Response response = protocolObjects.messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);

            String serverTxId = ((ViaHeader)response.getHeader(ViaHeader.NAME)).getBranch();
            ServerTransaction serverTx = (ServerTransaction) this.serverTxTable.get(serverTxId);
            if ( serverTx != null && (serverTx.getState().equals(TransactionState.TRYING) ||
                    serverTx.getState().equals(TransactionState.PROCEEDING))) {
                Request originalRequest = serverTx.getRequest();
                Response resp = protocolObjects.messageFactory.createResponse(Response.REQUEST_TERMINATED,originalRequest);
                serverTx.sendResponse(resp);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);

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

    public SipProvider createProvider() {
        try {

            ListeningPoint lp = protocolObjects.sipStack.createListeningPoint(myAddress,
                    myPort, protocolObjects.transport);

            sipProvider = protocolObjects.sipStack.createSipProvider(lp);
            LOG.info("provider " + sipProvider);
            LOG.info("sipStack = " + protocolObjects.sipStack);
            sipProvider.setAutomaticDialogSupportEnabled(true);
            return sipProvider;
        } catch (Exception ex) {
            LOG.error(ex);
            TestHarness.fail(unexpectedException);
            return null;

        }

    }

    public Shootme( int myPort, ProtocolObjects protocolObjects ) {
        this.myPort = myPort;
        this.protocolObjects = protocolObjects;
    }



    public void processIOException(IOExceptionEvent exceptionEvent) {
        LOG.info("IOException");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        LOG.info("Transaction terminated event recieved");

    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        LOG.info("Dialog terminated event recieved");

    }

    public void checkState() {
        TestHarness.assertTrue("Should see invite", inviteSeen);

    }

}
