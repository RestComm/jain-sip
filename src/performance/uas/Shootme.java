package performance.uas;

import gov.nist.javax.sip.message.RequestExt;
import java.io.File;
import java.io.FileInputStream;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

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
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;


/**
 * This is the UAS application for performance testing
 *
 * @author Vladimir Ralev
 */
public class Shootme implements SipListener {

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static SipStack sipStack;
    
    private static SipFactory sipFactory;
    
    private static SipProvider sipProvider; 
    
    private static Timer timer = new Timer();

    private static final String myAddress = "127.0.0.1";

    private static final int myPort = 5080;
    
    private static String TRANSPORT_TCP = "tcp";
    private static String TRANSPORT_UDP = "udp";

    protected static final String usageString = "java "
            + Shootme.class.getCanonicalName() + " \n"
            + ">>>> is your class path set to the root?";

	private static final long BYE_DELAY = 5000;

	private static final String TIMER_USER = "sipp-timer";

    private static void usage() {
        System.out.println(usageString);
        junit.framework.TestCase.fail("Exit JVM");
    }

    class ByeTask extends TimerTask {
        Dialog dialog;
        
        public ByeTask(Dialog dialog)  {
            this.dialog = dialog;
        }
        public void run () {
            try {
               Request byeRequest = this.dialog.createRequest(Request.BYE);
               ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
               dialog.sendRequest(ct);
               dialog = null;
            } catch (Exception ex) {
                ex.printStackTrace();                
            }

        }
    }
    
    public void processRequest(RequestEvent requestEvent) {
        final Request request = requestEvent.getRequest();
        final ServerTransaction serverTransactionId = requestEvent.getServerTransaction();

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
     * Process the ACK request.
     */
    public void processAck(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
    	final Dialog dialog = requestEvent.getDialog();
    	final RequestExt request = (RequestExt) requestEvent.getRequest();
    	if(((SipURI)request.getFromHeader().getAddress().getURI()).getUser().equalsIgnoreCase(TIMER_USER)) {
    		timer.schedule(new ByeTask(dialog), BYE_DELAY) ;
    	}
    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {

        final Request request = requestEvent.getRequest();
        final SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        ServerTransaction st = serverTransaction;        
        try {
        	if (st == null) {
        		st = sipProvider.getNewServerTransaction(request);
            }
        	sipProvider.getNewDialog(st);
        	final String toTag = ""+System.nanoTime();
            Response response = messageFactory.createResponse(Response.RINGING,
                    request);            
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag(toTag); // Application is supposed to set.            
			// Creates a dialog only for non trying responses				
            st.sendResponse(response);

            response = messageFactory.createResponse(Response.OK,
                    request);
            final Address address = addressFactory.createAddress("Shootme <sip:"
                    + myAddress + ":" + myPort + ">");
            final ContactHeader contactHeader = headerFactory
                    .createContactHeader(address);
            response.addHeader(contactHeader);
            toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag(toTag); // Application is supposed to set.
            st.sendResponse(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            //junit.framework.TestCase.fail("Exit JVM");
        }
    }


    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {
        final Request request = requestEvent.getRequest();
        final Dialog dialog = requestEvent.getDialog();
        try {
            final Response response = messageFactory.createResponse(200, request);
            if(serverTransactionId == null) {
            	serverTransactionId = ((SipProvider)requestEvent.getSource()).getNewServerTransaction(request);
            }
            serverTransactionId.sendResponse(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            //junit.framework.TestCase.fail("Exit JVM");

        }
    }

    public void processCancel(RequestEvent requestEvent,
            ServerTransaction serverTransactionId) {

    }

    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
    	Request request = null;
    	if(timeoutEvent.getClientTransaction() == null) {
    		request = timeoutEvent.getServerTransaction().getRequest();
    	} else {
    		request = timeoutEvent.getClientTransaction().getRequest();
    	}
    	//System.out.println(request);
    }

    public void init() {        
        sipStack = null;
        sipFactory = SipFactory.getInstance();
        System.setProperty( "javax.net.ssl.keyStore",  "/Users/vladimirralev/keystore.ImportKey" );
        System.setProperty( "javax.net.ssl.trustStore", "/Users/vladimirralev/keystore.ImportKey" );
        System.setProperty( "javax.net.ssl.keyStorePassword", "importkey" );
        System.setProperty( "javax.net.ssl.keyStoreType", "jks" );
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new File("mss-sip-stack.properties")));
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
        } catch (Exception e) {
            // could not find
            // gov.nist.jain.protocol.ip.sip.SipStackImpl
            // in the classpath
            e.printStackTrace();
            System.err.println(e.getMessage());
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            junit.framework.TestCase.fail("Exit JVM");
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
            ListeningPoint lpTcp = sipStack.createListeningPoint("127.0.0.1",
                    myPort, TRANSPORT_TCP);
            ListeningPoint lpUdp = sipStack.createListeningPoint("127.0.0.1",
                    myPort, TRANSPORT_UDP);

            Shootme listener = this;

            sipProvider = sipStack.createSipProvider(lpTcp);
            sipProvider.addListeningPoint(lpUdp);
            sipProvider.addSipListener(listener);

        } catch (Exception ex) {
            ex.printStackTrace();
            usage();
        }

    }

    public static void main(String args[]) {
        new Shootme().init();
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
    	Request request = null;
    	if(transactionTerminatedEvent.getClientTransaction() == null) {
    		request = transactionTerminatedEvent.getServerTransaction().getRequest();
    	} else {
    		request = transactionTerminatedEvent.getClientTransaction().getRequest();
    	}
    	//System.out.println(request);
    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
    	Dialog dialog = dialogTerminatedEvent.getDialog();
    	//System.out.println(dialog);
    }

}
