/**
 *
 */
package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.parser.StringMsgParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author jonas
 *
 */
public class SipMessageStreamDecoder extends ByteToMessageDecoder {

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

    private final Clock clock;

    /**
     * Contains the raw framed message.
     */
    private RawMessage message;

    /**
     *
     */
    public SipMessageStreamDecoder(final Clock clock) {
        this.clock = clock;
        reset();
    }

    public SipMessageStreamDecoder() {
        this(new SystemClock());
    }

    @Override
    public boolean isSingleDecode() {
        return true;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out)
            throws Exception {
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
            final long arrivalTime = this.clock.getCurrentTimeMillis();
            final SIPMessage msg = toSipMessage(this.message);
            final Channel channel = ctx.channel();
            final Connection connection = new TcpConnection(channel, (InetSocketAddress) channel.remoteAddress());
            out.add(new DefaultSipMessageEvent(connection, msg, arrivalTime));
            reset();
        }
    }

    private static StringMsgParser parser = new StringMsgParser();

    private static final byte[] CRLF_ARR = {'\r', '\n'};
    private static final Buffer CRLF = Buffers.wrap(CRLF_ARR);

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

}
