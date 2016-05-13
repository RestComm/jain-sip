
JAIN-SIP 1.2 Reference Implementation
--------------------------------------
CONTENTS
-------
See docs/index.html

BUILD Notes
-----------
Platforms:
---------
You need to install J2SE JDK 1.5 or above. You can
get the SDK from http://www.javasoft.com



Dependencies
------------

1. You need to install ant and the junit extension for ant on your machine.
2. You need to have junit.jar in your classpath.
3. You need to have log4j.jar in your classpath (included in this distribution).

There are versions of the dependent libraries in the lib directory.  
For your build environment, please edit ant-build-config.properties.

YOU DO NOT need jdom.jar and ant.jar. These are strictly for buildng 
the ant tools.

Building It from Scratch
-------------------------
The distribution is pre-built but should you feel inclined to make changes,
or run the examples, you may wish to rebuild everything.

ant make 

Builds everything.


Building the TCK
----------------

Edit tck.properties and set the claspath to your implementation.

ant runtck 

(builds a jar file containing the TCK and runs it).

Look in test-reports  to see the results of your run.

Extensions
----------

IMS Headers, headers in gov.nist.javax.sip.extension and all the classes
that are suffixed with "Ext" in their name can be used without concern as
they will be included in the next generation of the API. These will not 
change as a rule.

You should refrain from using any other internal classes. These are subject
to change without notice.

----------------------------------------------------------------------------
Running the examples

Please ensure that the directory  classes  (relative to where you have
built the distribution) is included in the  classpath. Ant targets
are provided in each example directory to run the examples.

How to get Source Code Refreshes
--------------------------------

Cruise Control Snapshot
-----------------------
http://hudson.jboss.org/hudson/view/Mobicents/job/jain-sip/


----------------------------------------------------------------------------
SVN Access
----------

https://svn.java.net/svn/jsip~svn/

Please sign up as a java.net user in order to get svn access.


----------------------------------------------------------------------------

Credits
--------

Architecture / API design:
-------------------------

JAIN-SIP: Joint Spec Leads -- Phelim O'Doherty (BEA) and M. Ranganathan (NIST). 
JAIN-SDP: The SDP API spec lead is Kelvin Porter from Cisco.

Sample Sequence diagram on how a request is processed:

<img src='http://g.gravizo.com/g?%40startuml%3B%0Aautonumber%3B%0Aactor%20UAC%3B%0Aboundary%20UDPMessageProcessor%3B%0Acontrol%20UDPMessageChannel%3B%0Acontrol%20NISTSIPMessageHandImpl%3B%0Acontrol%20EventScanner%3B%0Alegend%20left%3B%0A%20%20Handling%20incoming%20UDP%20request%3B%0Aendlegend%3B%0Anote%20right%20of%20UDPMessageProcessor%20%23aqua%3B%0A%09NIST%20SIP%20abstraction%20that%20maps%20to%20SIPListener%3B%0Aend%20note%3B%0Anote%20right%20of%20NISTSIPMessageFactoryImpl%20%23aqua%3B%0A%09Created%20on%20stack%20init%3B%0Aend%20note%3B%0AUAC%20-%3E%20UDPMessageProcessor%20%3A%20INVITE%3B%0Acreate%20UDPMessageChannel%3B%0AUDPMessageProcessor%20-%3E%20UDPMessageChannel%3B%0Aactivate%20UDPMessageChannel%3B%0AUDPMessageChannel%20-%3E%20StringMsgParser%20%3A%20parseSIPMessage%3B%0AUDPMessageChannel%20-%3E%20SIPTransactionStack%3A%20nesSIPServerRequest%3B%0Aactivate%20SIPTransactionStack%3B%0ASIPTransactionStack%20-%3E%20SIPTransactionStack%20%3A%20createTransaction%3B%0ASIPTransactionStack%20-%3E%20NISTSIPMessageFactoryImpl%20%3A%20newSIPServerRequest%3B%0Acreate%20NISTSIPMessageHandImpl%3B%0Adeactivate%20SIPTransactionStack%3B%0AUDPMessageChannel%20-%3E%20NISTSIPMessageHandImpl%20%3A%20processRequest%3B%0Aactivate%20NISTSIPMessageHandImpl%3B%0ANISTSIPMessageHandImpl%20%20-%3E%20SIPTransactionStack%20%3A%20getDialog%3B%0ANISTSIPMessageHandImpl%20%20-%3E%20EventScanner%20%3A%20deliverEvent%3B%0Aactivate%20EventScanner%3B%0AEventScanner%20-%3E%20SIPListener%20%3A%20processRequest%3B%0Adeactivate%20EventScanner%3B%0Adeactivate%20NISTSIPMessageHandImpl%3B%0Adeactivate%20UDPMessageChannel%3B%0A%40enduml'>

Implementation Lead:
---------------------
"M. Ranganathan" <mranga@nist.gov>

Implementation Team ( version 1.2)
----------------------------------
"M. Ranganathan" <mranga@nist.gov>
"Jeroen van Bemmel" <jeroen@zonnet.nl>

TCK (version 1.2)
----------------
M. Ranganathan  <mranga@nist.gov>
Jeroen van Bemmel <jeroen@zonnet.nl>



---------------------------------------------------------------------------
LICENSE
-------

The implementation is public domain although the API itself is'nt. 
See the license directory in this distribution for definitive information.

----------------------------------------------------------------------------

Substantial input by early adopters and fearless users.

See List of Contributions at:

file:./www/README.html

----------------------------------------------------------------------------
