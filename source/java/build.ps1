#POWERSHELL SCRIPT for cleaning up builds and copying in additional dependencies

#mvn clean install -Pwindows,core,plugins -Dtycho.disableP2Mirrors=true
#mvn clean install -Pallplatforms,core,plugins -Dtycho.disableP2Mirrors=true
#mvn clean install -Pallplatforms,core,plugins,languagepacks -Dtycho.disableP2Mirrors=true

if (Test-Path './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/'){
	'Processing Window 64'
	Remove-Item './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/eclipsec.exe'
	New-Item './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/updatesite' -type directory
	Copy-Item 'L:/Refractions/SMART/Delivery/jres/win64/jre' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/jre' -recurse
	Copy-Item 'L:/Refractions/SMART/Delivery/jres/GPSBabel' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/GPSBabel' -recurse
	Copy-Item 'L:/Refractions/SMART/Delivery/LowMemory_Shortcuts/SMART.LowMemory.exe.lnk' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/SMART.LowMemory.exe' 
	Copy-Item './org.wcs.smart.updatesite/target/org.wcs.smart.updatesite-6.0.0-SNAPSHOT.zip' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86_64/updatesite/site_6.0.0.v1.zip'
}else{
	'WIN 64 BUILD NOT CREATED'
}

if (Test-Path './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/'){
	'Processing Window 32'
	Remove-Item './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/eclipsec.exe'
	New-Item './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/updatesite' -type directory
	Copy-Item 'L:/Refractions/SMART/Delivery/jres/win32/jre' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/jre' -recurse
	Copy-Item 'L:/Refractions/SMART/Delivery/jres/GPSBabel' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/GPSBabel' -recurse
	Copy-Item 'L:/Refractions/SMART/Delivery/LowMemory_Shortcuts/SMART.LowMemory.exe.lnk' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/SMART.LowMemory.exe' 
	Copy-Item './org.wcs.smart.updatesite/target/org.wcs.smart.updatesite-6.0.0-SNAPSHOT.zip' './org.wcs.smart-product/target/products/smartapp/win32/win32/x86/updatesite/site_6.0.0.v1.zip'
}else{
	'WIN 32 BUILD NOT CREATED'
}

#TODO: macosx and linux