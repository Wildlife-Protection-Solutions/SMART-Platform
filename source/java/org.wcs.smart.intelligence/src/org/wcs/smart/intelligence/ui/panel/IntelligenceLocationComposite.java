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
import org.wcs.smart.ui.map.location.ISmartPoint;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

/**
 * Composite for collecting the intelligence location(s) information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceLocationComposite extends IntelligenceComposite {

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
        
        locationSelect = new LocationSelectComposite<IntelligencePoint>(this, SWT.NONE) {
			@Override
			protected ISmartPoint createNewPoint() {
				return new IntelligencePoint();
			}
        };
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.intelligence.ui.panel.IIntelligenceModifier#updateModel(org.wcs.smart.intelligence.model.Intelligence)
	 */
	@Override
	public boolean updateModel(Intelligence intelligence) {
		//Update the points
		List<IntelligencePoint> points = locationSelect.getPoints();
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
		return true;
	}

	@Override
	public void initFromModel(Intelligence intelligence) {
		locationSelect.getPoints().clear();
		locationSelect.getPoints().addAll(intelligence.getPoints());
	}

}
