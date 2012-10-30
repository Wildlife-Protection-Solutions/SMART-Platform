<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Edited by XMLSpy® -->

<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:smrt="http://www.smartconservationsoftware.org/xml/1.0/datamodel" >

 <xsl:variable name="spacing" as="xs:integer" select="20"/>
<xsl:template match="smrt:DataModel">
  <html>
  <body>
  
  <script type="text/javascript">
  
  	function onClick(element){
  		if (element.getAttribute('state') == "hide"){
  			element.parentNode.parentNode.parentNode.lastChild.style.display="none";
  			element.innerHTML = '+'
  			element.setAttribute('state', 'show');
  		}else{
  			element.parentNode.parentNode.parentNode.lastChild.style.display="";
  			element.innerHTML = '-'
  			element.setAttribute('state', 'hide');
  		}
  		
  	}
  	function onClickList(element){
  		if (element.getAttribute('state') == "hide"){
  			element.parentNode.lastChild.style.display="none";
  			element.innerHTML = '+'
  			element.setAttribute('state', 'show');
  		}else{
  			element.parentNode.lastChild.style.display="";
  			element.innerHTML = '-'
  			element.setAttribute('state', 'hide');
  		}
  		
  	}
  
  </script>
  
  <h2>Data Model</h2>
  
  <xsl:apply-templates select="smrt:categories/smrt:category"/>
  </body>
  </html>
</xsl:template>

<xsl:template match="smrt:category">
  <xsl:variable name="pos" as="xs:integer" select="(count(ancestor-or-self::*) - 3)*$spacing"/>
   <div >
   
   <xsl:attribute name="style">
   	<xsl:text>border-left:1px solid #33446B;background-color:none;color:#33446B;padding:2px;margin-left:</xsl:text><xsl:value-of select="$pos"/><xsl:text>px</xsl:text>
   </xsl:attribute >
   	<div>
   	<div >
   	  <div style="float:left;padding-left:5px;padding-right:5px;width:5px;font-size:12pt;font-weight:bold;cursor:pointer" onclick="onClick(this)" state='hide'>-</div>
   	  <img style="float:left; padding-top:2px;" src="data:image/png;base64,R0lGODlhEAAQAKEDAD9fn1+fvz8/f////yH5BAEKAAMALAAAAAAQABAAAAIonI+pyyMPm4NP0tqusJfTBYRiQJaIOJbkiQLqerTuy6JvUKeqxPdDAQA7"/>
   	
   	  <div style="font-weight:bold;float:left;padding-left:3px"><xsl:value-of select="smrt:names/@value"/></div>
   	  <div style="clear:both"/>
   	</div>
   	
        
        </div>   
        <div >
        
   	<xsl:apply-templates select="smrt:attribute"/>
   	<xsl:apply-templates select="smrt:category"/>
   	
   	</div>
   
   </div>
	
</xsl:template>


