 <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 <xsl:template match="extension[@id='org.locationtech.udig.catalog.operations']"></xsl:template>
 <xsl:template match="wizard[@id='org.locationtech.udig.catalog.ui.dataExportWizard']"></xsl:template>
 
 <xsl:template match="node()|@*">
         <xsl:copy>
             <xsl:apply-templates select="node()|@*"/>
         </xsl:copy>
     </xsl:template>
 </xsl:stylesheet>