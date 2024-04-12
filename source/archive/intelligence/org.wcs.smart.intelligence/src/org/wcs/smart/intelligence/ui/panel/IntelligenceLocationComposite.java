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
package org.wcs.smart.intelligence.ui.panel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.ui.map.location.ILocationPointsChangeListener;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

/**
 * Composite for collecting the intelligence location(s) information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceLocationComposite extends IntelligenceComposite implements ILocationPointsChangeListener {

	private LocationSelectComposite<IntelligencePoint> locationSelect;

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceLocationComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceLocation_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(1, false));
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		this.setLayoutData(layoutData);
        
        locationSelect = new LocationSelectComposite<IntelligencePoint>(this, SWT.NONE, getLayerStyle()) {
			@Override
			protected IntelligencePoint createNewPoint() {
				return new IntelligencePoint();
			}
        };
        locationSelect.addLocationPointsChangeListener(this);
	}
	
	@Override
	protected void updateModelInternal(Intelligence intelligence) {
		//create a copy of points array from composite (we don't want to remove from original array as this will effect gui)
		List<IntelligencePoint> points = new ArrayList<IntelligencePoint>(locationSelect.getPoints());
		//Update the points
		if (intelligence.getPoints() == null) {
			intelligence.setPoints(new ArrayList<IntelligencePoint>());
		}
		
		for (Iterator<IntelligencePoint> iterator = intelligence.getPoints().iterator(); iterator.hasNext();) {
			IntelligencePoint pt = iterator.next();
			if (!points.remove(pt)){
				iterator.remove();
			}
		}
		
		//add remaining; these should all be new points
		for (Iterator<IntelligencePoint> iterator = points.iterator(); iterator.hasNext();) {
			IntelligencePoint pt = iterator.next();
			pt.setIntelligence(intelligence);
			intelligence.getPoints().add(pt);
		}
	}

	@Override
	public void initFromModel(Intelligence intelligence) {
		locationSelect.setPoints(intelligence.getPoints());
	}

	@Override
	public void locationPointsChanged() {
		fireInputChangeListeners();
	}

	private String getLayerStyle(){
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
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

				//rule for not selected points (same as default)
				"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
				"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
				"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:Literal&gt;false&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
				"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
				"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
				"							&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;" + //$NON-NLS-1$
				"							&lt;sld:Fill&gt;" + //$NON-NLS-1$
		    	"							&lt;sld:CssParameter name=\"fill\"&gt;#FF0000&lt;/sld:CssParameter&gt;" + //$NON-NLS-1$
		    	"							&lt;/sld:Fill&gt;" + //$NON-NLS-1$
				"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Size&gt;10.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
				"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
				"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
				
				//rule for selected points
				"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
				"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
				"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
				"                            &lt;ogc:Literal&gt;true&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
				"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
				"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
				"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
				"							&lt;sld:WellKnownName&gt;star&lt;/sld:WellKnownName&gt;" + //$NON-NLS-1$
				"							&lt;sld:Fill&gt;" + //$NON-NLS-1$
		    	"							&lt;sld:CssParameter name=\"fill\"&gt;#FFFF00&lt;/sld:CssParameter&gt;" + //$NON-NLS-1$
		    	"							&lt;/sld:Fill&gt;" + //$NON-NLS-1$
				"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
				"						&lt;sld:Size&gt;10.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
				"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
				"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
				"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
				
				
				"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
				"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
				"</styleEntry>"; //$NON-NLS-1$
	}
}
