#!/bin/bash

# NOTE: I found a version of this script online and updated
# it to work with version 4.15 of BIRT.


# TO RUN:
# 1. Download 'birt-runtime-4.15.0-202403270652.zip' BIRT runtime ZIP file, then unzip it.
#    unzip ~/Downloads/birt-runtime-4.14.0-202312020807.zip -d /tmp/birt-runtime-4.14.0-202312020807
# 2. Update the RUNTIME_PATH variable below to point to the unzipped folder
# 3. Update the REPO_LOC variable below to point to the directory containing your .m2 folder (repo)

# TO UPGRADE:
# 1. need to upgrade the birt runtime version 
# 2. need to go through the depenencies in this script and upgrade versions/add required/remove old

RUNTIME_PATH="C:/data/SMART/BIRT/birt-runtime-4.15.0-202403270652"
LIB_PATH="${RUNTIME_PATH}/ReportEngine/lib"
ADDONS_PATH="${RUNTIME_PATH}/ReportEngine/addons"
GROUP_ID_BASE="com.friss.org.eclipse.birt"
REPO_LOC="C:/Users/<USERNAME>"

rm -rf /tmp/pom.xml
cat <<EOT >>/tmp/pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <groupId>${GROUP_ID_BASE}</groupId>
    <artifactId>org.eclipse.birt.runtime</artifactId>
    <version>4.15.0</version>
    <packaging>jar</packaging>
    <name>org.eclipse.birt.runtime_4.15.0.jar</name>
    <description>the BIRT runtime artifact</description>
    <url>https://projects.eclipse.org/projects/technology.birt</url>
    <licenses>
        <license>
            <name>Eclipse Public License - v 1.0</name>
            <url>http://www.eclipse.org/org/documents/epl-v10.html</url>
        </license>
    </licenses>
    <dependencies>
EOT

function addToPom {
    GROUP_ID="${1}"
    ARTIFACT_ID="${2}"
    ARTIFACT_VERSION="${3}"
    cat <<EOT >> /tmp/pom.xml
          <dependency>
              <groupId>${GROUP_ID}</groupId>
              <artifactId>${ARTIFACT_ID}</artifactId>
              <version>${ARTIFACT_VERSION}</version>
          </dependency>
EOT
}

function deploy {
  FILE="${1}"
  GROUP_ID="${2}"
  ARTIFACT_ID="${3}"
  ARTIFACT_VERSION="${4}"

  # Reposilite will always serve the POM inside the JAR instead of the one we generate. Unfortunately this POM is
  # incorrect (contains deps with -SNAPSHOT versions). Therefor we delete it from the JAR's.
  #zip --delete ${FILE} "META-INF/maven/*"
  7z d -r ${FILE} "META-INF/maven/*"

  #mvn deploy:deploy-file -Durl=https://reposilite.serviceplanet.nl/third-party \
  mvn deploy:deploy-file -Durl=file://${REPO_LOC}/.m2/repository \
    -DgeneratePom=true \
    -DrepositoryId=third-party \
    -Dfile=${FILE} \
    -DgroupId=${GROUP_ID} \
    -DartifactId=${ARTIFACT_ID} \
    -Dversion=${ARTIFACT_VERSION} \
    -Dpackaging=jar \
    -Ddescription="Eclipse BIRT Runtime"

  addToPom ${GROUP_ID} ${ARTIFACT_ID} ${ARTIFACT_VERSION}
EOT
}