<xsl:template match="smrt:category/smrt:attribute">
   <xsl:variable name="pos" as="xs:integer" select="(count(ancestor-or-self::*) - 3)*($spacing)"/>
   <div>
     <xsl:attribute name="style">
   	<xsl:text>border-left:1px solid #33446B;background-color:none;color:#33446B;padding:1px;margin-left:</xsl:text><xsl:value-of select="$pos"/><xsl:text>px</xsl:text>
     </xsl:attribute >
     
     <div style="padding-left: 5px;">
   	
     <xsl:variable name="attributekey" select="@attributekey"/>
     <xsl:for-each select="/smrt:DataModel/smrt:attributes/smrt:attribute[@key=$attributekey]">
	<xsl:if test="@type = 'NUMERIC'">     
         	<img style="float:left; padding-left: 20px; padding-top:2px;" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAAApUlEQVR4nGL4//8/Awjbx8//D2OTggECiIkBCBwSFvxnIBMABBATSPOBBQmM5BoAEEBMlGgGAYAAYqJEMwgABBBeA0DeIxQ+AAHECAp9dEGQt5DDBl84AQQQC6VhABBABOOZUPoACCAWfIajOx05PGDiAAFEts0weYAAIugCDBvRAEAA4TQAlwb0aAUIIJIzD7oXAAKI4pQIEEB4wwCf82FeBAgwAMLS6dmBQUwpAAAAAElFTkSuQmCC"/>
     	 </xsl:if>
     	 <xsl:if test="@type = 'TEXT'">     
         	<img style="float:left; padding-left: 20px; padding-top:2px;" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAAAqUlEQVR4nGL8//8/g0PCgv8MQHBgQQIjA4kAIIAY7ePn/0cWINUQgABigmkix3YQAAggFhAB8wI5ACCAKHYBQACx4JJAdhU+wwECiAmf6TCX4fMiQAAxIZtOjjcAAginF0CAmMAFCCCivIDNYJjhAAGE1wW4XAUzFMQGCCCivIArbEDiAAGE0wBiAxQggPCGATEAIIAoMgDkRYAAYgRlZ3I0gmiQNwECDADyTELm22QzIwAAAABJRU5ErkJggg=="/>
     	 </xsl:if>
     	 <xsl:if test="@type = 'LIST'">   
     	        <div style="float:left;padding-left:5px;padding-right:5px;width:10px;font-size:12pt;font-weight:bold;cursor:pointer" onclick="onClickList(this)" state='show'>+</div>  
         	<img style="float:left; padding-top:2px;" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAAAdklEQVR4nGL8//8/AyUAIICYKNINBAABxAIiHBwayHLGgQMNjAABBDaAQUGBbBcABBDYAAUH8g0ACCBGSgMRIIAoDkSAAKI4EAECiOJABAggigMRIIAoDkSAAKI4EAECiOJABAggigMRIIAoDkSAAKI4EAECDAAMgBpjHy0TpgAAAABJRU5ErkJggg=="/>
     	 </xsl:if>
     	 <xsl:if test="@type = 'BOOLEAN'">     
         	<img style="float:left; padding-left: 20px; padding-top:2px;" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAAAq0lEQVR4nGL8//8/AyUAIICYiFXokLDgPwijiwMEEFEGYNMIAwABRNAAZM0HFiQwossDBBATPucR0gwCAAGE4gJkDcRoBgGAAGKExQIuf+LTDAIAAcSETyEhzSAAEEAoXkDWQIxmEAAIIEZCCQnmNWQDkcUAAoikhIRMwwBAABF0ATZNyC4CCCCiXIAeHsh8gAAiKykj8wECiOykDBMHCCCiAxGmGd07AAEGAN5nUs7+k/IGAAAAAElFTkSuQmCC"/>
     	 </xsl:if>
     	 <xsl:if test="@type = 'TREE'">
     	        <!--<div style="float:left;padding-left:5px;padding-right:5px;width:10px;font-size:12pt;font-weight:bold;cursor:pointer" onclick="onClickList(this)" state='show'>+</div>  -->
         	<img style="float:left; padding-left:20px; padding-top:2px;" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAAAi0lEQVR4nGL8//8/Ay7gkLAALHlgQQIjLjUAAcSEUzeRACCAKDYAIIAoNgAggCg2ACCAKDYAIIAoNgAggCg2ACCAGGHpABbnxAJY2gAIIBZkQQUHBaI0PzjwAM4GCCAWdMkFCQ44Ux0IJCw4gOJSgACiOAwAAohiAwACiGIDAAKIYgMAAohiAwACDAANWhUdJ27ybAAAAABJRU5ErkJggg=="/>
     	 </xsl:if>
     	<div style="font-weight:normal;float:left;padding-left:3px"><xsl:value-of select="smrt:names/@value"/></div>	 
     	<div style="clear:both"/>
     	 
     	 <xsl:if test="@type = 'LIST'">     
     	   <div>
              <xsl:attribute name="style"><xsl:text>display:none;border-left:1px solid #33446B;background-color:none;color:#33446B;padding:1px;margin-left:</xsl:text><xsl:value-of select="$pos"/><xsl:text>px</xsl:text></xsl:attribute >
              <div style="padding-left: 5px;">
                 <xsl:for-each select="smrt:values">
                    <div style="font-weight:normal;float:left;padding-left:3px"><xsl:value-of select="smrt:names/@value"/></div>
                    <div style="clear:both"/>
                 </xsl:for-each>
             </div>
           </div>
     	 </xsl:if>
     	 
     	 <!--
     	 <xsl:if test="@type = 'TREE'">     
     	   <div>
              <xsl:attribute name="style"><xsl:text>display:none;border-left:1px solid #33446B;background-color:none;color:#33446B;padding:1px;margin-left:</xsl:text><xsl:value-of select="$pos"/><xsl:text>px</xsl:text></xsl:attribute >
              <div style="padding-left: 5px;">
                 <xsl:for-each select="smrt:tree">
                    <div style="font-weight:normal;float:left;padding-left:3px"><xsl:value-of select="smrt:names/@value"/></div>
                    <div style="clear:both"/>
                    
                    <xsl:apply-templates select="smrt:children"/>
                 </xsl:for-each>
             </div>
           </div>
     	 </xsl:if>
     	 -->
     	 
     	 
     	 
     </xsl:for-each>
     
     
   	</div>
   	
   
   </div>
   
  
</xsl:template>

<!--
<xsl:template match="smrt:tree/smrt:children">
  <xsl:variable name="pos" as="xs:integer" select="(count(ancestor-or-self::*) - 3)*$spacing"/>
  <div >
   <xsl:attribute name="style">
   	<xsl:text>border-left:1px solid #33446B;background-color:none;color:#33446B;padding:2px;margin-left:</xsl:text><xsl:value-of select="$pos"/><xsl:text>px</xsl:text>
   </xsl:attribute >
   	<div>
     	  <div >
   	    <div style="float:left;padding-left:5px;padding-right:5px;width:5px;font-size:12pt;font-weight:bold;cursor:pointer" onclick="onClickList(this)" state='show'>+</div>   	
   	    <div style="font-weight:bold;float:left;padding-left:3px"><xsl:value-of select="smrt:names/@value"/></div>
   	    <div style="clear:both"/>
   	  </div>
        </div>   
        <div >
   	  <xsl:apply-templates select="smrt:children"/>     
   	</div>
   </div>
</xsl:template>
-->
</xsl:stylesheet> 
