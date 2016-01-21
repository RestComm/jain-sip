package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

/**
 * Encapsulates a
 * 
 * @author jonas@jonasborjesson.com
 */
public final class UdpConnection extends AbstractConnection {

    // public UdpConnection(final ChannelHandlerContext ctx, final InetSocketAddress remoteAddress)
    // {
    // super(ctx, remoteAddress);
    // }

    public UdpConnection(final Channel channel, final InetSocketAddress remoteAddress) {
        super(channel, remoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUDP() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final SIPMessage msg) {
        final DatagramPacket pkt = new DatagramPacket(toByteBuf(msg), getRemoteAddress());
        channel().writeAndFlush(pkt);
    }

    @Override
    public boolean connect() {
        return true;
    }

}
