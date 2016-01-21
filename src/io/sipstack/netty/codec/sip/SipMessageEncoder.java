/**
 * 
 */
package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;


import java.io.IOException;

/**
 * @author jonas
 *
 */
public class SipMessageEncoder extends MessageToByteEncoder<SIPMessage> {
    public static final byte CR = '\r';

    public static final byte LF = '\n';
    @Override
    protected void encode(final ChannelHandlerContext ctx, final SIPMessage msg, final ByteBuf out) {
        try {
            final Buffer b = Buffers.wrap(msg.encodeAsBytes("TCP"));
            for (int i = 0; i < b.getReadableBytes(); ++i) {
                out.writeByte(b.getByte(i));
            }
            out.writeByte(CR);
            out.writeByte(LF);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
