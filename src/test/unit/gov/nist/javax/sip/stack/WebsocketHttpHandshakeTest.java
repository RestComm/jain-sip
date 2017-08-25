package test.unit.gov.nist.javax.sip.stack;

import gov.nist.javax.sip.stack.WebSocketHttpHandshake;
import junit.framework.TestCase;
/**
 *
 */
public class WebsocketHttpHandshakeTest extends TestCase {

	public void testRegularDigest() throws Exception {

		String secWebSocketKey = "dGhlIHNhbXBsZSBub25jZQ==";
                String computed = WebSocketHttpHandshake.computeRev13Response(secWebSocketKey);
                assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", computed);
                
	}
}
