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
/************************************************************************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT and Telecommunications Institute (Aveiro, Portugal)  *
 ************************************************************************************************/

package gov.nist.javax.sip.parser.ims;


import java.text.ParseException;

import gov.nist.javax.sip.header.ims.PAccessNetworkInfo;
import gov.nist.javax.sip.header.ims.PAccessNetworkInfoList;
import gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;
import gov.nist.core.Token;
import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;


/**
 * P-Access-Network-Info header parser.
 *
 * <p>RFC 3455 - Private Header (P-Header) Extensions to the Session Initiation
 *   Protocol (SIP) for the 3rd-Generation Partnership Project (3GPP) </p>
 *
 * <p>Syntax (RFC 3455):</p>
 * <pre>
 * P-Access-Network-Info  = "P-Access-Network-Info" HCOLON access-net-spec
 * access-net-spec        = access-type *(SEMI access-info)
 * access-type            = "IEEE-802.11a" / "IEEE-802.11b" /
 *                          "3GPP-GERAN" / "3GPP-UTRAN-FDD" /
 *                          "3GPP-UTRAN-TDD" / "3GPP-CDMA2000" / token
 * access-info            = cgi-3gpp / utran-cell-id-3gpp / extension-access-info
 * extension-access-info  = gen-value
 * cgi-3gpp               = "cgi-3gpp" EQUAL (token / quoted-string)
 * utran-cell-id-3gpp     = "utran-cell-id-3gpp" EQUAL (token / quoted-string)
 * gen-value              = token / host / quoted-string
 * </pre>
 * 
 * <p>RFC 7913 - P-Access-Network-Info ABNF Update </p>
 * <p>Newer RFC https://tools.ietf.org/html/rfc7913</p>
 * <pre>
 *       access-info            = cgi-3gpp / utran-cell-id-3gpp /
 *                                dsl-location / i-wlan-node-id /
 *                                ci-3gpp2 / eth-location /
 *                                ci-3gpp2-femto / fiber-location /
 *                                np / gstn-location /local-time-zone /
 *                                dvb-rcs2-node-id / operator-specific-GI /
 *                                utran-sai-3gpp / extension-access-info
 *       np                     = "network-provided"
 *       extension-access-info  = generic-param
 * </pre>
 *
 * <p>RFC 7315 - Private Header (P-Header) Extensions to the Session Initiation Protocol (SIP) for the 3GPP </p>
 * <p>Syntax (RFC 7315):</p>
 *
 * <pre>
 * P-Access-Network-Info  = "P-Access-Network-Info" HCOLON
 *                                 access-net-spec *(COMMA access-net-spec)
 * access-net-spec        = (access-type / access-class)
 *                          *(SEMI access-info)
 * access-type            = "IEEE-802.11" / "IEEE-802.11a" /
 *                          "IEEE-802.11b" / "IEEE-802.11g" /
 *                          "IEEE-802.11n" /
 *                          "IEEE-802.3" / "IEEE-802.3a" /
 *                          "IEEE-802.3ab" / "IEEE-802.3ae" /
 *                          "IEEE-802.3ak" / "IEEE-802.3ah" /
 *                          "IEEE-802.3aq" / "IEEE-802.3an" /
 *                          "IEEE-802.3e" / "IEEE-802.3i" /
 *                          "IEEE-802.3j" / "IEEE-802.3u" /
 *                          "IEEE-802.3y" / "IEEE-802.3z" /
 *                          "3GPP-GERAN" /
 *                          "3GPP-UTRAN-FDD" / "3GPP-UTRAN-TDD" /
 *                          "3GPP-E-UTRAN-FDD" / "3GPP-E-UTRAN-TDD" /
 *                          "3GPP2-1X-Femto" / "3GPP2-UMB" /
 *                          "3GPP2-1X-HRPD" / "3GPP2-1X" /
 *                          "ADSL" / "ADSL2" / "ADSL2+" / "RADSL" /
 *                          "SDSL" / "HDSL" / "HDSL2" / "G.SHDSL" /
 *                          "VDSL" / "IDSL" /
 *                          "DOCSIS" / "GSTN" / "GPON" / " XGPON1" /
 *                          "DVB-RCS2" / token
 * access-class           = "3GPP-GERAN" /  "3GPP-UTRAN" /
 *                          "3GPP-E-UTRAN" / "3GPP-WLAN" /
 *                          "3GPP-GAN" / "3GPP-HSPA" /
 *                          "3GPP2" / token
 * np                     = "network-provided"
 * extension-access-info  = gen-value
 * cgi-3gpp               = "cgi-3gpp" EQUAL
 *                          (token / quoted-string)
 * utran-cell-id-3gpp     = "utran-cell-id-3gpp" EQUAL
 *                          (token / quoted-string)
 * i-wlan-node-id         = "i-wlan-node-id" EQUAL
 *                          (token / quoted-string)
 * dsl-location           = "dsl-location" EQUAL
 *                          (token / quoted-string)
 * eth-location           = "eth-location" EQUAL
 *                          (token / quoted-string)
 * fiber-location         = "fiber-location" EQUAL
 *                          (token / quoted-string)
 * ci-3gpp2               = "ci-3gpp2" EQUAL
 *                          (token / quoted-string)
 * ci-3gpp2-femto         = "ci-3gpp2-femto" EQUAL
 *                          (token / quoted-string)
 * gstn-location          = "gstn-location" EQUAL
 *                          (token / quoted-string)
 * dvb-rcs2-node-id       = "dvb-rcs2-node-id" EQUAL
 *                          quoted-string
 * local-time-zone        = "local-time-zone"  EQUAL
 *                          quoted-string
 * operator-specific-GI   = "operator-specific-GI" EQUAL
 *                          (token / quoted-string)
 * utran-sai-3gpp         = "utran-sai-3gpp" EQUAL
 *                          (token / quoted-string)
 * </pre>
 *
 * @author Miguel Freitas (IT) PT-Inovacao
 */


