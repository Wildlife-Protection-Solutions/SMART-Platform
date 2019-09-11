/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.survey.navigation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.cybertracker.navigation.ui.INavigationLayerTargetProvider;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.SamplingUnit;

/**
 * Sampling unit navigation layer target provider.
 * 
 * @author Emily
 *
 */
public class SamplingUnitNavigationTargetProvider implements INavigationLayerTargetProvider {

	private SamplingUnitWizardPage page1;
	
	@Override
	public String getTypeName() {
		return Messages.SamplingUnitNavigationTargetProvider_ProviderName;
	}

	@Override
	public Image getImage() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY32_ICON);
	}
	
	@Override
	public List<WizardPage> getPages() {
		if (page1 == null) {
			page1 = new SamplingUnitWizardPage();
		}
		return Collections.singletonList(page1);
	}

	@Override
	public List<NavigationTarget> getTargets(IProgressMonitor monitor) {
		List<SamplingUnit> sus = page1.getSamplingUnits();
		
		List<NavigationTarget> targets = new ArrayList<>();
		
		for (SamplingUnit su : sus) {
			try {
				String id = MessageFormat.format("{0} ({1})", su.getId(), su.getSurveyDesign().getName()); //$NON-NLS-1$
				Geometry g = su.getGeometry();
				if (g instanceof Point || g instanceof LineString) {
					NavigationTarget target = new NavigationTarget();
					target.setGeometry(g);
					target.setId(id);
					targets.add(target);
				}else if (g instanceof MultiPoint || g instanceof MultiLineString) {
					for (int i = 0; i < g.getNumGeometries(); i ++) {
						NavigationTarget target = new NavigationTarget();
						target.setGeometry(g.getGeometryN(i));
						target.setId(id);
						targets.add(target);
					}
				}
			} catch (Exception e) {
				CyberTrackerPlugIn.log(e.getMessage(), e);
			}
			
		}
		
		return targets;
	}

	@Override
	public boolean canFinish() {
		return page1.isPageComplete();
	}

}
