/**
 * 
 */
package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * @author jonas@jonasborjesson.com
 */
public final class TcpConnection extends AbstractConnection {


    public TcpConnection(final Channel channel, final InetSocketAddress remote) {
        super(channel, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final SIPMessage msg) {
        channel().writeAndFlush(toByteBuf(msg));
    }

    @Override
    public boolean connect() {
        return true;
    }

}
