 <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 <xsl:template match="viewContribution[@id='org.locationtech.udig.style.sld.styleAction']"></xsl:template>
 <xsl:template match="objectContribution[@id='org.locationtech.udig.style.sld.editor.LayerContribution']"></xsl:template>
 <xsl:template match="node()|@*">
         <xsl:copy>
             <xsl:apply-templates select="node()|@*"/>
         </xsl:copy>
     </xsl:template>
 </xsl:stylesheet>
 
 
 