# Core
deploy "${LIB_PATH}/org.eclipse.core.runtime_3.31.0.v20240215-1631.jar" "${GROUP_ID_BASE}" "org.eclipse.core.runtime" "3.31.0"
deploy "${LIB_PATH}/org.eclipse.datatools.connectivity.oda.consumer_3.5.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.connectivity.oda.consumer" "3.5.0"
deploy "${LIB_PATH}/org.eclipse.datatools.connectivity.oda_3.7.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.connectivity.oda" "3.7.0"
deploy "${LIB_PATH}/org.eclipse.datatools.connectivity_1.15.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.connectivity" "1.15.0"
deploy "${LIB_PATH}/org.eclipse.emf.common_2.30.0.v20231210-0956.jar" "${GROUP_ID_BASE}" "org.eclipse.emf.common" "2.30.0"
deploy "${LIB_PATH}/org.eclipse.emf.ecore.xmi_2.37.0.v20231208-1346.jar" "${GROUP_ID_BASE}" "org.eclipse.emf.ecore.xmi" "2.37.0"
deploy "${LIB_PATH}/org.eclipse.emf.ecore_2.36.0.v20240203-0859.jar" "${GROUP_ID_BASE}" "org.eclipse.emf.ecore" "2.36.0"
deploy "${LIB_PATH}/org.eclipse.equinox.common_3.19.0.v20240214-0846.jar" "${GROUP_ID_BASE}" "org.eclipse.equinox.common" "3.19.0"
deploy "${LIB_PATH}/org.eclipse.equinox.registry_3.12.0.v20240213-1057.jar" "${GROUP_ID_BASE}" "org.eclipse.equinox.registry" "3.12.0"
deploy "${LIB_PATH}/org.eclipse.osgi_3.19.0.v20240213-1246.jar" "${GROUP_ID_BASE}" "org.eclipse.osgi" "3.19.0"

#replace it with standard maven artifact to reduce conflicts with the project importing this runtime
#deploy "${LIB_PATH}/com.github.librepdf.openpdf_1.4.1.jar" "${GROUP_ID_BASE}" "com.github.librepdf.openpdf" "1.4.1"
addToPom "com.github.librepdf" "openpdf" "1.4.1"
#deploy "${LIB_PATH}/org.apache.batik.anim_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.anim" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-anim" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.awt.util_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.awt.util" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-awt-util" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.bridge_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.bridge" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-bridge" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.css_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.css" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-css" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.constants_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.constants" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-constants" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.dom_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.dom" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-dom" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.ext_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.ext" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-ext" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.gvt_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.gvt" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-gvt" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.i18n_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.i18n" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-i18n" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.parser_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.parser" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-parser" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.script_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.script" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-script" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.dom.svg_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.dom.svg" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-svg-dom" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.transcoder_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.transcoder" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-transcoder" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.util_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.util" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-util" "1.17"
#deploy "${LIB_PATH}/org.apache.batik.xml_1.17.0.v20231009-1000.jar" "${GROUP_ID_BASE}" "org.apache.batik.xml" "1.17.0"
addToPom "org.apache.xmlgraphics" "batik-xml" "1.17"
#deploy "${LIB_PATH}/org.apache.xerces_2.12.2.v20230928-1306.jar" "${GROUP_ID_BASE}" "org.apache.xerces" "2.12.2"
addToPom "xerces" "xercesImpl" "2.12.2"
#deploy "${LIB_PATH}/org.apache.xmlgraphics_2.9.0.v20230916-1600.jar" "${GROUP_ID_BASE}" "org.apache.xmlgraphics" "2.9.0"
addToPom "org.apache.xmlgraphics" "xmlgraphics-commons" "2.9"
#deploy "${LIB_PATH}/com.ibm.icu_74.2.0.jar" "${GROUP_ID_BASE}" "com.ibm.icu" "74.2.0"
addToPom "com.ibm.icu" "icu4j" "74.2"
#higher rhino version will break some reports.
#deploy "${LIB_PATH}/org.mozilla.rhino_1.7.14.jar" "${GROUP_ID_BASE}" "org.mozilla.javascript" "1.7.14"
addToPom "org.mozilla" "rhino" "1.7.14"
#this is not needed, JDK has included this
#deploy "${LIB_PATH}/javax.xml_1.3.4.v201005080400.jar" "${GROUP_ID_BASE}" "javax.xml" "1.3.4"
#bad package name in original BIRT runtime lib folder
#deploy "${LIB_PATH}/org.eclipse.orbit.xml-apis-ext_1.0.0.v20230923-0644.jar" "${GROUP_ID_BASE}" "w3c.dom.and.css" "1.0.0"

