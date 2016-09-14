package test.unit.gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.ims.PChargingVectorParser;
import test.unit.gov.nist.javax.sip.parser.ParserTestCase;

public class PChargingVectorParserTest extends ParserTestCase {
    
    public void testParser() {
        String[] preferredID =  {
                "P-Charging-Vector: icid-value=value1;orig-ioi=value2;icid-generated-at=value3\n",

                "P-Charging-Vector: icid-value=[2a02:ed0:1000:3033::249];orig-ioi=1223abc4;icid-generated-at=[2a02:ed0:1000:3033::249]\n",
                
                };
        
        super.testParser(PChargingVectorParser.class, preferredID);
    }

    public void testInvalidHeaderParam() {
        
        String preferredID =  "P-Charging-Vector: icid-value=[2a02:ed0:1000:3033::249k];orig-ioi=1223abc4;icid-generated-at=[2a02:ed0:1000:3033::249]\n";
        
        try {                  
           HeaderParser hp = createParser(PChargingVectorParser.class, preferredID);
           hp.parse();
           fail("Header:"+ preferredID + "Invalid IPv6 parsing expected to fail");
           
        } catch (java.text.ParseException ex) {} 
    }
}
