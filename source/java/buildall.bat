SET version=8.0.0
SET builddate=%date:~5,2%%date:~8,2%%date:~0,4%
SET currentpath=%cd%


for /f %%i in ('date /t') do set RESULT=%%i
echo The directory is %RESULT%

SET outputlocation=C:\data\SMART\Builds\SMART8\%RESULT%

SET keystore_alias=tomcat
SET keystore_location=C:\data\SMART\Builds\keystore\connect8.refractions.net.jks
SET keystore_password=*******

REM you can change the java version if required
REM set PATH=C:\Java\jdk-11.0.2\bin;%PATH%

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
DEL /s /q C:\Users\Emily\.m2\repository\org\wcs\smart\org.wcs.smart.connect.dataqueue.i2.nl\8.0.0
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
REM call mvn clean install -Pallplatforms,product,update,core,plugins,utils,languagepacks
call mvn clean install -Pallplatforms,product,update,core,plugins,utils,eclipse-sign,languagepacks -Djarsigner.alias=%keystore_alias% -Djarsigner.storepass=%keystore_password% -Djarsigner.keystore=%keystore_location%

del %outputlocation%\smartapp-win32.win32.x86_64.zip
copy .\org.wcs.smart-product\target\products\smartapp-win32.win32.x86_64.zip %outputlocation%

del %outputlocation%\smartapp-macosx.cocoa.x86_64.zip
copy .\org.wcs.smart-product\target\products\smartapp-macosx.cocoa.x86_64.tar.gz %outputlocation%

del %outputlocation%\smartapp-linux.gtk.x86_64.zip
copy .\org.wcs.smart-product\target\products\smartapp-linux.gtk.x86_64.tar.gz %outputlocation%

del %outputlocation%\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.updatesite\target\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip %outputlocation%

del %outputlocation%\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.utils.updatesite\target\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip %outputlocation%

del %outputlocation%\org.wcs.smart.translations.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.translations.updatesite\target\org.wcs.smart.translations.updatesite-%version%-SNAPSHOT.zip %outputlocation%

move %outputlocation%\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip %outputlocation%\org.wcs.smart.updatesite-%version%-%builddate%.zip
move %outputlocation%\org.wcs.smart.translations.updatesite-%version%-SNAPSHOT.zip %outputlocation%\org.wcs.smart.translations.updatesite-%version%-%builddate%.zip
move %outputlocation%\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip %outputlocation%\smartutils-%version%.zip



REM Combine site & translations into single package

cd %outputlocation%

mkdir temp

mkdir "temp/org.wcs.smart.updatesite-%version%-%builddate%"

copy "org.wcs.smart.updatesite-%version%-%builddate%.zip" "temp/org.wcs.smart.updatesite-%version%-%builddate%/org.wcs.smart.updatesite-%version%-%builddate%.zip"

cd temp/org.wcs.smart.updatesite-%version%-%builddate%

tar -xf org.wcs.smart.updatesite-%version%-%builddate%.zip

del org.wcs.smart.updatesite-%version%-%builddate%.zip 

cd ../../

mkdir "temp/org.wcs.smart.translations.updatesite-%version%-%builddate%"

copy "org.wcs.smart.translations.updatesite-%version%-%builddate%.zip" "temp/org.wcs.smart.translations.updatesite-%version%-%builddate%/org.wcs.smart.translations.updatesite-%version%-%builddate%.zip"

cd temp/org.wcs.smart.translations.updatesite-%version%-%builddate%

tar -xf org.wcs.smart.translations.updatesite-%version%-%builddate%.zip 

del org.wcs.smart.translations.updatesite-%version%-%builddate%.zip 

cd ..

echo artifact.repository.factory.order=compositeArtifacts.xml,\! > p2.index
echo version=1 >> p2.index
echo metadata.repository.factory.order=compositeContent.xml,\! >> p2.index

echo ^<?xml version='1.0' encoding='UTF-8'?^> > compositeArtifacts.xml
echo ^<?compositeArtifactRepository version='1.0.0'?^> >> compositeArtifacts.xml
echo ^<repository name='SMART 8.0.0 Update Site' >> compositeArtifacts.xml
echo     type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'^> >> compositeArtifacts.xml
echo   ^<properties size='1'^> >> compositeArtifacts.xml
echo     ^<property name='p2.timestamp' value='%builddate%0000'/^> >> compositeArtifacts.xml
echo   ^</properties^> >> compositeArtifacts.xml
echo   ^<children size='2'^> >> compositeArtifacts.xml
echo     ^<child location='org.wcs.smart.updatesite-%version%-%builddate%'/^>  >> compositeArtifacts.xml
echo     ^<child location='org.wcs.smart.translations.updatesite-%version%-%builddate%'/^>  >> compositeArtifacts.xml    
echo   ^</children^> >> compositeArtifacts.xml    
echo ^</repository^> >> compositeArtifacts.xml  


echo ^<?xml version='1.0' encoding='UTF-8'?^> > compositeContent.xml
echo ^<?compositeMetadataRepository version='1.0.0'?^> >> compositeContent.xml
echo ^<repository name='SMART 8.0.0 Update Site' >> compositeContent.xml
echo    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'^> >> compositeContent.xml
echo   ^<properties size='1'^> >> compositeContent.xml
echo     ^<property name='p2.timestamp' value='%builddate%0000'/^> >> compositeContent.xml
echo   ^</properties^> >> compositeContent.xml
echo   ^<children size='2'^> >> compositeContent.xml
echo     ^<child location='org.wcs.smart.updatesite-%version%-%builddate%'/^>  >> compositeContent.xml
echo     ^<child location='org.wcs.smart.translations.updatesite-%version%-%builddate%'/^>  >> compositeContent.xml    
echo   ^</children^> >> compositeContent.xml    
echo ^</repository^> >> compositeContent.xml  


tar -a -c -f "../smartsite-%version%.zip" *

cd ..

rmdir temp /s /q



@RD /s /q %outputlocation%\smartapp-win32.win32.x86_64

powershell Expand-Archive %outputlocation%\smartapp-win32.win32.x86_64.zip %outputlocation%\smartapp-win32.win32.x86_64

cd %outputlocation%
del "smartapp-win32.win32.x86_64\SMARTc.exe"
mkdir "smartapp-win32.win32.x86_64\updatesite"
copy "smartsite-%version%.zip" "smartapp-win32.win32.x86_64\updatesite\smartsite-%version%.zip"
copy "smartutils-%version%.zip" "smartapp-win32.win32.x86_64\updatesite\smartutils-%version%.zip"

cd %currentpath%

REM complete - start smart and install plugins/restore backup