public class PAccessNetworkInfoParser
    extends HeaderParser
    implements TokenTypes
{

    public PAccessNetworkInfoParser(String accessNetwork) {

        super(accessNetwork);

    }


    protected PAccessNetworkInfoParser(Lexer lexer) {
        super(lexer);

    }


    public SIPHeader parse() throws ParseException
    {

        if (debug)
            dbg_enter("AccessNetworkInfoParser.parse");

        PAccessNetworkInfoList accessNetworkInfoList = new PAccessNetworkInfoList();

        try {
            headerName(TokenTypes.P_ACCESS_NETWORK_INFO);
            while (true) {
                PAccessNetworkInfo accessNetworkInfo = new PAccessNetworkInfo();
                accessNetworkInfo.setHeaderName(SIPHeaderNamesIms.P_ACCESS_NETWORK_INFO);

                this.lexer.SPorHT();
                lexer.match(TokenTypes.ID);
                Token token = lexer.getNextToken();
                accessNetworkInfo.setAccessType(token.getTokenValue());
                this.lexer.SPorHT();
                while (lexer.lookAhead(0) == ';') {
                    this.lexer.match(';');
                    this.lexer.SPorHT();

                    try {
                        NameValue nv = super.nameValue('=');
                        accessNetworkInfo.setParameter(nv);
                    } catch (ParseException e) {
                        this.lexer.SPorHT();
                        String ext = this.lexer.quotedString();
                        if (ext == null) {
                            ext = this.lexer.ttokenGenValue();
                        } else {
                            // avoids tokens such as "a=b" to be stripped of quotes and misinterpretend as
                            // RFC 7913 generic-param when re-encoded
                            ext = "\"" + ext + "\"";
                        }
                        accessNetworkInfo.setExtensionAccessInfo(ext);
                    }
                    this.lexer.SPorHT();
                }
                accessNetworkInfoList.add(accessNetworkInfo);
                this.lexer.SPorHT();
                char la = lexer.lookAhead(0);
                if (la == ',') {
                    this.lexer.match(',');
                    this.lexer.SPorHT();
                } else if (la == '\n')
                    break;
                else
                    throw createParseException("unexpected char");
            }
            return accessNetworkInfoList;
        } finally {
            if (debug)
                dbg_leave("AccessNetworkInfoParser.parse");
        }

    }




}
