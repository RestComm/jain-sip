package gov.nist.javax.sip.stack;

import gov.nist.core.CommonLogger;
import gov.nist.core.HostPort;
import gov.nist.core.LogWriter;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;

public class NettyUDPMessageProcessor extends MessageProcessor {

    private static StackLogger logger = CommonLogger.getLogger(NettyUDPMessageProcessor.class);
    private final EventLoopGroup workerGroup;
    private final Bootstrap b = new Bootstrap();
    private Channel bindChannel;
    /**
     * The Mapped port (in case STUN suport is enabled)
     */
    private int port;

    /**
     * Incoming messages are queued here.
     */
    protected BlockingQueue<DatagramQueuedMessageDispatch> messageQueue;

    /**
     * Auditing taks that checks for outdated requests in the queue
     */
    BlockingQueueDispatchAuditor congestionAuditor;

    /**
     * A list of message channels that we have started.
     */
    protected LinkedList messageChannels;


    private int maxMessageSize = SipStackImpl.MAX_DATAGRAM_SIZE;
    private final Class _datagramChannelClass;

    /**
     * Constructor.
     *
     * @param sipStack pointer to the stack.
     */
    protected NettyUDPMessageProcessor(InetAddress ipAddress,
            SIPTransactionStack sipStack, int port) throws IOException {
        super(ipAddress, port, "udp", sipStack);

        this.sipStack = sipStack;
        if (sipStack.getMaxMessageSize() < SipStackImpl.MAX_DATAGRAM_SIZE && sipStack.getMaxMessageSize() > 0) {
            this.maxMessageSize = sipStack.getMaxMessageSize();
        }
        if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
            logger.logDebug("Max Message size is " + maxMessageSize);
        }

        this.port = port;
        
	if (Epoll.isAvailable())
	{
            _datagramChannelClass = EpollDatagramChannel.class;
            workerGroup = new EpollEventLoopGroup(sipStack.threadPoolSize);
            logger.logInfo("Using Epoll polling method!!!.");
	}
	else
	{
            _datagramChannelClass = NioDatagramChannel.class;
            workerGroup = new NioEventLoopGroup(sipStack.threadPoolSize);
	}        
        
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
                option(ChannelOption.SO_RCVBUF, sipStack.getReceiveUdpBufferSize()).
                option(ChannelOption.SO_SNDBUF, sipStack.getSendUdpBufferSize()).             
                group(workerGroup)
                .channel(_datagramChannelClass)
                .handler(new NettyUDPMessageProcessor.InboundInit());
        

    }

    private SipMessageDatagramDecoder decoder = new SipMessageDatagramDecoder();

    class InboundInit extends io.netty.channel.ChannelInitializer {

        @Override
        public void initChannel(final Channel ch) throws Exception {
            final ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", decoder);
            NettyUDPMessageChannel handler = new NettyUDPMessageChannel(getIpAddress(), getPort(),
                    sipStack, NettyUDPMessageProcessor.this);
            pipeline.addLast("handler", handler);
        }
    }       

    /**
     * Get port on which to listen for incoming stuff.
     *
     * @return port on which I am listening.
     */
    @Override
    public int getPort() {
        return this.port;
    }

    /**
     * Start our processor thread.
     * @throws java.io.IOException
     */
    @Override
    public void start() throws IOException {

        final InetSocketAddress socketAddress = new InetSocketAddress(this.getIpAddress(), this.getPort());
        try {
            bindChannel = b.bind(socketAddress).sync().channel();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }
    
    private Channel outboundChannel;
    
    public synchronized Channel getOutboundChannel() throws InterruptedException {
        //final InetSocketAddress socketAddress = new InetSocketAddress(this.getIpAddress(), this.getPort());
        if (outboundChannel == null) {
            outboundChannel = b.bind(0).sync().channel();
             
        }
        return outboundChannel;
    }    
    
    
    public ChannelFuture createOutboundChannel(InetAddress peerAddress,
            int peerPort) {
        //final InetSocketAddress socketAddress = new InetSocketAddress(this.getIpAddress(), this.getPort());
            return b.bind(0);
    }

    /**
     * Shut down the message processor. Close the socket for recieving incoming
     * messages.
     */
    @Override
    public void stop() {
        for (Object messageChannel : messageChannels) {
            ((MessageChannel) messageChannel).close();
        }
        Future<?> shutdownGracefully = this.workerGroup.shutdownGracefully();
        shutdownGracefully.awaitUninterruptibly(5000);
        bindChannel.close();
    }

    /**
     * Return the transport string.
     *
     * @return the transport string
     */
    @Override
    public String getTransport() {
        return "udp";
    }

    /**
     * Returns the stack.
     *
     * @return my sip stack.
     */
    @Override
    public SIPTransactionStack getSIPStack() {
        return sipStack;
    }

    /**
     * Create and return new TCPMessageChannel for the given host/port.
     */
    @Override
    public MessageChannel createMessageChannel(HostPort targetHostPort)
            throws UnknownHostException {
        return new NettyUDPMessageChannel(targetHostPort.getInetAddress(),
                targetHostPort.getPort(), sipStack, this);
    }

    @Override
    public MessageChannel createMessageChannel(InetAddress host, int port)
            throws IOException {
        return new NettyUDPMessageChannel(host, port, sipStack, this);
    }

    /**
     * Default target port for UDP
     */
    @Override
    public int getDefaultTargetPort() {
        return 5060;
    }

    /**
     * UDP is not a secure protocol.
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    /**
     * UDP can handle a message as large as the MAX_DATAGRAM_SIZE.
     */
    @Override
    public int getMaximumMessageSize() {
        return sipStack.getReceiveUdpBufferSize();
    }

    /**
     * Return true if there are any messages in use.
     */
    @Override
    public boolean inUse() {
        return !messageQueue.isEmpty();
    }

    public Channel getBindChannel() {
        return bindChannel;
    }
}
