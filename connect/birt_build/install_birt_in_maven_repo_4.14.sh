#!/bin/bash

# This script was tested in macOS.
# Download 'birt-runtime-4.13.0-20230309.zip' BIRT runtime ZIP file, then unzip it.
# unzip ~/Downloads/birt-runtime-4.14.0-202312020807.zip -d /tmp/birt-runtime-4.14.0-202312020807

RUNTIME_PATH="C:/data/SMART/BIRT/birt-runtime-4.14.0-202312020807"
LIB_PATH="${RUNTIME_PATH}/ReportEngine/lib"
ADDONS_PATH="${RUNTIME_PATH}/ReportEngine/addons"
GROUP_ID_BASE="com.friss.org.eclipse.birt"

rm -rf /tmp/pom.xml
cat <<EOT >>/tmp/pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <groupId>${GROUP_ID_BASE}</groupId>
    <artifactId>org.eclipse.birt.runtime</artifactId>
    <version>4.14.0</version>
    <packaging>jar</packaging>
    <name>org.eclipse.birt.runtime_4.14.0.jar</name>
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
  mvn deploy:deploy-file -Durl=file://${HOME}/.m2/repository \
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
deploy "${LIB_PATH}/org.eclipse.core.runtime_3.30.0.v20231102-0719.jar" "${GROUP_ID_BASE}" "org.eclipse.core.runtime" "3.30.0"
deploy "${LIB_PATH}/org.eclipse.datatools.connectivity.oda.consumer_3.5.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.connectivity.oda.consumer" "3.5.0"
deploy "${LIB_PATH}/org.eclipse.datatools.connectivity.oda_3.7.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.connectivity.oda" "3.7.0"
deploy "${LIB_PATH}/org.eclipse.datatools.connectivity_1.15.0.202311071249.jar" "${GROUP_ID_BASE}" "org.eclipse.datatools.connectivity" "1.15.0"
deploy "${LIB_PATH}/org.eclipse.emf.common_2.29.0.v20230916-0637.jar" "${GROUP_ID_BASE}" "org.eclipse.emf.common" "2.29.0"
deploy "${LIB_PATH}/org.eclipse.emf.ecore.xmi_2.36.0.v20231002-1156.jar" "${GROUP_ID_BASE}" "org.eclipse.emf.ecore.xmi" "2.36.0"
deploy "${LIB_PATH}/org.eclipse.emf.ecore_2.35.0.v20230829-0934.jar" "${GROUP_ID_BASE}" "org.eclipse.emf.ecore" "2.35.0"
deploy "${LIB_PATH}/org.eclipse.equinox.common_3.18.200.v20231106-1826.jar" "${GROUP_ID_BASE}" "org.eclipse.equinox.common" "3.18.200"
deploy "${LIB_PATH}/org.eclipse.equinox.registry_3.11.400.v20231102-2218.jar" "${GROUP_ID_BASE}" "org.eclipse.equinox.registry" "3.11.400"
deploy "${LIB_PATH}/org.eclipse.osgi_3.18.600.v20231110-1900.jar" "${GROUP_ID_BASE}" "org.eclipse.osgi" "3.18.600"

#replace it with standard maven artifact to reduce conflicts with the project importing this runtime
#deploy "${LIB_PATH}/com.github.librepdf.openpdf_1.3.33.jar" "${GROUP_ID_BASE}" "com.github.librepdf.openpdf" "1.3.33"
addToPom "com.github.librepdf" "openpdf" "1.3.39"
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
#deploy "${LIB_PATH}/org.apache.xerces_2.12.2.v20220131-0835.jar" "${GROUP_ID_BASE}" "org.apache.xerces" "2.12.2"
addToPom "xerces" "xercesImpl" "2.12.2"
#deploy "${LIB_PATH}/org.apache.xmlgraphics_2.9.0.v20230916-1600.jar" "${GROUP_ID_BASE}" "org.apache.xmlgraphics" "2.9.0"
addToPom "org.apache.xmlgraphics" "xmlgraphics-commons" "2.9"
#deploy "${LIB_PATH}/com.ibm.icu_74.1.0.jar" "${GROUP_ID_BASE}" "com.ibm.icu" "74.1.0"
addToPom "com.ibm.icu" "icu4j" "74.2"
#higher rhino version will break some reports.
#deploy "${LIB_PATH}/org.mozilla.javascript_1.7.10.v20221112-0806.jar" "${GROUP_ID_BASE}" "org.mozilla.javascript" "1.7.10"
addToPom "org.mozilla" "rhino" "1.7.10"
#this is not needed, JDK has included this
#deploy "${LIB_PATH}/javax.xml_1.3.4.v201005080400.jar" "${GROUP_ID_BASE}" "javax.xml" "1.3.4"
#bad package name in original BIRT runtime lib folder
#deploy "${LIB_PATH}/org.eclipse.orbit.xml-apis-ext_1.0.0.v20230923-0644.jar" "${GROUP_ID_BASE}" "w3c.dom.and.css" "1.0.0"
addToPom "org.eclipse.birt.runtime.3_7_1" "org.w3c.dom.svg" "1.1.0"
addToPom "org.eclipse.birt.runtime" "org.w3c.dom.smil" "1.0.1.v200903091627"
addToPom "org.eclipse.birt.runtime" "org.w3c.css.sac" "1.3.1.v200903091627"
#deploy "${LIB_PATH}/org.apache.commons.commons-collections4_4.4.0.jar" "${GROUP_ID_BASE}" "org.apache.commons.commons-collections4" "4.4.0"
addToPom "org.apache.commons" "commons-collections4" "4.4"
#deploy "${LIB_PATH}/org.apache.commons.commons-compress_1.25.0.jar" "${GROUP_ID_BASE}" "org.apache.commons.commons-compress" "1.25.0"
addToPom "org.apache.commons" "commons-compress" "1.25.0"
#deploy "${LIB_PATH}/org.apache.commons.commons-io_2.15.0.jar" "${GROUP_ID_BASE}" "org.apache.commons.commons-io" "2.15.0"
addToPom "commons-io" "commons-io" "2.15.1"
#deploy "${LIB_PATH}/org.apache.logging.log4j.api_2.21.1.jar" "${GROUP_ID_BASE}" "org.apache.logging.log4j.api" "2.21.1"
addToPom "org.apache.logging.log4j" "log4j-api" "2.22.1"
#deploy "${LIB_PATH}/org.apache.poi_5.2.4.v20231007-1530.jar" "${GROUP_ID_BASE}" "org.apache.poi" "5.2.4"
addToPom "org.apache.poi" "poi" "5.2.5"
#deploy "${LIB_PATH}/org.apache.poi.ooxml_5.2.4.v20231007-1530.jar" "${GROUP_ID_BASE}" "org.apache.poi.ooxml" "5.2.4"
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
#zip --delete ${LIB_PATH}/org.eclipse.birt.runtime_4.14.0-202312020807.jar "META-INF/maven/*"
7z d -r ${LIB_PATH}/org.eclipse.birt.runtime_4.14.0-202312020807.jar "META-INF/maven/*"

mvn deploy:deploy-file -Durl=file://${HOME}/.m2/repository \
    -DpomFile=/tmp/pom.xml \
    -DrepositoryId=third-party \
    -Dfile=${LIB_PATH}/org.eclipse.birt.runtime_4.14.0-202312020807.jar \
    -DgroupId=${GROUP_ID_BASE} \
    -DartifactId=org.eclipse.birt.runtime \
    -Dversion=4.14.0 \
    -Dpackaging=jar \
    -Ddescription="Eclipse BIRT Runtime"
