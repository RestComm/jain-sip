package gov.nist.javax.sip.stack;

import java.util.EventListener;

/**
 * Interface to injection an external SIPDialog into SIPTransaction 
 * if ID does not exist locally
 *
 * @author Benedikt Machens
 */

public interface SIPDialogInjectionListener extends EventListener {
    /**
     * 
     * @param dialogId
     * @return External SIPDialog
     */
    public SIPDialog getExternalSIPDialog(String dialogId);
}
