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
import gov.nist.core.LogWriter;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import static io.sipstack.netty.codec.sip.AbstractConnection.CR;
import static io.sipstack.netty.codec.sip.AbstractConnection.LF;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyTcpMessageChannel extends NettyConnectionOrientedMessageChannel {

    private static StackLogger logger = CommonLogger
            .getLogger(NettyTcpMessageChannel.class);
    protected static HashMap<SocketChannel, NettyTcpMessageChannel> channelMap = new HashMap<SocketChannel, NettyTcpMessageChannel>();

    protected SocketChannel socketChannel;
    protected long lastActivityTimeStamp;

    public static NettyTcpMessageChannel create(
            NettyTcpMessageProcessor nioTcpMessageProcessor,
            SocketChannel socketChannel) throws IOException {
        NettyTcpMessageChannel retval = channelMap.get(socketChannel);
        if (retval == null) {
            retval = new NettyTcpMessageChannel(nioTcpMessageProcessor,
                    socketChannel);
            channelMap.put(socketChannel, retval);
        }
        retval.messageProcessor = nioTcpMessageProcessor;
        return retval;
    }

    public static NettyTcpMessageChannel getMessageChannel(SocketChannel socketChannel) {
        return channelMap.get(socketChannel);
    }

    public static void putMessageChannel(SocketChannel socketChannel,
            NettyTcpMessageChannel nioTcpMessageChannel) {
        channelMap.put(socketChannel, nioTcpMessageChannel);
    }

    public static void removeMessageChannel(SocketChannel socketChannel) {
        channelMap.remove(socketChannel);
    }

    public void readChannel() {

    }

    protected NettyTcpMessageChannel(NettyTcpMessageProcessor messageProcessor,
            SocketChannel socketChannel) throws IOException {
        super(messageProcessor.getSIPStack());
        try {
            if (socketChannel.remoteAddress() != null) {
            this.peerAddress = socketChannel.remoteAddress().getAddress();
            this.peerPort = socketChannel.remoteAddress().getPort();
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(NettyTcpMessageChannel.class.getName()).log(Level.SEVERE, null, ex);
                }
                this.peerAddress = socketChannel.remoteAddress().getAddress();
                this.peerPort = socketChannel.remoteAddress().getPort();                
            }
            this.socketChannel = socketChannel;
            super.mySock = socketChannel;
            this.peerProtocol = messageProcessor.transport;
            lastActivityTimeStamp = System.currentTimeMillis();
            super.key = MessageChannel.getKey(peerAddress, peerPort, messageProcessor.transport);

            myAddress = messageProcessor.getIpAddress().getHostAddress();
            myPort = messageProcessor.getPort();

        } finally {
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                logger.logDebug("Done creating NioTcpMessageChannel " + this + " socketChannel = " + socketChannel);
            }
        }

    }

    public NettyTcpMessageChannel(InetAddress inetAddress, int port,
            SIPTransactionStack sipStack,
            NettyTcpMessageProcessor messageProcessor) throws IOException {
        super(sipStack);
        if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
            logger.logDebug("NioTcpMessageChannel::NioTcpMessageChannel: "
                    + inetAddress.getHostAddress() + ":" + port);
        }
        try {
            this.messageProcessor = messageProcessor;
            // Take a cached socket to the destination, if none create a new one and cache it
            socketChannel = messageProcessor.nioHandler.createOrReuseSocket(
                    inetAddress, port);
            peerAddress = socketChannel.remoteAddress().getAddress();
            peerPort = socketChannel.remoteAddress().getPort();
            super.mySock = socketChannel;
            peerProtocol = getTransport();
            putMessageChannel(socketChannel, this);
            lastActivityTimeStamp = System.currentTimeMillis();
            super.key = MessageChannel.getKey(peerAddress, peerPort, getTransport());

            myAddress = messageProcessor.getIpAddress().getHostAddress();
            myPort = messageProcessor.getPort();

        } finally {
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                logger.logDebug("NioTcpMessageChannel::NioTcpMessageChannel: Done creating NioTcpMessageChannel "
                        + this + " socketChannel = " + socketChannel);
            }
        }
    }

    public Channel getSocketChannel() {
        return socketChannel;
    }

    @Override
    protected void close(boolean removeSocket, boolean stopKeepAliveTask) {

    }

    /**
     * get the transport string.
     *
     * @return "tcp" in this case.
     */
    public String getTransport() {
        return "TCP";
    }

    /**
     * Send a message to a specified address.
     *
     * @param message Pre-formatted message to send.
     * @param receiverAddress Address to send it to.
     * @param receiverPort Receiver port.
     * @throws IOException If there is a problem connecting or sending.
     */
    public void sendMessage(byte message[], InetAddress receiverAddress,
            int receiverPort, boolean retry) throws IOException {
        sendTCPMessage(message, receiverAddress, receiverPort, retry);
    }

    /**
     * Send a message to a specified address.
     *
     * @param message Pre-formatted message to send.
     * @param receiverAddress Address to send it to.
     * @param receiverPort Receiver port.
     * @throws IOException If there is a problem connecting or sending.
     */
    public void sendTCPMessage(byte message[], InetAddress receiverAddress,
            int receiverPort, boolean retry) throws IOException {
            final Buffer b = Buffers.wrap(message);
            final int capacity = b.capacity() + 2;
            final ByteBuf buffer = mySock.alloc().buffer(capacity, capacity);

            for (int i = 0; i < b.getReadableBytes(); ++i) {
                buffer.writeByte(b.getByte(i));
            }
            buffer.writeByte(CR);
            buffer.writeByte(LF);        
        this.mySock.writeAndFlush(buffer);
        //TODO
        //throw new UnsupportedOperationException();

    }

    public void onNewSocket(byte[] message) {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Exception processor for exceptions detected from the parser. (This is
     * invoked by the parser when an error is detected).
     *
     * @param sipMessage -- the message that incurred the error.
     * @param ex -- parse exception detected by the parser.
     * @param header -- header that caused the error.
     * @throws ParseException Thrown if we want to reject the message.
     */
    public void handleException(ParseException ex, SIPMessage sipMessage,
            Class hdrClass, String header, String message)
            throws ParseException {
        if (logger.isLoggingEnabled()) {
            logger.logException(ex);
        }
        // Log the bad message for later reference.
        if ((hdrClass != null)
                && (hdrClass.equals(From.class) || hdrClass.equals(To.class)
                || hdrClass.equals(CSeq.class)
                || hdrClass.equals(Via.class)
                || hdrClass.equals(CallID.class)
                || hdrClass.equals(ContentLength.class)
                || hdrClass.equals(RequestLine.class) || hdrClass
                .equals(StatusLine.class))) {
            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                logger.logDebug("Encountered Bad Message \n"
                        + sipMessage.toString());
            }

			// JvB: send a 400 response for requests (except ACK)
            // Currently only UDP, @todo also other transports
            String msgString = sipMessage.toString();
            if (!msgString.startsWith("SIP/") && !msgString.startsWith("ACK ")) {
                if (socketChannel != null) {
                    if (logger.isLoggingEnabled(LogWriter.TRACE_ERROR)) {
                        logger
                                .logError("Malformed mandatory headers: closing socket! :"
                                        + socketChannel.toString());
                    }

                    socketChannel.close();

                }
            }

            throw ex;
        } else {
            sipMessage.addUnparsed(header);
        }
    }

    /**
     * Equals predicate.
     *
     * @param other is the other object to compare ourselves to for equals
     */
    @Override
    public boolean equals(Object other) {

        if (!this.getClass().equals(other.getClass())) {
            return false;
        } else {
            NettyTcpMessageChannel that = (NettyTcpMessageChannel) other;
            if (this.socketChannel != that.socketChannel) {
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * TCP Is not a secure protocol.
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    public long getLastActivityTimestamp() {
        return lastActivityTimeStamp;
    }

    @Override
    protected void sendMessage(byte[] msg, boolean b) throws IOException {
        //TODO
        throw new UnsupportedOperationException();        
    }

}
