SET version=7.5.1

for /f %%i in ('date /t') do set RESULT=%%i
echo The directory is %RESULT%

SET outputlocation=C:\data\SMART\Builds\SMART7\%RESULT%
echo %outputlocation%
MKDIR %outputlocation%

REM run maven to build packages
call mvn clean install -Pallplatforms,product,update,core,plugins,utils,languagepacks
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

REM del %outputlocation%\org.wcs.smart.migrationtools.updatesite-%version%-SNAPSHOT.zip
REM copy .\org.wcs.smart.migrationtools.updatesite\target\org.wcs.smart.migrationtools.updatesite-%version%-SNAPSHOT.zip %outputlocation%

move %outputlocation%\org.wcs.smart.updatesite-%version%-SNAPSHOT.zip %outputlocation%\site_%version%.zip
REM move %outputlocation%\org.wcs.smart.migrationtools.updatesite-%version%-SNAPSHOT.zip %outputlocation%\migrationtools_%version%.zip
move %outputlocation%\org.wcs.smart.utils.updatesite-%version%-SNAPSHOT.zip %outputlocation%\smartutils_%version%.zip

@RD /s /q %outputlocation%\smartapp-win32.win32.x86_64

powershell Expand-Archive %outputlocation%\smartapp-win32.win32.x86_64.zip %outputlocation%\smartapp-win32.win32.x86_64

del %outputlocation%\smartapp-win32.win32.x86_64\eclipsec.exe
mkdir %outputlocation%\smartapp-win32.win32.x86_64\updatesite
copy %outputlocation%\site_%version%.zip %outputlocation%\smartapp-win32.win32.x86_64\updatesite
copy %outputlocation%\smartutils_%version%.zip %outputlocation%\smartapp-win32.win32.x86_64\updatesite

REM complete - start smart and install plugins/restore backup
