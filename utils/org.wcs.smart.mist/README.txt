This project uses Maven.

To build in a eclipse project:
1.  run "mvn eclipse:eclipse"
2.  then run "mvn compile"

The mvn compile command will download both the firebird database and a 32 bit java jre 
from the Refractions internal network.  If you do not have access to the Refractions 
internal network you need to manually download a 32 bit windows jre, unpack it and put 
the results in the jre_win32 folder.

Firebird is downloaded directly from the firebird website.

To package into a distribute:
run "mvn package" 

This will create a directory in the target folder that contains all files (including
the jre) and a bat file to run the program.  The BAT file is required because
firebird requires the Firebird application to be on the PATH.
  