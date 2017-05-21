package test.unit.gov.nist.javax.sip.stack.nonblocking;

import gov.nist.core.CommonLogger;
import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.NioMessageProcessorFactory;
import gov.nist.javax.sip.stack.NioTcpMessageProcessor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import junit.framework.Assert;
import test.tck.msgflow.callflows.ScenarioHarness;

/**
 * Tests for new NonBlocking connect feature.
 * 
 * All these tests are concurrently executed to exercise the
 * different sync sections, and ensure consistent results.
 *
 */
public class NonBlockingTest extends ScenarioHarness {

    private Set<Closeable> testResources;

    public NonBlockingTest() {
        super("NonBlockingTest", true);
    }

    private static final int CLIENT_PORT = 6500;

	private static final int SERVER_PORT = 5600;

    private static final String TEST_PROTOCOL = "tcp";
    
    private static final int NUM_THREADS = 30;
    
    private static final int THREAD_ASSERT_TIME = 6000;    
    

    public void setUp() throws Exception {
        testResources = new HashSet();
    }
    
    class PoolCloser implements Closeable {
    	private ExecutorService pool;
    	
		public PoolCloser(ExecutorService pool) {
			super();
			this.pool = pool;
		}

		@Override
		public void close() throws IOException {
			pool.shutdownNow();
		}
    }

    public void tearDown() throws Exception {
        for (Closeable rAux : testResources) {
            try {
                rAux.close();
            } catch (Exception e) {

            }
        }
    }
    

    public void testNoRemoteSocket() throws Exception {
        final Client client = new Client();
        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        testResources.add(new PoolCloser(pool));
        for (int i = 0; i < NUM_THREADS; i++) {
            pool.submit(new Runnable() {
                public void run() {
                    try {
                        client.sendInvite(SERVER_PORT);
                    } catch (Exception e) {

                    }
                }
            });
        }
        pool.awaitTermination(THREAD_ASSERT_TIME, TimeUnit.MILLISECONDS);
        Thread.sleep(THREAD_ASSERT_TIME);
        Assert.assertTrue(!client.responses.isEmpty());
        Assert.assertEquals(503, client.responses.get(0).getResponse().getStatusCode());
    }

    public void testConnReestablished() throws Exception {
        final Client client = new Client();
        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        testResources.add(new PoolCloser(pool));     
        Set<Closeable> servers = new HashSet();
    	Server server = new Server(SERVER_PORT);
    	servers.add(server);        
        for (int i = 0; i < NUM_THREADS; i++) {

            pool.submit(new Runnable() {
                public void run() {
                    try {
                        client.sendInvite(SERVER_PORT);
                    } catch (Exception e) {

                    }
                }
            });
        }
        pool.awaitTermination(THREAD_ASSERT_TIME, TimeUnit.MILLISECONDS);
        Thread.sleep(THREAD_ASSERT_TIME);        
        //*2 counting for ACK
        Assert.assertEquals(NUM_THREADS * 2, server.requestCounter.get());
        for (Closeable rAux : servers) {
            try {
                rAux.close();
            } catch (Exception e) {

            }
        }
        SipFactory.getInstance().resetFactory();
        CommonLogger.getLogger(NonBlockingTest.class).logInfo("server closed");       
        Thread.sleep(THREAD_ASSERT_TIME);
        client.resetCounters();
        CommonLogger.getLogger(NonBlockingTest.class).logInfo("server restarted");
    	server = new Server(SERVER_PORT);
    	testResources.add(server);          
        for (int i = 0; i < NUM_THREADS; i++) {
      	
            pool.submit(new Runnable() {
                public void run() {
                    try {
                        client.sendInvite(SERVER_PORT);
                    } catch (Exception e) {

                    }
                }
            });
        }
        Thread.sleep(THREAD_ASSERT_TIME);
        Assert.assertEquals(NUM_THREADS, client.responses.size()); 
    }    

    public class Server extends SipAdapter implements Closeable {

        protected SipStack sipStack;

        protected SipFactory sipFactory = null;

        protected SipProvider provider = null;
        private MessageFactory messageFactory;
        private AddressFactory addressFactory;
        private HeaderFactory headerFactory;        
        
        private String host;

