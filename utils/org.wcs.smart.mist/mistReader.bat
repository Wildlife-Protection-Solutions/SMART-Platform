@echo off
REM -------------------------------------------------------------
REM Script to read a MUST DB and output the stations, employees, agencies&ranks and patrol types
REM Usage from DOS: Read_Mist.bat <DB Filename/Location>

echo "Enter Database Name:"
set /p DBName=

echo "Enter Language Code(en, fr, th...):"
set /p Lang=


SET LAUNCHDIR=%~dp0
SET FIREBIRDDIR=%LAUNCHDIR%%FIREBIRDDIR%

set PATH=%PATH%;%FIREBIRDDIR%
"jre\bin\java.exe" -cp %CP1%%CP2% org.wcs.smart.mist.dataReader.DataReader %DBName% %Lang%