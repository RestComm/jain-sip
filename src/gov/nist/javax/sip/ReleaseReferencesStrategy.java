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
/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
 *******************************************************************************/

package gov.nist.javax.sip;

/**
 * This Enum defines the multiple strategies the stack can use to clean 
 * more or less aggressively the references in order to win both memory 
 * and CPU cycles.
 * 
 * <ul>
 * <li>None: This is the default. It keeps references to request and responses as part of transactions and dialogs. This consumes the most amount of memory usage</li>
 * <li>Normal: It removes references to request and responses in transactions and dialogs in cleaning them up but keep byte arrays of requests or responses to be able to reparse them if the application need them. This gives a 2x improvement in memory as opposed to None</li>
 * <li>Aggressive: It removes references to request and responses in transactions and dialogs in cleaning them up and don't allow reparsing. Need careful application design. This gives a further improvements in memory as opposed to Normal Strategy and also CPU improvements.</li>
 * </ul>
 * 
 * @author jean.deruelle@telestax.com
 *
 */
public enum ReleaseReferencesStrategy {
	None,
	Normal,
	Aggressive
}
