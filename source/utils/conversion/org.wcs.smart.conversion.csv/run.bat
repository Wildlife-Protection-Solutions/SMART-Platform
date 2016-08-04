@echo off
setlocal

set is64=0
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "64-Bit"') do (
    set is64=1
)
if %is64%==0 (
    @echo Application requires 64-Bit version of java. Please install the desired version.
    pause
)
endlocal
@echo on
javaw -cp %CP1%;%CP2% org.wcs.smart.conversion.csv.Csv2SmartMatcher > csv2smart.log