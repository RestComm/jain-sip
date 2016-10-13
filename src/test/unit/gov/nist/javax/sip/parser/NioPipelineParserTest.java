package test.unit.gov.nist.javax.sip.parser;

import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.NioPipelineParser;
import gov.nist.javax.sip.parser.SIPMessageListener;
import gov.nist.javax.sip.stack.NioMessageProcessorFactory;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import java.text.ParseException;
import java.util.Properties;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipStack;
import junit.framework.Assert;
import test.tck.msgflow.callflows.ScenarioHarness;

public class NioPipelineParserTest extends ScenarioHarness {

    private static NioPipelineParser parser;
    private static AsserterListener listener;
    
    //TODO replace by AssertUntil
    private static final int ASSERTION_WAIT = 200;

    public NioPipelineParserTest() {
        super("NioPipelineParserTest", true);
    }

    @Override
    public void setUp() throws PeerUnavailableException {
        final Properties defaultProperties = new Properties();

        defaultProperties.setProperty("javax.sip.STACK_NAME", "server");
        defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
        defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "server_debug_ViaRPortTest.txt");
        defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "server_log_ViaRPortTest.txt");
        //defaultProperties.setProperty("gov.nist.javax.sip.TCP_POST_PARSING_THREAD_POOL_SIZE", "64");
        defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        SipStack sipStack = sipFactory.createSipStack(defaultProperties);
        listener = new AsserterListener();
        parser = new NioPipelineParser((SIPTransactionStack) sipStack, listener, 10000);
    }

    private static final String HEADER_CHUNK = "INVITE sip:00001002000022@p25dr;user=TIA-P25-SU SIP/2.0\r\n"
            + "CSeq: 1 INVITE\r\n"
            + "From: <sip:0000100200000c@p25dr;user=TIA-P25-SU>;tag=841\r\n"
            + "To: <sip:00001002000022@p25dr;user=TIA-P25-SU>\r\n"
            + "Via: SIP/2.0/UDP 02.002.00001.p25dr;branch=z9hG4bKa10f04383e3d8e8dbf3f6d06f6bb6880\r\n"
            + "Max-Forwards: 70\r\n"
            + "Contact: <sip:02.002.00001.p25dr>\r\n"
            + "Call-ID: c6a12ddad0ddc1946d9f443c884a7768@127.0.0.1\r\n"
            + "Content-Type: application/sdp;level=1\r\n"
            + "Content-Length: 145\r\n";
    private static final String HEADER1 =  "Allow: REGISTER,INVITE,ACK,BYE,CANCEL\r";
    private static final String HEADER2 =  "\n";
    private static final String BODY_CHUNK ="v=0\r\n"
            + "o=- 30576 0 IN IP4 127.0.0.1\r\n"
            + "s=TIA-P25-SuToSuCall\r\n"
            + "t=0 0\r\n"
            + "c=IN IP4 127.0.0.1\r\n"
            + "m=audio 12412 RTP/AVP 100\r\n"
            + "a=rtpmap:100 X-TIA-P25-IMBE/8000\r\n";

    class AsserterListener implements SIPMessageListener {

        private int processedMsgs = 0;
        private SIPMessage lastMsg = null;

        @Override
        public void processMessage(SIPMessage msg) throws Exception {
            lastMsg = msg;
            processedMsgs = processedMsgs + 1;
        }

        @Override
        public void sendSingleCLRF() throws Exception {
        }

        @Override
        public void handleException(ParseException ex, SIPMessage sipMessage, Class headerClass, String headerText, String messageText) throws ParseException {
        }

        public int getProcessedMsgs() {
            return processedMsgs;
        }

        public SIPMessage getLastMsg() {
            return lastMsg;
        }
    }
    
    public void testNormalBodySeparation() throws Exception {
        parser.addBytes((HEADER_CHUNK + "\r\n").getBytes());
        parser.addBytes(BODY_CHUNK.getBytes());
        Thread.sleep(ASSERTION_WAIT);
        Assert.assertEquals(1, listener.getProcessedMsgs());
    }
    
    public void testHeaderSeparationAtChunkEnd() throws Exception {
        parser.addBytes((HEADER_CHUNK + HEADER1).getBytes());
        parser.addBytes((HEADER2 + "\r\n" + BODY_CHUNK).getBytes());
        Thread.sleep(ASSERTION_WAIT);        
        Assert.assertEquals(1, listener.getProcessedMsgs());
    }    

    public void testBodySeparationAtChunkEnd() throws Exception {
        parser.addBytes((HEADER_CHUNK + "\r").getBytes());
        parser.addBytes(("\n" + BODY_CHUNK).getBytes());
        Thread.sleep(ASSERTION_WAIT);        
        Assert.assertEquals(1, listener.getProcessedMsgs());
    }

}
