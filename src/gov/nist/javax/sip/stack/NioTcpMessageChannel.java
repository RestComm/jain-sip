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
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.NioPipelineParser;
import gov.nist.javax.sip.stack.NioTcpMessageProcessor.PendingData;

import static javax.sip.message.Response.SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLException;
import javax.sip.TransactionState;

public class NioTcpMessageChannel extends ConnectionOrientedMessageChannel {
	private static StackLogger logger = CommonLogger
			.getLogger(NioTcpMessageChannel.class);

	protected SocketChannel socketChannel;
	protected long lastActivityTimeStamp;
	NioPipelineParser nioParser = null;
	
	Queue<PendingData> queue = new ConcurrentLinkedQueue<PendingData>();
	
	synchronized void resetQueue() {
		queue = new ConcurrentLinkedQueue<PendingData>();
	}
	
	void addPendingData(PendingData d) {
		queue.add(d);
		
	}
	


        
        private static final int BUF_SIZE = 4096;        
        private final ByteBuffer byteBuffer  = ByteBuffer.allocateDirect(BUF_SIZE);
                
	public void readChannel() {
                if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                    logger.logDebug("NioTcpMessageChannel::readChannel");
                }
                
		this.isRunning = true;
		try {
			int nbytes = this.socketChannel.read(byteBuffer);
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				logger.logDebug("Read " + nbytes + " from socketChannel");
			}                        
			boolean streamError = nbytes == -1;                        
			if(streamError) 
				throw new IOException("End-of-stream read (-1). " +
					"This is usually an indication we are stuck and it is better to disconnect.");
			// This prevents us from getting stuck in a selector thread spinloop when socket is constantly ready for reading but there are no bytes.
			if(nbytes == 0) 
				throw new IOException("The socket is giving us empty TCP packets. " +
					"This is usually an indication we are stuck and it is better to disconnect.");                        
                        
			byteBuffer.flip();
			byte[] msg = new byte[byteBuffer.remaining()];
			byteBuffer.get(msg);
			byteBuffer.clear();

			// Otherwise just add the bytes to queue
			addBytes(msg);
			lastActivityTimeStamp = System.currentTimeMillis();

		} catch (Exception ex) { // https://java.net/jira/browse/JSIP-464 make sure to close connections on all exceptions to avoid the stack to hang
			// Terminate the message.
			if(ex instanceof IOException && !(ex instanceof SSLException)) {
				try {
					nioParser.addBytes("\r\n\r\n".getBytes("UTF-8"));
				} catch (Exception e) {
					// InternalErrorHandler.handleException(e);
				}
			}

			try {
				if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
					logger.logDebug("I/O Issue closing sock " + ex.getMessage() + "myAddress:myport " + myAddress + ":" + myPort + ", remoteAddress:remotePort " + peerAddress + ":" + peerPort);
					logger.logStackTrace();
				}
				
				close(true, false);
				
				
			} catch (Exception ex1) {
				if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
					logger.logDebug("Issue closing the socket " + ex1);
			}
		} 
