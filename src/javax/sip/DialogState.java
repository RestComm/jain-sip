/**
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Unpublished - rights reserved under the Copyright Laws of the United States.
 * Copyright � 2003 Sun Microsystems, Inc. All rights reserved.
 * Copyright � 2005 BEA Systems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties. 
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Module Name   : JSIP Specification
 * File Name     : DialogState.java
 * Author        : Phelim O'Doherty
 *
 *  HISTORY
 *  Version   Date      Author              Comments
 *  1.1     08/10/2002  Phelim O'Doherty    Initial version
 *  1.2     07/07/2005  Phelim O'Doherty    Deprecated the completed state.
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

package javax.sip;

import java.io.*;

/**
 * This class contains the enumerations that define the underlying state of an 
 * existing dialog. 
 *
 * There are three explicit states for a dialog, namely:
 * <ul>
 * <li> Early - A dialog is in the "early" state, which occurs when it is 
 * created when a provisional response is recieved to the INVITE Request.
 * <li> Confirmed - A dialog transitions to the "confirmed" state when a 2xx 
 * final response is received to the INVITE Request.
 * <li> Terminated - A dialog transitions to the "terminated" state for all 
 * other reasons or if no response arrives at all on the dialog.
 * </ul>
 *
 * @author BEA Systems, NIST 
 * @version 1.2
 */
public enum DialogState{
    // Initial Value
    NULL_STATE(-1),
    // This constant value indicates the internal value of the "Early". 
    EARLY(0),
    // This constant value indicates that the dialog state is "Confirmed".
    CONFIRMED(1),
    /**
     * This constant value indicates that the dialog state is "Completed".
     * @deprecated Since v1.2. This state does not exist in a dialog.
     */
    COMPLETED(2),
    // This constant value indicates the internal value of the "Terminated". 
    TERMINATED(3);
    
    // internal variable
    private int intValue = -1;
    
    /*
     * This method is private method for setting value of each Dialog State.
     */
    private DialogState(int value){
        intValue = value;
    }
    
    /**
     * This method return a integer value of this dialog state
     * @return The integer value of the dialog state
     */
    public int getValue(){
        return intValue;
    }
    
    /**
     * This method return a DialogState value of this dialog state
     * @return The DialogState value of the dialog state
     */
    public static DialogState valueOf (int value){
        DialogState ret = NULL_STATE;
        for (DialogState s : DialogState.values()){
            if (s.getValue() == value){
                ret = s;
            }
        }
        return ret;
    }
}




















