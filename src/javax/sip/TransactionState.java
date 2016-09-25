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
 * File Name     : TransactionState.java
 * Author        : Phelim O'Doherty
 *
 *  HISTORY
 *  Version   Date      Author              Comments
 *  1.1     08/10/2002  Phelim O'Doherty    Initial version
 *  1.2     19/05/2005  Phelim O'Doherty    Added equals and hashcode method
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

package javax.sip;



import java.io.*;



/**

 * This class contains the enumerations that define the underlying state of an 
 * existing transaction. SIP defines four types of 

 * transactions, these are Invite Client transactions, Invite Server transactions,

 * Non-Invite Client transactions and Non-Invite Server transactions.

 *

 * There are six explicit states for the various transactions, namely:

 * <ul>

 * <li> <b>Calling:</b> 

 * <ul>

 * <li> Invite Client transaction: The initial state, "calling", MUST be entered 

 * when the application initiates a new client transaction with an INVITE request.

 * </ul>

 * <li> <b>Trying:</b> 

 * <ul>

 * <li> Non-Invite Client transaction: The initial state "Trying" is entered 

 * when the application initiates a new client transaction with a request.  

 * <li> Non-Invite Server transaction: The initial state "Trying" is entered 

 * when the application is passed a request other than INVITE or ACK. 

 * </ul>

 * <li> <b>Proceeding:</b> 

 * <ul>

 * <li> Invite Client transaction: If the client transaction receives a 

 * provisional response while in the "Calling" state, it transitions to the 

 * "Proceeding" state. 

 * <li> Non-Invite Client transaction: If a provisional response is received 

 * while in the "Trying" state, the client transaction SHOULD move to the 

 * "Proceeding" state.  

 * <li> Invite Server transaction: When a server transaction is constructed for 

 * a request, it enters the initial state "Proceeding".  

 * <li> Non-Invite Server transaction: While in the "Trying" state, if the 

 * application passes a provisional response to the server transaction, the 

 * server transaction MUST enter the "Proceeding" state.

 * </ul>

 * <li> <b>Completed:</b> The "Completed" state exists to buffer any additional 

 * response retransmissions that may be received, which is why the client 

 * transaction remains there only for unreliable transports.

 * <ul>

 * <li> Invite Client transaction: When in either the "Calling" or "Proceeding" 

 * states, reception of a response with status code from 300-699 MUST cause the 

 * client transaction to transition to "Completed".

 * <li> Non-Invite Client transaction: If a final response (status codes 

 * 200-699) is received while in the "Trying" or "Proceeding" state, the client 

 * transaction MUST transition to the "Completed" state.

 * <li> Invite Server transaction: While in the "Proceeding" state, if the 

 * application passes a response with status code from 300 to 699 to the server 

 * transaction, the state machine MUST enter the "Completed" state. 

 * <li>Non-Invite Server transaction: If the application passes a final response 

 * (status codes 200-699) to the server while in the "Proceeding" state, the

 * transaction MUST enter the "Completed" state.

 * </ul>

 * <li> <b>Confirmed:</b> The purpose of the "Confirmed" state is to absorb any 

 * additional ACK messages that arrive, triggered from retransmissions of the 

 * final response. Once this time expires the server MUST transition to the 

 * "Terminated" state.

 * <ul>

 * <li> Invite Server transaction: If an ACK is received while the server 

 * transaction is in the "Completed" state, the server transaction MUST 

 * transition to the "Confirmed" state.

 * </ul>

 * <li> <b>Terminated:</b> The transaction MUST be available for garbage collection 

 * the instant it enters the "Terminated" state.

 * <ul>

 * <li> Invite Client transaction:  When in either the "Calling" or "Proceeding" 

 * states, reception of a 2xx response MUST cause the client transaction to 

 * enter the "Terminated" state. If amount of time that the server transaction 

 * can remain in the "Completed" state when unreliable transports are used 

 * expires while the client transaction is in the "Completed" state, the client 

 * transaction MUST move to the "Terminated" state. 

 * <li> Non-Invite Client transaction: If the transaction times out while the 

 * client transaction is still in the "Trying" or "Proceeding" state, the client 

 * transaction SHOULD inform the application about the timeout, and then it 

 * SHOULD enter the "Terminated" state. If the response retransmissions buffer 

 * expires while in the "Completed" state, the client transaction MUST transition 

 * to the "Terminated" state.

 * <li> Invite Server transaction: If in the "Proceeding" state, and the application 

 * passes a 2xx response to the server transaction, the server transaction MUST 

 * transition to the "Terminated" state. When the server transaction abandons 

 * retransmitting the response while in the "Completed" state, it implies that 

 * the ACK was never received.  In this case, the server transaction MUST 

 * transition to the "Terminated" state, and MUST indicate to the TU that a 

 * transaction failure has occurred.

 * <li> Non-Invite Server transaction: If the request retransmissions buffer 

 * expires while in the "Completed" state, the server transaction MUST transition 

 * to the "Terminated" state.

 * </ul>

 * </ul>

 * 

 * For each specific transaction state machine, refer to 

 * <a href = "http://www.ietf.org/rfc/rfc3261.txt">RFC3261</a>.

 *

 * @author BEA Systems, NIST
 * @version 1.2

 */
public enum TransactionState{
    NULL_STATE(-1),
    // This constant value indicates the internal value of the "Calling" 
    CALLING(0),
    // This constant value indicates the internal value of the "Trying" 
    TRYING(1),
    // This constant value indicates the internal value of the "Proceeding" 
    PROCEEDING(2),
    // This constant value indicates the internal value of the "Completed" 
    COMPLETED(3),
    // This constant value indicates the internal value of the "Confirmed" 
    CONFIRMED(4),
    // This constant value indicates the internal value of the "Terminated" 
    TERMINATED(5);
    
    // Internal Variable
    private int intValue = -1;
    
    private TransactionState(int value){
        intValue = value;
    }
    
    /**
     * This method return a integer value of this transaction state
     * @return The integer value of the transaction state
     */
    public int getValue(){
        return intValue;
    }
    
    /**
     * This method return a DialogState value of this dialog state
     * @return The DialogState value of the dialog state
     */
    public static TransactionState valueOf(int value){
        TransactionState ret = NULL_STATE;
        for(TransactionState s: TransactionState.values()){
            if (s.getValue() == value){
                ret = s;
            }
        }
        return ret;
    }
}