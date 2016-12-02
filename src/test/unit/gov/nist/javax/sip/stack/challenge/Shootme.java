/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), and others.
 * This software is has been contributed to the public domain.
 * As a result, a formal license is not needed to use the software.
 *
 * This software is provided "AS IS."
 * NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 *
 */
package test.unit.gov.nist.javax.sip.stack.challenge;

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
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import test.tck.msgflow.callflows.ProtocolObjects;

/**
 * This class is a UAC template.
 *
 * @author M. Ranganathan
 */

public class Shootme implements SipListener {

    private static final Logger LOG = LogManager.getLogger(Shootme.class);

    private ProtocolObjects protocolObjects;

    // To run on two machines change these to suit.
    public static final String myAddress = "127.0.0.1";

    public static final int myPort = 5070;

    private Dialog dialog;

    private boolean challenged, challengedBye;

    class ApplicationData {
        protected int ackCount;
    }

    public Shootme(ProtocolObjects protocolObjects) {
        this.protocolObjects = protocolObjects;
    }

    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent
                .getServerTransaction();

        LOG.info("\n\nRequest " + request.getMethod() + " received at "
                + protocolObjects.sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestEvent, serverTransactionId);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestEvent, serverTransactionId);
        }

    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        LOG.info("Got an INVITE  " + request);
        try {

            // JvB: first time, challenge with 401 response (without proper
            // headers)
            if (!challenged) {
                challenged = true;
                ChallengeTest.assertNull(requestEvent.getServerTransaction());

                Response challenge = protocolObjects.messageFactory
                        .createResponse(401, request);
                ToHeader toHeader = (ToHeader) challenge
                        .getHeader(ToHeader.NAME);
                toHeader.setTag("challenge");
                sipProvider.sendResponse(challenge); // dont create ST
                return;
            }

            LOG.info("shootme: got an Invite sending OK");
            // LOG.info("shootme: " + request);
            Response response = protocolObjects.messageFactory.createResponse(
                    180, request);
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("4321");
            Address address = protocolObjects.addressFactory
                    .createAddress("Shootme <sip:" + myAddress + ":" + myPort
                            + ">");
            ContactHeader contactHeader = protocolObjects.headerFactory
                    .createContactHeader(address);
            response.addHeader(contactHeader);
            ServerTransaction st = requestEvent.getServerTransaction();

            if (st == null) {
                st = sipProvider.getNewServerTransaction(request);
                LOG.info("Server transaction created!" + request);

                LOG.info("Dialog = " + st.getDialog());
                if (st.getDialog().getApplicationData() == null) {
                    st.getDialog().setApplicationData(new ApplicationData());
                }
            } else {
                // If Server transaction is not null, then
                // this is a re-invite.
                LOG.info("This is a RE INVITE ");
                ChallengeTest.assertSame("Dialog mismatch ", st.getDialog(),
                        this.dialog);
            }

            // Thread.sleep(5000);
            LOG.info("got a server tranasaction " + st);
            byte[] content = request.getRawContent();
            if (content != null) {
                LOG.info(" content = " + new String(content));
                ContentTypeHeader contentTypeHeader = protocolObjects.headerFactory
                        .createContentTypeHeader("application", "sdp");
                LOG.info("response = " + response);
                response.setContent(content, contentTypeHeader);
            }
            dialog = st.getDialog();
            if (dialog != null) {
                LOG.info("Dialog " + dialog);
                LOG.info("Dialog state " + dialog.getState());
            }
            st.sendResponse(response);
            response = protocolObjects.messageFactory.createResponse(200,
                    request);
            toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("4321");
            // Application is supposed to set.
            response.addHeader(contactHeader);
            st.sendResponse(response);
            LOG.info("TxState after sendResponse = " + st.getState());
        } catch (Exception ex) {
            String s = "unexpected exception";

            LOG.error(s, ex);
            ChallengeTest.fail(s);
        }
    }

    /**
     * Process the ACK request.
     */
    private void processAck(RequestEvent r, ServerTransaction tid) {
        try {
            LOG.info("Got an ACK!");
        } catch (Exception ex) {
            LOG.error("unexpected exception", ex);
            ChallengeTest.fail("unexpected exception");

        }
    }

    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {

        // SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            // JvB: first time, challenge with 401 response (without proper
            // headers)
            if (!challengedBye) {
                challengedBye = true;
                LOG.info("Got a BYE! Challenging...");
                Response challenge = protocolObjects.messageFactory
                        .createResponse(401, request);
                serverTransactionId.sendResponse(challenge);
                return;
            }

            LOG.info("shootme:  got a bye sending OK.");
            Response response = protocolObjects.messageFactory.createResponse(
                    200, request);
            if (serverTransactionId != null) {
                serverTransactionId.sendResponse(response);
                LOG.info("Dialog State is "
                        + serverTransactionId.getDialog().getState());
            } else {
                LOG.info("null server tx.");
                // sipProvider.sendResponse(response);
            }

        } catch (Exception ex) {
            String s = "Unexpected exception";
            LOG.error(s, ex);
            ChallengeTest.fail(s);

        }
    }

    public void processResponse(ResponseEvent responseReceivedEvent) {
        LOG.info("Got a response");
        Response response = (Response) responseReceivedEvent.getResponse();
        Transaction tid = responseReceivedEvent.getClientTransaction();

        LOG.info("Response received with client transaction id " + tid
                + ":\n" + response);
        try {
            if (response.getStatusCode() == Response.OK
                    && ((CSeqHeader) response.getHeader(CSeqHeader.NAME))
                            .getMethod().equals(Request.INVITE)) {
                ChallengeTest.assertNotNull(
                        "INVITE 200 response should match a transaction", tid);
                Dialog dialog = tid.getDialog();
                CSeqHeader cseq = (CSeqHeader) response
                        .getHeader(CSeqHeader.NAME);
                Request request = dialog.createAck(cseq.getSeqNumber());
                dialog.sendAck(request);
            }
            if (tid != null) {
                Dialog dialog = tid.getDialog();
                LOG.info("Dalog State = " + dialog.getState());
            }
        } catch (Exception ex) {

            String s = "Unexpected exception";

            LOG.error(s, ex);
            ChallengeTest.fail(s);
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
        LOG.info("dialogState = " + transaction.getDialog().getState());
        LOG.info("Transaction Time out");
    }

    public SipProvider createSipProvider() throws Exception {
        ListeningPoint lp = protocolObjects.sipStack.createListeningPoint(
                myAddress, myPort, protocolObjects.transport);

        SipProvider sipProvider = protocolObjects.sipStack
                .createSipProvider(lp);
        return sipProvider;
    }

    public static void main(String args[]) throws Exception {
        ProtocolObjects protocolObjects = new ProtocolObjects("shootme",
                "gov.nist", "udp", true,false, false);

        Shootme shootme = new Shootme(protocolObjects);
        shootme.createSipProvider().addSipListener(shootme);

    }

    public void checkState() {
        ChallengeTest.assertTrue(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sip.SipListener#processIOException(javax.sip.IOExceptionEvent)
     */
    public void processIOException(IOExceptionEvent exceptionEvent) {
        LOG.error("An IO Exception was detected : "
                + exceptionEvent.getHost());

    }

    /*
     * (non-Javadoc)
     *
     * @seejavax.sip.SipListener#processTransactionTerminated(javax.sip.
     * TransactionTerminatedEvent)
     */
    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        LOG.info("Tx terminated event ");

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.sip.SipListener#processDialogTerminated(javax.sip.DialogTerminatedEvent
     * )
     */
    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        LOG.info("Dialog terminated event detected ");

    }

}