//		catch (Exception ex) {
//			InternalErrorHandler.handleException(ex, logger);
//		}

	}
	
	// TLS will override here to add decryption
	protected void addBytes(byte[] bytes) throws Exception {
		nioParser.addBytes(bytes);
	}

	protected NioTcpMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel socketChannel) throws IOException {
		super(nioTcpMessageProcessor.getSIPStack());
		super.myClientInputStream = socketChannel.socket().getInputStream();
		try {
			this.peerAddress = socketChannel.socket().getInetAddress();
			this.peerPort = socketChannel.socket().getPort();
			this.socketChannel = socketChannel;
			super.mySock = socketChannel.socket();
			// messages that we write out to him.
			nioParser = new NioPipelineParser(sipStack, this,
					this.sipStack.getMaxMessageSize());
			this.peerProtocol = nioTcpMessageProcessor.transport;
			lastActivityTimeStamp = System.currentTimeMillis();
			super.key = MessageChannel.getKey(peerAddress, peerPort, nioTcpMessageProcessor.transport);

            myAddress = nioTcpMessageProcessor.getIpAddress().getHostAddress();
            myPort = nioTcpMessageProcessor.getPort();

		} finally {
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				logger.logDebug("Done creating NioTcpMessageChannel " + this + " socketChannel = " +socketChannel);
			}
		}

	}

	public NioTcpMessageChannel(InetAddress inetAddress, int port,
			SIPTransactionStack sipStack,
			NioTcpMessageProcessor nioTcpMessageProcessor) throws IOException {
		super(sipStack);
		if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
			logger.logDebug("NioTcpMessageChannel::NioTcpMessageChannel: "
				+ inetAddress.getHostAddress() + ":" + port);
		}
		try {
			messageProcessor = nioTcpMessageProcessor;
			// Take a cached socket to the destination, if none create a new one and cache it
			socketChannel = nioTcpMessageProcessor.nioHandler.createOrReuseSocket(
					inetAddress, port);
			peerAddress = socketChannel.socket().getInetAddress();
			peerPort = socketChannel.socket().getPort();
			super.mySock = socketChannel.socket();
			peerProtocol = getTransport();
			nioParser = new NioPipelineParser(sipStack, this,
					this.sipStack.getMaxMessageSize());
			NIOHandler nioHandler = nioTcpMessageProcessor.nioHandler;
			nioHandler.putMessageChannel(socketChannel, this);
			lastActivityTimeStamp = System.currentTimeMillis();
			super.key = MessageChannel.getKey(peerAddress, peerPort, getTransport());

            myAddress = nioTcpMessageProcessor.getIpAddress().getHostAddress();
            myPort = nioTcpMessageProcessor.getPort();


		} finally {
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				logger.logDebug("NioTcpMessageChannel::NioTcpMessageChannel: Done creating NioTcpMessageChannel "
						+ this + " socketChannel = " + socketChannel);
			}
		}
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	@Override
	protected void close(boolean removeSocket, boolean stopKeepAliveTask) {
		try {
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				logger.logDebug("Closing NioTcpMessageChannel "
						+ this + " socketChannel = " + socketChannel);
			}
			NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;
			nioHandler.removeMessageChannel(socketChannel);
			if(socketChannel != null) {
				socketChannel.close();
			}
			if(nioParser != null) {
				nioParser.close();
			}
			this.isRunning = false;
			if(removeSocket) {
				if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
					logger.logDebug("Removing NioTcpMessageChannel "
							+ this + " socketChannel = " + socketChannel);
				}
				((NioTcpMessageProcessor) this.messageProcessor).nioHandler.removeSocket(socketChannel);
				((ConnectionOrientedMessageProcessor) this.messageProcessor).remove(this);
			}
			if(stopKeepAliveTask) {
				cancelPingKeepAliveTimeoutTaskIfStarted();
			}
		} catch (IOException e) {
			logger.logError("Problem occured while closing", e);
		}

	}

	/**
	 * get the transport string.
	 * 
	 * @return "tcp" in this case.
	 */
	public String getTransport() {
		return this.messageProcessor.transport;
	}

	/**
	 * Send message to whoever is connected to us. Uses the topmost via address
	 * to send to.
	 * 
	 * @param msg
	 *            is the message to send.
	 * @param isClient
	 */
	protected void sendMessage(byte[] msg, boolean isClient) throws IOException {

		if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
			logger.logDebug("sendMessage isClient  = " + isClient + " this = " + this);
		}
		lastActivityTimeStamp = System.currentTimeMillis();
		
		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;
		if(this.socketChannel != null && this.socketChannel.isConnected() && this.socketChannel.isOpen()) {
			nioHandler.putSocket(NIOHandler.makeKey(this.peerAddress, this.peerPort), this.socketChannel);
		}
		sendTCPMessage(msg, this.peerAddress, this.peerPort, isClient);
	}
	
	/**
	 * Send a message to a specified address.
	 * 
	 * @param message
	 *            Pre-formatted message to send.
	 * @param receiverAddress
	 *            Address to send it to.
	 * @param receiverPort
	 *            Receiver port.
	 * @throws IOException
	 *             If there is a problem connecting or sending.
	 */
	public void sendMessage(byte message[], InetAddress receiverAddress,
			int receiverPort, boolean retry) throws IOException {
		sendTCPMessage(message, receiverAddress, receiverPort, retry);
	}
	/**
	 * Send a message to a specified address.
	 * 
	 * @param message
	 *            Pre-formatted message to send.
	 * @param receiverAddress
	 *            Address to send it to.
	 * @param receiverPort
	 *            Receiver port.
	 * @throws IOException
	 *             If there is a problem connecting or sending.
	 */
	public void sendTCPMessage(byte message[], InetAddress receiverAddress,
			int receiverPort, boolean retry) throws IOException {
		if (message == null || receiverAddress == null) {
			logger.logError("receiverAddress = " + receiverAddress);
			throw new IllegalArgumentException("Null argument");
		}
		lastActivityTimeStamp = System.currentTimeMillis();

		if (peerPortAdvertisedInHeaders <= 0) {
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				logger.logDebug("receiver port = " + receiverPort
						+ " for this channel " + this + " key " + key);
			}
			if (receiverPort <= 0) {
				// if port is 0 we assume the default port for TCP
				this.peerPortAdvertisedInHeaders = 5060;
			} else {
				this.peerPortAdvertisedInHeaders = receiverPort;
			}
			if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
				logger.logDebug("2.Storing peerPortAdvertisedInHeaders = "
						+ peerPortAdvertisedInHeaders + " for this channel "
						+ this + " key " + key);
			}
		}
		NIOHandler nioHandler = ((NioTcpMessageProcessor) messageProcessor).nioHandler;
		
		SocketChannel sock = nioHandler.sendBytes(this.messageProcessor
					.getIpAddress(), receiverAddress, receiverPort, this.messageProcessor.transport,
					message, retry, this);

		if (sock != socketChannel && sock != null) {
			if (socketChannel != null) {
				if (logger.isLoggingEnabled(LogWriter.TRACE_WARN)) {
					logger
							.logWarning("[2] Old socket different than new socket on channel "
									+ key + socketChannel + " " + sock);
					logger.logStackTrace();
					logger.logWarning("Old socket local ip address "
							+ socketChannel.socket().getLocalSocketAddress());
					logger.logWarning("Old socket remote ip address "
							+ socketChannel.socket().getRemoteSocketAddress());
					logger.logWarning("New socket local ip address "
							+ sock.socket().getLocalSocketAddress());
					logger.logWarning("New socket remote ip address "
							+ sock.socket().getRemoteSocketAddress());
				}
				close(false, false); // we can call socketChannel.close() directly but we better use the inherited method
				
				socketChannel = sock;
				nioHandler.putMessageChannel(socketChannel, this);
				
				onNewSocket(message);
			}
			
			if (socketChannel != null) {
				if (logger.isLoggingEnabled(LogWriter.TRACE_WARN)) {
					logger
					.logWarning("There was no exception for the retry mechanism so we keep going "
							+ key);
				}
			}
			socketChannel = sock;
		}

	}

	public void onNewSocket(byte[] message) {

	}
	

	/**
	 * Exception processor for exceptions detected from the parser. (This is
	 * invoked by the parser when an error is detected).
	 * 
	 * @param sipMessage
	 *            -- the message that incurred the error.
	 * @param ex
	 *            -- parse exception detected by the parser.
	 * @param header
	 *            -- header that caused the error.
	 * @throws ParseException
	 *             Thrown if we want to reject the message.
	 */
	public void handleException(ParseException ex, SIPMessage sipMessage,
			Class hdrClass, String header, String message)
			throws ParseException {
		if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG))
            logger.logDebug("Parsing Exception: " , ex);
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

					try {
						socketChannel.close();

					} catch (IOException ie) {
						if (logger.isLoggingEnabled(LogWriter.TRACE_ERROR)) {
							logger.logError("Exception while closing socket! :"
									+ socketChannel.toString() + ":" + ie.toString());
						}

					}
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
	 * @param other
	 *            is the other object to compare ourselves to for equals
	 */

	public boolean equals(Object other) {

		if (!this.getClass().equals(other.getClass()))
			return false;
		else {
			NioTcpMessageChannel that = (NioTcpMessageChannel) other;
			if (this.socketChannel != that.socketChannel)
				return false;
			else
				return true;
		}
	}

	/**
	 * TCP Is not a secure protocol.
	 */
	public boolean isSecure() {
		return false;
	}
	
	public long getLastActivityTimestamp() {
		return lastActivityTimeStamp;
	}
	
	public void triggerConnectFailure() {
        //alert of IOException to pending Data TXs
		Queue<PendingData> failedMsgs = queue;
		resetQueue();
        while (failedMsgs != null && failedMsgs.peek() != null ) {
            PendingData pData = failedMsgs.poll();
            
            SIPTransaction transaction = sipStack.findTransaction(pData.txId, false);
            if (transaction != null) {
                if (transaction instanceof SIPClientTransaction) {
                	//8.1.3.1 Transaction Layer Errors
                    if (transaction.getRequest() != null &&
                            !transaction.getRequest().getMethod().equalsIgnoreCase("ACK"))
                    {
                        SIPRequest req = (SIPRequest) transaction.getRequest();
                        SIPResponse unavRes = req.createResponse(SERVICE_UNAVAILABLE, "Transport error sending request.");
                        try {
                                this.processMessage(unavRes);
                        } catch (Exception e) {
                            if (logger.isLoggingEnabled(LogWriter.TRACE_DEBUG)) {
                                logger.logDebug("failed to report transport error", e);
                            }
                        }
                    }
                } else {
                	//17.2.4 Handling Transport Errors
                    transaction.raiseIOExceptionEvent();
                }
            }
        }
	}

}
