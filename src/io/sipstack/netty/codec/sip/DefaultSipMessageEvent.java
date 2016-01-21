/**
 * 
 */
package io.sipstack.netty.codec.sip;

import gov.nist.javax.sip.message.SIPMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public class DefaultSipMessageEvent implements SipMessageEvent {

    private final Connection connection;
    private final SIPMessage msg;
    private final long arrivalTime;

    /**
     * 
     */
    public DefaultSipMessageEvent(final Connection connection, final SIPMessage msg, final long arrivalTime) {
        this.connection = connection;
        this.msg = msg;
        this.arrivalTime = arrivalTime;
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public SIPMessage getMessage() {
        return this.msg;
    }

    @Override
    public long getArrivalTime() {
        return this.arrivalTime;
    }

}
