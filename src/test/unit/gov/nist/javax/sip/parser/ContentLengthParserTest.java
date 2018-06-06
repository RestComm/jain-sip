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
 * Created on Jul 27, 2004
 *
 *The Open SLEE project
 */
package test.unit.gov.nist.javax.sip.parser;
import gov.nist.javax.sip.parser.*;

/**
 *
 */
public class ContentLengthParserTest extends ParserTestCase {

    public void testParser(){

        String content[] = {
            "l: 345\n",
            "Content-Length: 3495\n",
            "Content-Length: 0 \n"
                };

        super.testParser(ContentLengthParser.class,content);

    }

    // https://github.com/RestComm/jain-slee/issues/111
    public void testNegativeContentLength(){
        String error = "";
        try {
            final String header = "Content-Length: -123 \n";
            System.out.print(header);
            createParser(ContentLengthParser.class, header).parse();
        } catch (java.text.ParseException ex) {
            System.out.println(ex.getMessage());
            error = ex.getLocalizedMessage();
        }
        assertTrue(error.contains("the contentLength parameter is <0"));
    }
}
