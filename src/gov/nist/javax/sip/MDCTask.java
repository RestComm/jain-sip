package gov.nist.javax.sip;

import java.util.Map;

/**
 * 
 * This interface allows to provide more MDC vars in addition to the affinity
 * object. 
 *
 */
public interface MDCTask extends ThreadAffinityTask{
    Map<String,String> getMDCVars();
}
