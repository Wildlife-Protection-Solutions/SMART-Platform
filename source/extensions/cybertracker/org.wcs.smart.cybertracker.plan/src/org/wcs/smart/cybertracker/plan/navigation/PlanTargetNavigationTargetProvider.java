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
package org.wcs.smart.cybertracker.plan.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.cybertracker.navigation.ui.INavigationLayerTargetProvider;
import org.wcs.smart.cybertracker.plan.internal.Messages;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;

/**
 * Navigation layer target provider for patrol tracks.
 * 
 * @author Emily
 *
 */
public class PlanTargetNavigationTargetProvider implements INavigationLayerTargetProvider {

	private PlanTargetWizardPage page1;
	
	@Override
	public String getTypeName() {
		return Messages.PlanTargetNavigationTargetProvider_TypeName;
	}

	@Override
	public Image getImage() {
		return SmartPlanPlugIn.getDefault().getImageRegistry().get(SmartPlanPlugIn.PLAN_ICON);
	}
	
	@Override
	public List<WizardPage> getPages() {
		if (page1 == null) {
			page1 = new PlanTargetWizardPage();
		}
		return Collections.singletonList(page1);
	}

	@Override
	public List<NavigationTarget> getTargets(IProgressMonitor monitor) {
		
		List<SpatialPlanTarget> parts = page1.getPlanTargets();
		List<NavigationTarget> targets = new ArrayList<>();
		
		for (SpatialPlanTarget part : parts) {
			int cnt = 1;
			for (SpatialPlanTargetPoint p : part.getPoints()) {
				NavigationTarget target = new NavigationTarget();
				target.setGeometry(GeometryFactoryProvider.getFactory().createPoint( new Coordinate(p.getX(), p.getY())));
				target.setId( part.getName() + " (" + (cnt++) + ")" ); //$NON-NLS-1$ //$NON-NLS-2$
				targets.add(target);
			}
		}
		
		return targets;
	}

	@Override
	public boolean canFinish() {
		return page1.isPageComplete();
	}

}
