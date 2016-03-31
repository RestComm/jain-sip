package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.StringMsgParser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.text.ParseException;

import java.util.List;
import gov.nist.javax.sip.parser.ParseExceptionListener;
import io.netty.channel.ChannelHandler.Sharable;
/**
 * The {@link SipMessageDatagramDecoder} will frame an incoming UDP packet into
 * a {@link SipMessage}. Since the data will only be framed, only very minimal
 * checking of whether the data is actually a valid SIP message or not will be
 * performed. It is up to the user to validate the SipMessage through the method
 * {@link SipMessage#verify()}. The philosophy is to simply just frame things as
 * fast as possible and then do lazy parsing as much as possible.
 * 
 * @author jonas@jonasborjesson.com
 */
@Sharable
public final class SipMessageDatagramDecoder extends MessageToMessageDecoder<DatagramPacket> implements ParseExceptionListener {

    private final Clock clock;
    
    private static StringMsgParser parser = new StringMsgParser();


    /**
     * The maximum allowed initial line. If we pass this threshold we will drop
     * the message and close down the connection (if we are using a connection
     * oriented protocol ie)
     */
    public static final int MAX_ALLOWED_INITIAL_LINE_SIZE = 256;

    /**
     * The maximum allowed size of ALL headers combined (in bytes).
     */
    public static final int MAX_ALLOWED_HEADERS_SIZE = 1024;

    public static final int MAX_ALLOWED_CONTENT_LENGTH = 2048;




    private void dropConnection(final ChannelHandlerContext ctx, final String reason) {
    }
   

    public SipMessageDatagramDecoder() {
        this.clock = new SystemClock();
        
    }

    public SipMessageDatagramDecoder(final Clock clock) {
        this.clock = clock;
    }  

    /**
     * Framing an UDP packet is much simpler than for a stream based protocol
     * like TCP. We just assumes that everything is correct and therefore all is
     * needed is to read the first line, which is assumed to be a SIP initial
     * line, then read all headers as one big block and whatever is left better
     * be the payload (if there is one).
     * 
     * Of course, things do go wrong. If e.g. the UDP packet is fragmented, then
     * we may end up with a partial SIP message but the user can either decide
     * to double check things by calling {@link SipMessage#verify()} or the user
     * will eventually notice when trying to access partial headers etc.
     * 
     */
    @Override
    protected void decode(final ChannelHandlerContext ctx, final DatagramPacket msg, final List<Object> out)
            throws Exception {
        final long arrivalTime = this.clock.getCurrentTimeMillis();
        byte[] msgBytes = new byte[msg.content().writerIndex()];
        msg.content().getBytes(0, msgBytes);
        final SIPMessage sipMsg = parser.parseSIPMessage(msgBytes, true, false, this);
        final Channel channel = ctx.channel();
        final Connection connection = new UdpConnection(channel, (InetSocketAddress) channel.remoteAddress());
        out.add(new DefaultSipMessageEvent(connection, sipMsg, arrivalTime));

        
    }
    
    public void handleException(
        ParseException ex,
        SIPMessage sipMessage,
        Class headerClass,
        String headerText,
        String messageText)
        throws ParseException {
        //TODO
    }    

}
