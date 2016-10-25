package test.unit.gov.nist.javax.sip.stack;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransportNotSupportedException;

import gov.nist.javax.sip.stack.NioMessageProcessorFactory;
import junit.framework.Assert;
import junit.framework.TestCase;

public class AddconcurrentProviderTest extends TestCase {
    public SipStack sipStack;
    private ExecutorService threadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());    
    private AtomicInteger portCounter = new AtomicInteger(5060);
    private AtomicBoolean failed = new AtomicBoolean(false);
	public void testAddConcurrentProvider() throws Exception
	{
        SipFactory sipFactory = null;
        sipStack = null;
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        // If you want to try TCP transport change the following to
        String transport = "udp";
        String peerHostPort = "127.0.0.1:5070";
        // String peerHostPort = "230.0.0.1:5070";
        properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/"
                + transport);
        // If you want to use UDP then uncomment this.
        properties.setProperty("javax.sip.STACK_NAME", "shootist");

        // The following properties are specific to nist-sip
        // and are not necessarily part of any other jain-sip
        // implementation.
        // You can set a max message size for tcp transport to
        // guard against denial of service attack.
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
                "logs/" + this.getClass().getName() + ".shootistdebug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                "shootistlog.txt");

        // Drop the client connection after we are done with the transaction.
        properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS",
                "false");
        // Set to 0 (or NONE) in your production code for max speed.
        // You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
        if(System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
        	properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
        }

        // Create SipStack object
        sipStack = sipFactory.createSipStack(properties);
        System.out.println("Shootist : createSipStack " + sipStack);

        
        for (int i =0 ; i < 10 ; i ++){
        	threadPool.submit(new Runnable() {

				@Override
				public void run() {
	                ListeningPoint udpListeningPoint;
					try {
						udpListeningPoint = sipStack.createListeningPoint("127.0.0.1", portCounter.incrementAndGet(), "udp");
		                SipProvider sipProvider = sipStack.createSipProvider(udpListeningPoint);
					} catch (Exception e) {
						failed.set(true);
					}
				}
        		
        	});
        	threadPool.submit(new Runnable() {

				@Override
				public void run() {
					try {
		                Iterator it = sipStack.getSipProviders();
		                if (it.hasNext()) {it.next();}
					} catch (ConcurrentModificationException e) {
						failed.set(true);
					}
				}
        		
        	});        	
        }
        threadPool.awaitTermination(1, TimeUnit.SECONDS);
        Assert.assertFalse(failed.get());
        
	}
}