addToPom "org.eclipse.birt.runtime.3_7_1" "org.w3c.dom.svg" "1.1.0"
addToPom "org.eclipse.birt.runtime" "org.w3c.dom.smil" "1.0.1.v200903091627"
addToPom "org.eclipse.birt.runtime" "org.w3c.css.sac" "1.3.1.v200903091627"

#deploy "${LIB_PATH}/org.apache.commons.commons-collections4_4.4.0.jar" "${GROUP_ID_BASE}" "org.apache.commons.commons-collections4" "4.4.0"
addToPom "org.apache.commons" "commons-collections4" "4.4"
#deploy "${LIB_PATH}/org.apache.commons.commons-compress_1.26.0.jar" "${GROUP_ID_BASE}" "org.apache.commons.commons-compress" "1.26.0"
addToPom "org.apache.commons" "commons-compress" "1.26.0"
#deploy "${LIB_PATH}/org.apache.commons.commons-io_2.15.1.jar" "${GROUP_ID_BASE}" "org.apache.commons.commons-io" "2.15.1"
addToPom "commons-io" "commons-io" "2.15.1"
#deploy "${LIB_PATH}/org.apache.logging.log4j.api_2.23.0.jar" "${GROUP_ID_BASE}" "org.apache.logging.log4j.api" "2.23.0"
addToPom "org.apache.logging.log4j" "log4j-api" "2.23.0"
#deploy "${LIB_PATH}/org.apache.poi_5.2.5.v20231203-1619.jar" "${GROUP_ID_BASE}" "org.apache.poi" "5.2.5"
addToPom "org.apache.poi" "poi" "5.2.5"
#deploy "${LIB_PATH}/org.apache.poi.ooxml_5.2.5.v20231203-1619.jar" "${GROUP_ID_BASE}" "org.apache.poi.ooxml" "5.2.5"
addToPom "org.apache.poi" "poi-ooxml" "5.2.5"
#poi-ooxml should be enough without ooxml.schemas
#deploy "${LIB_PATH}/org.apache.poi.ooxml.schemas_5.2.4.v20231007-1530.jar" "${GROUP_ID_BASE}" "org.apache.poi.ooxml.schemas" "5.2.4"
#xmlbeans is a dependency of poi-ooxml
#deploy "${LIB_PATH}/org.apache.xmlbeans_5.1.1.v20230929-1100.jar" "${GROUP_ID_BASE}" "org.apache.xmlbeans" "5.1.1"
#deploy "${LIB_PATH}/org.osgi.service.prefs_1.1.2.202109301733.jar" "${GROUP_ID_BASE}" "org.osgi.service.prefs" "1.1.2"
addToPom "org.osgi" "org.osgi.service.prefs" "1.1.2"
#the following are needed to pass xls_spudsoft test
#deploy "${LIB_PATH}/com.zaxxer.sparsebits_1.3.0.v20230929-1000.jar" "${GROUP_ID_BASE}" "com.zaxxer.sparsebits" "1.3.0"
addToPom "com.zaxxer" "SparseBitSet" "1.3"

# Addons
#deploy "${ADDONS_PATH}/org.eclipse.datatools.enablement.oda.xml_1.5.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.enablement.oda.xml" "1.5.0"

cat <<EOT >>/tmp/pom.xml
    </dependencies>
</project>
EOT

# The final jar we will refer to in our own projects
#zip --delete ${LIB_PATH}/org.eclipse.birt.runtime_4.15.0-202403270652.jar "META-INF/maven/*"
7z d -r ${LIB_PATH}/org.eclipse.birt.runtime_4.15.0-202403270652.jar "META-INF/maven/*"

mvn deploy:deploy-file -Durl=file://${REPO_LOC}/.m2/repository \
    -DpomFile=/tmp/pom.xml \
    -DrepositoryId=third-party \
    -Dfile=${LIB_PATH}/org.eclipse.birt.runtime_4.15.0-202403270652.jar \
    -DgroupId=${GROUP_ID_BASE} \
    -DartifactId=org.eclipse.birt.runtime \
    -Dversion=4.15.0 \
    -Dpackaging=jar \
    -Ddescription="Eclipse BIRT Runtime"
