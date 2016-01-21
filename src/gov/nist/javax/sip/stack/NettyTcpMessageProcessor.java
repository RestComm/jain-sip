/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
package gov.nist.javax.sip.stack;

import gov.nist.core.CommonLogger;
import gov.nist.core.HostPort;
import gov.nist.core.LogWriter;
import gov.nist.core.StackLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.sipstack.netty.codec.sip.SipMessageEncoder;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.netty.codec.sip.SipMessageStreamDecoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Netty implementation for TCP.
 *
 * @author mranga
 *
 */
public class NettyTcpMessageProcessor extends NettyConnectionOrientedMessageProcessor {

    private static StackLogger logger = CommonLogger.getLogger(NettyTcpMessageProcessor.class);
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(); //new EpollEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();//new EpollEventLoopGroup();
    final ServerBootstrap b = new ServerBootstrap(); // (3)
    final Bootstrap outboundBootstrap = new Bootstrap();
    private Channel bindChannel;
    protected NettyHandler nioHandler;

    public SocketChannel blockingConnect(InetSocketAddress address, InetAddress localAddress) throws IOException {
        try {
            ChannelFuture future = outboundBootstrap.connect(address);
            Channel channel = future.sync().channel();
            SocketChannel sChannel = (SocketChannel) channel;
            return sChannel;
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    public void send(SocketChannel socket, byte[] data) {

    }

    public NettyTcpMessageChannel createMessageChannel(NettyTcpMessageProcessor nioTcpMessageProcessor, SocketChannel client) throws IOException {
        return NettyTcpMessageChannel.create(NettyTcpMessageProcessor.this, client);
    }

    class OutboundHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

        private NettyConnectionOrientedMessageChannel channel = null;

        private void initChannel(SipMessageEvent i) {
            if (channel == null) {
                String key = MessageChannel.getKey(i.getConnection().getRemoteAddress().getAddress(),
                        i.getConnection().getRemotePort(), "TCP");
                if (messageChannels.containsKey(key)) {
                    channel = messageChannels.get(key);
                } else {
                    logger.logInfo("No msg channel found" + key);
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext chc, SipMessageEvent i) throws Exception {
            initChannel(i);
            if (channel != null) {
                channel.processMessage(i.getMessage());
            }
        }

    }

    class OutboundInit extends io.netty.channel.ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) throws Exception {
            final ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new SipMessageStreamDecoder());
            pipeline.addLast("encoder", new SipMessageEncoder());
            OutboundHandler handler = new OutboundHandler();
            pipeline.addLast("handler", handler);
        }
    }

    class MyInit extends io.netty.channel.ChannelInitializer<SocketChannel> {

        @Override
        public void initChannel(final SocketChannel ch) throws Exception {
            final ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new SipMessageStreamDecoder());
            pipeline.addLast("encoder", new SipMessageEncoder());
            NettyTcpMessageChannel handler = NettyTcpMessageChannel.create(NettyTcpMessageProcessor.this, ch);
            pipeline.addLast("handler", handler);
        }
    }

    public NettyTcpMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
        super(ipAddress, port, "TCP", sipStack);
        nioHandler = new NettyHandler(sipStack, this);

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024);
        b.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024);

        b.group(this.bossGroup, this.workerGroup)
                //.channel(EpollServerSocketChannel.class)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MyInit())
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        outboundBootstrap.group(workerGroup)
                //.channel(EpollSocketChannel.class)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new OutboundInit());
        //.option(ChannelOption.SO_BACKLOG, 128);

    }

    @Override
    public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
        if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
            logger.logDebug("NioTcpMessageProcessor::createMessageChannel: " + targetHostPort);
        }
        try {
            String key = MessageChannel.getKey(targetHostPort, transport);
            if (messageChannels.get(key) != null) {
                return this.messageChannels.get(key);
            } else {
                NettyTcpMessageChannel retval = new NettyTcpMessageChannel(targetHostPort.getInetAddress(),
                        targetHostPort.getPort(), sipStack, this);

                //	retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
                synchronized (messageChannels) {
                    this.messageChannels.put(key, retval);
                }
                retval.isCached = true;
                if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                    logger.logDebug("key " + key);
                    logger.logDebug("Creating " + retval);
                }
                return retval;

            }
        } finally {
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                logger.logDebug("MessageChannel::createMessageChannel - exit");
            }
        }
    }

    @Override
    public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
        String key = MessageChannel.getKey(targetHost, port, transport);
        if (messageChannels.get(key) != null) {
            return this.messageChannels.get(key);
        } else {
            NettyTcpMessageChannel retval = new NettyTcpMessageChannel(targetHost, port, sipStack, this);

            //           retval.getSocketChannel().register(selector, SelectionKey.OP_READ);
            this.messageChannels.put(key, retval);
            retval.isCached = true;
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                logger.logDebug("key " + key);
                logger.logDebug("Creating " + retval);
            }
            return retval;
        }

    }

    // https://java.net/jira/browse/JSIP-475
    @Override
    protected synchronized void remove(
            NettyConnectionOrientedMessageChannel messageChannel) {

    }

    @Override
    public int getDefaultTargetPort() {
        return 5060;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public void start() throws IOException {
        try {
            final InetSocketAddress socketAddress = new InetSocketAddress(this.getIpAddress(), this.getPort());
            final ChannelFuture f = b.bind(socketAddress).sync();
            bindChannel = f.channel();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }

    }

    @Override
    public void stop() {
        bindChannel.close();
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

}
