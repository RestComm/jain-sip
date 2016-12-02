package examples.forked.invite;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import javax.sip.Dialog;
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
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */

public class Shootme  extends TestCase implements SipListener {



    private static final Logger LOG = LogManager.getLogger(Shootme.class);

    private static String transport = "udp";

    private static final String myAddress = "127.0.0.1";

    private Hashtable serverTxTable = new Hashtable();

    private SipProvider sipProvider;

    private int myPort ;

    private static String unexpectedException = "Unexpected exception ";

    class MyTimerTask extends TimerTask {
        RequestEvent  requestEvent;
        String toTag;
        ServerTransaction serverTx;

        public MyTimerTask(RequestEvent requestEvent,ServerTransaction tx, String toTag) {
            LOG.info("MyTimerTask ");
            this.requestEvent = requestEvent;
            this.toTag = toTag;
            this.serverTx = tx;

        }

        public void run() {
            sendInviteOK(requestEvent,serverTx,toTag);
        }

    }

    protected static final String usageString = "java "
            + "examples.shootist.Shootist \n"
            + ">>>> is your class path set to the root?";



    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        LOG.info("\n\nRequest " + request.getMethod()
                + " received at shootme "
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
        LOG.info("processResponse " + responseEvent.getClientTransaction() +
                "\nresponse = " + responseEvent.getResponse());
    }

    /**
     * Process the ACK request. Send the bye and complete the call flow.
     */
    public void processAck(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        LOG.info("shootme: got an ACK! ");
        LOG.info("Dialog = " + requestEvent.getDialog());
        LOG.info("Dialog State = " + requestEvent.getDialog().getState());
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
            Response response = ProtocolObjects.messageFactory.createResponse(Response.TRYING,
                    request);
                ListeningPoint lp = sipProvider.getListeningPoint("udp");
            int myPort = lp.getPort();

            Address address = ProtocolObjects.addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");

            st.sendResponse(response);

            Response ringingResponse = ProtocolObjects.messageFactory.createResponse(Response.RINGING,
                    request);
            ContactHeader contactHeader = ProtocolObjects.headerFactory.createContactHeader(address);
            response.addHeader(contactHeader);
            ToHeader toHeader = (ToHeader) ringingResponse.getHeader(ToHeader.NAME);
            String toTag = new Integer((int) (Math.random() * 10000)).toString();
            toHeader.setTag(toTag); // Application is supposed to set.
            ringingResponse.addHeader(contactHeader);
            st.sendResponse(ringingResponse);
            Dialog dialog =  st.getDialog();
            dialog.setApplicationData(st);



            new Timer().schedule(new MyTimerTask(requestEvent,st,toTag), 100);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void sendInviteOK(RequestEvent requestEvent, ServerTransaction inviteTid, String toTag) {
        try {
            LOG.info("sendInviteOK: " + inviteTid);
            if (inviteTid.getState() != TransactionState.COMPLETED) {
                LOG.info("shootme: Dialog state before OK: "
                        + inviteTid.getDialog().getState());

                SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                Request request = requestEvent.getRequest();
                Response okResponse = ProtocolObjects.messageFactory.createResponse(Response.OK,
                        request);
                    ListeningPoint lp = sipProvider.getListeningPoint("udp");
                int myPort = lp.getPort();

                Address address = ProtocolObjects.addressFactory.createAddress("Shootme <sip:"
                        + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = ProtocolObjects.headerFactory
                        .createContactHeader(address);
                okResponse.addHeader(contactHeader);
                inviteTid.sendResponse(okResponse);
                LOG.info("shootme: Dialog state after OK: "
                        + inviteTid.getDialog().getState());
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
            Response response = ProtocolObjects.messageFactory.createResponse(200, request);
            if ( serverTransactionId != null) {
                serverTransactionId.sendResponse(response);
            }
            LOG.info("shootme:  dialogState = " + requestEvent.getDialog().getState());


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
            Response response = ProtocolObjects.messageFactory.createResponse(200, request);
            serverTransactionId.sendResponse(response);

            String serverTxId = ((ViaHeader)response.getHeader(ViaHeader.NAME)).getBranch();
            ServerTransaction serverTx = (ServerTransaction) this.serverTxTable.get(serverTxId);
            if ( serverTx != null && (serverTx.getState().equals(TransactionState.TRYING) ||
                    serverTx.getState().equals(TransactionState.PROCEEDING))) {
                Request originalRequest = serverTx.getRequest();
                Response resp = ProtocolObjects.messageFactory.createResponse(Response.REQUEST_TERMINATED,originalRequest);
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

            ListeningPoint lp = ProtocolObjects.sipStack.createListeningPoint(myAddress,
                    myPort, transport);

            sipProvider = ProtocolObjects.sipStack.createSipProvider(lp);
            LOG.info("udp provider " + sipProvider);
            return sipProvider;
        } catch (Exception ex) {
            LOG.error(ex);
            fail(unexpectedException);
            return null;

        }

    }

    public Shootme( int myPort) {
        this.myPort = myPort;
    }

    public static void main(String args[]) throws Exception {
        int myPort = new Integer(args[0]).intValue();

        ProtocolObjects.init("shootme_"+myPort,true);
        Shootme shootme = new Shootme(myPort);
        shootme.createProvider();
        shootme.sipProvider.addSipListener(shootme);
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

}
