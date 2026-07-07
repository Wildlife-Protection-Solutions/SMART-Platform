SET version=8.1.2
SET builddate=%date:~5,2%%date:~8,2%%date:~0,4%
SET currentpath=%cd%

for /f %%i in ('date /t') do set RESULT=%%i
echo The directory is %RESULT%
SET outputlocation=C:\data\SMART\Builds\SMART8\%RESULT%

SET keystore_alias=tomcat
SET keystore_location=C:\data\SMART\Builds\keystore\connect8.refractions.net.jks
SET keystore_password=hi1234

call mvn clean install -Pallplatforms,update,core,plugins,utils,languagepacks,eclipse-sign -Djarsigner.alias=%keystore_alias% -Djarsigner.storepass=%keystore_password% -Djarsigner.keystore=%keystore_location%
rem call mvn clean install -Pallplatforms,update,core,plugins,utils,languagepacks


MKDIR %outputlocation%

del %outputlocation%\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.updatesite\target\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip %outputlocation%\org.wcs.smart.updatesite-%version%-%builddate%.zip

del %outputlocation%\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.utils.updatesite\target\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip %outputlocation%\org.wcs.smart.utils.updatesite-%version%-%builddate%.zip

del %outputlocation%\org.wcs.smart.translations.updatesite-%version%-SNAPSHOT.zip
copy .\org.wcs.smart.translations.updatesite\target\org.wcs.smart.translations.updatesite-%version%-SNAPSHOT.zip %outputlocation%\org.wcs.smart.translations.updatesite-%version%-%builddate%.zip




