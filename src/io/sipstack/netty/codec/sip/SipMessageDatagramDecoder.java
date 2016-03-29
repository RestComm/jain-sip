package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.StringMsgParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import static io.sipstack.netty.codec.sip.SipMessageStreamDecoder.MAX_ALLOWED_CONTENT_LENGTH;
import static io.sipstack.netty.codec.sip.SipMessageStreamDecoder.MAX_ALLOWED_HEADERS_SIZE;
import static io.sipstack.netty.codec.sip.SipMessageStreamDecoder.MAX_ALLOWED_INITIAL_LINE_SIZE;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.List;

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
public final class SipMessageDatagramDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private final Clock clock;
    
    private static StringMsgParser parser = new StringMsgParser();

    private static final byte[] CRLF_ARR = {'\r', '\n'};
    private static final Buffer CRLF = Buffers.wrap(CRLF_ARR);
    
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

    /**
     * Contains the raw framed message.
     */
    private RawMessage message = new RawMessage(MAX_ALLOWED_INITIAL_LINE_SIZE, MAX_ALLOWED_HEADERS_SIZE,
                MAX_ALLOWED_CONTENT_LENGTH);    

    private SIPMessage toSipMessage(final RawMessage raw) throws Exception {
        Buffer msg = Buffers.wrap(raw.getInitialLine(), CRLF);
        msg = Buffers.wrap(msg, raw.getHeaders());
        msg = Buffers.wrap(msg, CRLF);
        //msg = Buffers.wrap(msg, CRLF);
        if (msg != null) {
            SIPMessage parsedMsg = parser.parseSIPMessage(msg.getArray(), false, false, null);
            if (raw.getPayload() != null && !raw.getPayload().isEmpty()) {
                parsedMsg.setMessageContent(raw.getPayload().getArray());
            }
            return parsedMsg;
        } else {
            throw new RuntimeException("msg was null");
        }
    }

    private void dropConnection(final ChannelHandlerContext ctx, final String reason) {
    }

    private void reset() {
        this.message = new RawMessage(MAX_ALLOWED_INITIAL_LINE_SIZE, MAX_ALLOWED_HEADERS_SIZE,
                MAX_ALLOWED_CONTENT_LENGTH);
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
        final ByteBuf buffer = msg.content();

        // some clients are sending various types of pings even over
        // UDP, such as linphone which is sending "jaK\n\r".
        // According to RFC5626, the only valid ping over UDP
        // is to use a STUN request and since such a request is
        // at least 20 bytes we will simply ignore anything less
        // than that. And yes, there is no way that an actual
        // SIP message ever could be less than 20 bytes.
        if (buffer.readableBytes() < 20) {
            return;
        }
        try {
            while (!this.message.isComplete() && buffer.isReadable()) {
                final byte b = buffer.readByte();
                this.message.write(b);
            }
        } catch (final MaxMessageSizeExceededException e) {
            dropConnection(ctx, e.getMessage());
            // TODO: mark this connection as dead since the future
            // for closing this decoder may take a while to actually
            // do its job
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        if (this.message.isComplete()) {
            final SIPMessage sipMsg = toSipMessage(this.message);
            final Channel channel = ctx.channel();
            final Connection connection = new TcpConnection(channel, (InetSocketAddress) channel.remoteAddress());
            out.add(new DefaultSipMessageEvent(connection, sipMsg, arrivalTime));
            reset();
        }
    }

}
