/*
 * JBoss, Home of Professional Open Source
 * This code has been contributed to the public domain by the author.
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
 * of the terms of this agreement.
 */
package gov.nist.javax.sip.address;

import java.text.ParseException;

import javax.sip.address.AddressFactory;

/**
 * This interface is extension to {@link javax.sip.address.AddressFactory}. It
 * declares methods which may be useful to user.
 * 
 * @author baranowb
 * @version 1.2
 */
public interface AddressFactoryEx extends AddressFactory {

	/**
	 * Creates SipURI instance from passed string. 
	 * 
	 * @param sipUri
	 *            - uri encoded string, it has form of:
	 *            <sips|sip>:username@host[:port]. NOTE: in case of IPV6, host must be
	 *            enclosed within [].
	 * @throws ParseException if the URI string is malformed. 
	 */
	public javax.sip.address.SipURI createSipURI(String sipUri) throws ParseException;

}
