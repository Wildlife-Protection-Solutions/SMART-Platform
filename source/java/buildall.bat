SET version=8.0.0


for /f %%i in ('date /t') do set RESULT=%%i
echo The directory is %RESULT%

SET outputlocation=C:\data\SMART\Builds\SMART8\%RESULT%
echo %outputlocation%
MKDIR %outputlocation%


REM - I had to do this because maven won't work if these directories/files exist

DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.paws.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.r.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.qa.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.qa.er.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.qa.patrol.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.i2.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.i2.patrol.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.i2.migrate.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.i2.connect.dataqueue.i2.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.event.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.event.i2.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.er.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.er.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.er.report.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.entity.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.entity.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.entity.report.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.cybertracker.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.cybertracker.incident.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.cybertracker.patrol.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.cybertracker.plan.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.cybertracker.survey.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.smartcollect.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.cybertracker.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.dataqueue.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.dataqueue.patrol.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.dataqueue.independentincident.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.dataqueue.er.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.dataqueue.cybertracker.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.asset.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.asset.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.birt.map.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.dataentry.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.help.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.independentincident.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.observation.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.observation.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.p2.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.patrol.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.patrol.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.plan.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.report.birt.query.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.report.nl\8.0.0
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.reporttable.nl\8.0.0



REM run maven to build packages
REM set PATH=C:\Java\jdk-11.0.2\bin;%PATH%
call mvn clean install -Pallplatforms,product,update,core,plugins,utils,languagepacks
REM call mvn clean install -Pallplatforms,product,update,core,plugins
REM call mvn install -Pallplatforms,migrationtools

del %outputlocation%\smartapp-win32.win32.x86_64.zip
copy .\org.wcs.smart-product\target\products\smartapp-win32.win32.x86_64.zip %outputlocation%

del %outputlocation%\smartapp-macosx.cocoa.x86_64.zip
copy .\org.wcs.smart-product\target\products\smartapp-macosx.cocoa.x86_64.zip %outputlocation%

del %outputlocation%\smartapp-linux.gtk.x86_64.zip
copy .\org.wcs.smart-product\target\products\smartapp-linux.gtk.x86_64.zip %outputlocation%

del %outputlocation%\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.updatesite\target\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip %outputlocation%

del %outputlocation%\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.utils.updatesite\target\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip %outputlocation%

del %outputlocation%\org.wcs.smart.migrationtools.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.migrationtools.updatesite\target\org.wcs.smart.migrationtools.updatesite-%version%-SNAPSHOT.zip %outputlocation%

move %outputlocation%\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip %outputlocation%\site_%version%.zip
move %outputlocation%\org.wcs.smart.migrationtools.updatesite-%version%-SNAPSHOT.zip %outputlocation%\migrationtools_%version%.zip
move %outputlocation%\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip %outputlocation%\smartutils_%version%.zip

@RD /s /q %outputlocation%\smartapp-win32.win32.x86_64

powershell Expand-Archive %outputlocation%\smartapp-win32.win32.x86_64.zip %outputlocation%\smartapp-win32.win32.x86_64

del %outputlocation%\smartapp-win32.win32.x86_64\SMARTc.exe
mkdir %outputlocation%\smartapp-win32.win32.x86_64\updatesite
copy %outputlocation%\site_%version%.zip %outputlocation%\smartapp-win32.win32.x86_64\updatesite
copy %outputlocation%\migrationtools_%version%.zip %outputlocation%\smartapp-win32.win32.x86_64\updatesite

REM complete - start smart and install plugins/restore backup
