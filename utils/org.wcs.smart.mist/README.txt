This project uses Maven.

To build in a eclipse project:
1a)  run "mvn eclipse:eclipse"
or
1b)  run "mvn -f pom-matcher.xml eclipse:eclipse"

2.  then run "mvn compile"

The mvn compile command will download both the firebird database and a 32 bit java jre 
from the Refractions internal network.  If you do not have access to the Refractions 
internal network you need to manually download a 32 bit windows jre, unpack it and put 
the results in the jre_win32 folder.

Firebird is downloaded directly from the firebird website.

org.wcs.smart
------------------
This project requires the org.wcs.smart project for the
xml file library.  You will need to manually create a jar
file and place it into your m2 repository:
C:\Users\XXX\.m2\repository\org\wcs\smart\smart-core\2.0.1\smart-core-2.0.1.jar


mistReader
----------------------------------------------
To package into a distribute:
run "mvn package" 

This will create a directory in the target folder that contains all files (including
the jre) and a bat file to run the program.  The BAT file is required because
firebird requires the Firebird application to be on the PATH.
  
  
mistMatcher
----------------------------------------------
To package into a distribute:
run "mvn -f pom-matcher.xml clean package" 

  