        public Server(int port) {
            try {
                final Properties defaultProperties = new Properties();
                host = "127.0.0.1";

                defaultProperties.setProperty("javax.sip.STACK_NAME", "server" + port);
                defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "TRACE");
                defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "server_debug_ViaRPortTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "server_log_ViaRPortTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
                defaultProperties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS",
                        "false");
                defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
                this.sipFactory = SipFactory.getInstance();
                this.sipFactory.setPathName("gov.nist");
                messageFactory = sipFactory.createMessageFactory();
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                this.sipStack = this.sipFactory.createSipStack(defaultProperties);
                this.sipStack.start();
                ListeningPoint lp = this.sipStack.createListeningPoint(host, port, TEST_PROTOCOL);
                this.provider = this.sipStack.createSipProvider(lp);
                this.provider.addSipListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail("unexpected exception ");
            }

        }
        
        public void processRequest(RequestEvent arg0) {
            super.processRequest(arg0);
            try {
            	SIPRequest req = (SIPRequest)arg0.getRequest();
            	if (req.getRequestLine().getMethod().equals("INVITE")) {
					ServerTransaction st = arg0.getServerTransaction();
		            if (st == null) {
		            	SipProvider sipProvider = (SipProvider) arg0.getSource();
		                st = sipProvider.getNewServerTransaction(req);
		            }
		            Address address = addressFactory.createAddress("Shootme <sip:127.0.0.1:" + SERVER_PORT + ">");
		            ContactHeader contactHeader = headerFactory.createContactHeader(address);
		            Response res = messageFactory.createResponse(200, req);
		            res.addHeader(contactHeader);
		            st.sendResponse(res);
            	}
			} catch (Exception e) {
			}
            
        }        

        @Override
        public void close() throws IOException {
            this.sipStack.stop();

        }
    }

    public class Client extends SipAdapter implements Closeable {

        private SipFactory sipFactory;
        private SipStack sipStack;
        private SipProvider provider;
        private HeaderFactory headerFactory;
        private MessageFactory messageFactory;
        private AddressFactory addressFactory; 
        private String host;
        private AtomicLong cSeqCounter = new AtomicLong(1);

        public Client() {
            try {
                testResources.add(this);
                final Properties defaultProperties = new Properties();
                host = "127.0.0.1";
                defaultProperties.setProperty("javax.sip.STACK_NAME", "client");
                defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "TRACE");
                defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "client_debug.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "client_log.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
                defaultProperties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS", "false");
                defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
                defaultProperties.setProperty("gov.nist.javax.sip.NIO_BLOCKING_MODE", "NONBLOCKING");

                this.sipFactory = SipFactory.getInstance();
                this.sipFactory.setPathName("gov.nist");
                this.sipStack = this.sipFactory.createSipStack(defaultProperties);
                this.sipStack.start();
                ListeningPoint lp = this.sipStack.createListeningPoint(host, CLIENT_PORT, TEST_PROTOCOL);
                this.provider = this.sipStack.createSipProvider(lp);
                headerFactory = this.sipFactory.createHeaderFactory();
                messageFactory = this.sipFactory.createMessageFactory();
                addressFactory = this.sipFactory.createAddressFactory();
                this.provider.addSipListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail("unexpected exception ");
            }
        }

        public void sendInvite(int serverPort) throws Exception {

            Address fromAddress = addressFactory.createAddress("here@somewhere:5070");
            ContactHeader contactHeader1 = headerFactory.createContactHeader(addressFactory.createAddress("sip:here@somewhere:5070"));
            ContactHeader contactHeader2 = headerFactory.createContactHeader(addressFactory.createAddress("sip:here@somewhereelse:5080"));

            CallIdHeader callId = provider.getNewCallId();
            CSeqHeader cSeq = headerFactory.createCSeqHeader(cSeqCounter.getAndIncrement(), Request.INVITE);
            FromHeader from = headerFactory.createFromHeader(fromAddress, "1234");
            ToHeader to = headerFactory.createToHeader(addressFactory.createAddress("server@" + host + ":" + SERVER_PORT), null);
            ViaHeader via = ((ListeningPointImpl) provider.getListeningPoint(TEST_PROTOCOL)).getViaHeader();
            List<ViaHeader> vias = Arrays.asList(via);
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(10);

            URI requestURI = addressFactory.createURI("sip:test@" + host + ":" + serverPort);
            Request request = messageFactory.createRequest(requestURI, Request.INVITE, callId, cSeq, from, to, vias, maxForwards);
            System.out.println(request);
            assertTrue(request.toString().indexOf("rport=") == -1);

            request.setRequestURI(requestURI);
            request.addHeader(contactHeader1);
            request.addHeader(contactHeader2);
            ClientTransaction ctx = this.provider.getNewClientTransaction(request);
            ctx.sendRequest();
        }

        @Override
        public void close() throws IOException {
            this.sipStack.stop();
        }
        
        public void processResponse(ResponseEvent arg0) {
        	super.processResponse(arg0);
            if (arg0.getResponse().getStatusCode() == 200 
            		&& arg0.getClientTransaction() != null
            		&& arg0.getClientTransaction().getDialog() != null) {
            	Request ackRequest;
				try {
					CSeq seq = (CSeq) arg0.getResponse().getHeader("CSeq");
					ackRequest = arg0.getClientTransaction().getDialog().createAck(seq.getSeqNumber());
					arg0.getClientTransaction().getDialog().sendAck(ackRequest);
				} catch (Exception e) {
					e.printStackTrace();
				}

            }
        }
    }

    private static class SipAdapter implements SipListener {

        AtomicInteger diagTerminatedCounter = new AtomicInteger(0);
        AtomicInteger IOExceptionCounter = new AtomicInteger(0);
        AtomicInteger requestCounter = new AtomicInteger(0);
        List<ResponseEvent> responses = Collections.synchronizedList(new ArrayList<ResponseEvent>());
        AtomicInteger timeoutcounter = new AtomicInteger(0);
        AtomicInteger txTerminatedCounter = new AtomicInteger(0);

        
        public void resetCounters(){
        	diagTerminatedCounter.set(0);
        	IOExceptionCounter.set(0);
        	requestCounter.set(0);
        	responses = Collections.synchronizedList(new ArrayList<ResponseEvent>());
        	timeoutcounter.set(0);
        	txTerminatedCounter.set(0);
        }
        public void processDialogTerminated(DialogTerminatedEvent arg0) {
            diagTerminatedCounter.incrementAndGet();
        }

        public void processIOException(IOExceptionEvent arg0) {
            IOExceptionCounter.incrementAndGet();
        }

        public void processRequest(RequestEvent arg0) {
            requestCounter.incrementAndGet();
        }

        public void processResponse(ResponseEvent arg0) {
            responses.add(arg0);
        }

        public void processTimeout(TimeoutEvent arg0) {
            timeoutcounter.incrementAndGet();
        }

        public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
            txTerminatedCounter.incrementAndGet();
        }
    }
}
