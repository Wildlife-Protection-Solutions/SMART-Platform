/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.entity.report;

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.ui.XMLMemento;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;


/**
 * Style provider for fixed entity tables.
 * 
 * @author Emily
 *
 */
public class EntityTableStyleProvider implements IBirtLayerStyleProvider {


	@Override
	public StyleBlackboard getStyle(String extensionId, String queryText,
			Session s) {
		
		if (extensionId.equals(SmartTableQuery.SMART_DATASET_TYPE)){
			Style style = null;
			if (queryText.toUpperCase().startsWith(EntityTable.ENTITYKEY_PREFIX + ":" + EntityType.Type.FIXED.name())){ //$NON-NLS-1$
				try{
					style = createDefaultStyle();
				}catch (Exception ex){
					Logger.getLogger(EntityTableStyleProvider.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
				}
			}
			
			if (style != null){
				StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
				sb.put(SLDContent.ID, style);
				return sb;
			}
		}
		return null;
	}
	
	/**
	 * The default style for fixed entity layers
	 * @return
	 */
	public static Style createDefaultStyle() throws Exception{
		String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
	    		"<styleEntry version=\"1.0\" type=\"SLDStyle\">"+ //$NON-NLS-1$
	    		"&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"+ //$NON-NLS-1$
	    		"	&lt;sld:UserStyle xmlns=\"http://www.opengis.net/sld\""+ //$NON-NLS-1$
	    		"		xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\""+ //$NON-NLS-1$
	    		"		xmlns:gml=\"http://www.opengis.net/gml\"&gt;"+ //$NON-NLS-1$
	    		"		&lt;sld:Name&gt;Default Styler&lt;/sld:Name&gt;"+ //$NON-NLS-1$
	    		"		&lt;sld:Title /&gt;"+ //$NON-NLS-1$
	    		"		&lt;sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
	    		"			&lt;sld:Name&gt;simple&lt;/sld:Name&gt;"+ //$NON-NLS-1$
	    		"			&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;"+ //$NON-NLS-1$
	    		"			&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
	    		"			&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
	    		//rule for default style
	    		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
	    		"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
	    		"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
	    		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
	    		"							&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;"+ //$NON-NLS-1$
	    		"							&lt;sld:Fill&gt;"+ //$NON-NLS-1$
	    		"								&lt;sld:CssParameter name=\"fill\"&gt;#00FFFF&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
	    		"							&lt;/sld:Fill&gt;"+ //$NON-NLS-1$
//	    		"							&lt;sld:Stroke /&gt;"+ //$NON-NLS-1$
	    		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
	    		"						&lt;sld:Size&gt;11.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
	    		"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
	    		"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
	    		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
	    			"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
	    		"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
	    		"</styleEntry>"; //$NON-NLS-1$
		
		XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
		SLDContent c = new SLDContent();
		Style style = (Style)c.load(memento);
		return style;
	}
}
