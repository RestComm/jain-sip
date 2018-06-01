package gov.nist.javax.sip.header.ims;

import gov.nist.javax.sip.header.SIPHeaderList;

/**
 * A list of PAccessNetworkInfo headers (there can be multiple in a message).
 *
 * @author M. Hoan Luu hoan.h.luu@telestax.com  <br/>
 *
 */
public class PAccessNetworkInfoList extends SIPHeaderList<PAccessNetworkInfo> {

    private static final long serialVersionUID = 6993762829214108619L;

    public PAccessNetworkInfoList() {
        super(PAccessNetworkInfo.class, PAccessNetworkInfoHeader.NAME);
    }

    @Override
    public Object clone() {
        PAccessNetworkInfoList retVal = new PAccessNetworkInfoList();
        return retVal.clonehlist(this.hlist);
    }
}
