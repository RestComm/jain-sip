/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.unit.gov.nist.javax.sip.parser;

import gov.nist.core.Host;
import junit.framework.TestCase;
/**
 *
 * @author HoanHL
 */
public class HostTest extends TestCase{
    
    private final static String validIPv6Address[] = { 
        "3ffe::c31c",
        "[3ffe::c31c]",
        "[3ffe:33:0:0:202:2dff:fe32:c31c]",
        "3ffe::c31c%4",
        "[3ffe::c31c%4]",
        "[3ffe:33:0:0:202:2dff:fe32:c31c%4]"
    };
    
    private final static String expectedIPv6Address[] = {
        //gov.nist.core.STRIP_ADDR_SCOPES = true
        "3ffe:0:0:0:0:0:0:c31c",
        "[3ffe:0:0:0:0:0:0:c31c]",
        "[3ffe:33:0:0:202:2dff:fe32:c31c]",
        //gov.nist.core.STRIP_ADDR_SCOPES = false
        "3ffe:0:0:0:0:0:0:c31c%4",
        "[3ffe:0:0:0:0:0:0:c31c%4]",
        "[3ffe:33:0:0:202:2dff:fe32:c31c%4]"
    };
    
    public void testHost() {
    	for (int i = 0; i < validIPv6Address.length; i++) {
            String hostName = validIPv6Address[i];
            System.out.println("hostName=" + hostName);
            Host host = new Host(hostName);
            String ipAddress = host.getIpAddress();
            System.out.println(ipAddress);
            if (ipAddress == null || !ipAddress.equals(expectedIPv6Address[i])) {
                fail("Expected is: " + expectedIPv6Address[i] + " but Actual is: " + ipAddress);
            }
        }
    }
    
}
