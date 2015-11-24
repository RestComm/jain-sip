/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), and others.
* This software is has been contributed to the public domain.
* As a result, a formal license is not needed to use the software.
*
* This software is provided "AS IS."
* NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
*
*/
/*
 * Created on Nov 23, 2015
 *
 *The JAIN-SIP Project
 */
package test.unit.gov.nist.javax.sip.parser;

import gov.nist.javax.sip.parser.SubscriptionStateParser;

/**
 *
 */
public class SubscriptionStateParserTest extends ParserTestCase {

    /* (non-Javadoc)
     * @see test.unit.gov.nist.javax.sip.parser.ParserTestCase#testParser()
     */
    public void testParser() {

        String content[] = {
            //test different subs states
            //test with expected params, and generic ones
            //test different param order
            "Subscription-State: terminated;reason=SIP ;text=\"Call Expired\"\n",
            "Subscription-State: active \n",
            "Subscription-State: pending;retry-after=4500 ;text=\"Call Expired\"\n",
            "Subscription-State: terminated;text=\"Call Expired\";retry-after=1000 ;expires=3600 ; reason=SIP \n"
            
        };

        super.testParser(SubscriptionStateParser.class,content);

    }

}
