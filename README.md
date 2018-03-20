[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fjain-sip.svg?type=shield)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fjain-sip?ref=badge_shield)


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

Documentation
-------------

If you're looking for some documentation for the Restcomm JAIN-SIP project, you might want to head over to: https://github.com/RestComm/jain-sip.docs


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

Credits
--------

Architecture / API design:
-------------------------

JAIN-SIP: Joint Spec Leads -- Phelim O'Doherty (BEA) and M. Ranganathan (NIST). 
JAIN-SDP: The SDP API spec lead is Kelvin Porter from Cisco.

Sample Sequence diagram on how a request is processed:

<img src='http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/RestComm/jain-sip/master/docs/uml/udp-request-processing-sequence-diagram.txt'>

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

The implementation is public domain although the API itself isn't. 
See the license directory in this distribution for definitive information.

----------------------------------------------------------------------------

Substantial input by early adopters and fearless users.

See List of Contributions at:

file:./www/README.html

----------------------------------------------------------------------------


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fjain-sip.svg?type=large)](https://app.fossa.io/projects/git%2Bhttps%3A%2F%2Fgithub.com%2FRestComm%2Fjain-sip?ref=badge_large)
