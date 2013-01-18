 <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 <xsl:template match="extension[@id='net.refractions.udig.catalog.operations']"></xsl:template>
 <xsl:template match="wizard[@id='net.refractions.udig.catalog.ui.dataExportWizard']"></xsl:template>
 
 <xsl:template match="node()|@*">
         <xsl:copy>
             <xsl:apply-templates select="node()|@*"/>
         </xsl:copy>
     </xsl:template>
 </xsl:stylesheet>