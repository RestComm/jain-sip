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
import gov.nist.core.StackLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;

public class NioWebSocketMessageProcessor extends NioTcpMessageProcessor {

    private static StackLogger logger = CommonLogger.getLogger(NioWebSocketMessageProcessor.class);
    
    public NioWebSocketMessageProcessor(InetAddress ipAddress,
			SIPTransactionStack sipStack, int port) {
		super(ipAddress, sipStack, port);		
		transport = "WS"; // by default its WS, can be overriden if there is TLS acclereator
	}
	
	@Override
	public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor, SocketChannel client) throws IOException {
		NioWebSocketMessageChannel retval = (NioWebSocketMessageChannel) nioHandler.getMessageChannel(client);
		if (retval == null) {
			retval = new NioWebSocketMessageChannel(sipStack,nioTcpMessageProcessor,
					client);
			
			nioHandler.putMessageChannel(client, retval);
		}
		return retval;
	}
        
    @Override        
    ConnectionOrientedMessageChannel constructMessageChannel(InetAddress targetHost, int port) throws IOException {
        return new NioWebSocketMessageChannel(targetHost,
                            port, sipStack, this);
    }

}
