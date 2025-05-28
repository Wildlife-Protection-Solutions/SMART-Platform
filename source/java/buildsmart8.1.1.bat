
SET keystore_alias=tomcat
SET keystore_location=C:\data\SMART\Builds\keystore\connect8.refractions.net.jks
SET keystore_password=hi1234

call mvn clean install -Pallplatforms,core,cybertracker,profile,event,smartcollect,connect,er,eclipse-sign -Djarsigner.alias=%keystore_alias% -Djarsigner.storepass=%keystore_password% -Djarsigner.keystore=%keystore_location%
