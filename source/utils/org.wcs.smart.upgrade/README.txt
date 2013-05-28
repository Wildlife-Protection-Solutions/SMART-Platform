This program provides a utility for upgrade SMART databases.  The update script works on a system backup but extracting the files, running an sql script containing upgrade statements, then re-packaging into a system backup to be restored in the new system.


To build a jar file and associated bat/sh files for delivery run the command:

1.  edit the pom.xml and change the source.db.version and target.db.verion properties to the correct version

2.  Run "mvn package"

3.  This will package up all dependencies and create associate bat and sh files in the target/SmartUpgrade directory.  You will need to copy the database upgrade script (version_XXX.sql) to this directory then it should be ready for delivery. 