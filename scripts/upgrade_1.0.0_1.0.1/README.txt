Follow the steps below to upgrade your SMART database from Version 1.0.0 to Version 1.0.5.

1. Backup your current database by running SMART, logging into one of your conservation areas, and using the 'File->Backup System' menu.

You can use whatever backup file name you'd like but for these instructions we will assume the backup file name is called:
C:\temp\SMART_XXX.bak.zip

2. In your OS file system unzip this backup to a temporary location:
C:\temp\SMART_XXX.bak\

3. Edit the versionCheck.sql in a text editor and change the first line from:

connect 'jdbc:derby:SMARTDB;user=smart_admin;password=smart_derby';
TO
connect 'jdbc:derby:C:\temp\SMART_XXX.bak\smartdb;user=smart_admin;password=smart_derby';

Note: If you extract your backup to a different location you will need to put that location in place of C:\temp\SMART_XXX.bak.  Don't forget the smartdb - this is the actual embedded apache database.

4. Edit the smart_1.0.5.sql file and make the same change you made to the versionCheck.sql file 

5. From the command line run versionCheck.bat in Windows or versionCheck.sh in MacOSX 

You should see the following output:

>>>>>>>
ij> connect 'jdbc:derby:C:\temp\SMART_XXX.bat\smartdb;user=smart_admin;password=smart_derby';
ij> select * from smart.db_version;
VERSION
---------------
1.0
<<<<<<

IF YOU DO NOT SEE 1.0 DO NOT CONTINUE.  You will need to contact your administrator as your database cannot be upgraded.

6. From the command line run update.bat (Windows) or update.sh (MacOSX)
You will see a selection of sql commands pass through the window.

7.  Re-run versionCheck.bat or versionCheck.sh again.  This time you should see version 1.0.5.  

>>>>>>>
ij> connect 'jdbc:derby:C:\temp\SMART_XXX.bat\smartdb;user=smart_admin;password=smart_derby';
ij> select * from smart.db_version;
VERSION
---------------
1.0.5
<<<<<<

IF YOU NOT DO SEE VERSION 1.0.5 DO NOT CONTINUE.  You will need to contact your administrator as your database was not upgraded for some reason.  Have the original backup file available and any errors you see.

8.  Zip up the smartdb and filestore directories in your temporary location into a new zip file.
C:\temp\SMART_XXX.bak\SMART_XXX_1.0.5.bak.zip.

This zipped up backup file should have the following contents:
/filestore/
/smartdb/

9.  This new zip file can now be used in SMART 1.0.5.  Open the SMART 1.0.5 application, Pick 'Restore From Backup' from the 'advanced' login link.