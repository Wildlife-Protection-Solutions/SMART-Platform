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
package org.wcs.smart.intelligence.ui.wizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.ui.map.location.ISmartPoint;
import org.wcs.smart.ui.map.location.LocationSelectComposite;

/**
 * Intelligence Wizard page for collecting the intelligence location(s) information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceLocationWizardPage extends IntelligenceWizardPage {
	
	private LocationSelectComposite<IntelligencePoint> locationSelect;

	/**
	 * @param pageName
	 */
	public IntelligenceLocationWizardPage() {
		super(Messages.IntelligenceLocationWizardPage_PageTitle);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
        locationSelect = new LocationSelectComposite<IntelligencePoint>(parent, SWT.NONE) {
			@Override
			protected ISmartPoint createNewPoint() {
				return new IntelligencePoint();
			}
        };
        
        setControl(locationSelect);
        setMessage(Messages.IntelligenceLocationWizardPage_Message);
 	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.intelligence.ui.wizard.IntelligenceWizardPage#updateModel(org.wcs.smart.intelligence.model.Intelligence)
	 */
	@Override
	protected boolean updateModel(Intelligence intelligence) {
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
		
		//add reminaing; these should all be new points
		for (Iterator<IntelligencePoint> iterator = points.iterator(); iterator.hasNext();) {
			IntelligencePoint pt = iterator.next();
			pt.setIntelligence(intelligence);
			intelligence.getPoints().add(pt);
		}
		return true;
	}

}
