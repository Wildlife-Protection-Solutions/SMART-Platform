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
package org.wcs.smart.cybertracker;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
/**
 * Clones the cybertracker properties when creating
 * a new conservation area from a template.
 * 
 * @author Emily
 *
 */
public class CybertrackerTemplateCloner implements
		IConservationAreaTemplateCloner {

	public CybertrackerTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
	
		@SuppressWarnings("unchecked")
		List<CyberTrackerProperties> list = engine.getSession().createCriteria(CyberTrackerProperties.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).list(); //$NON-NLS-1$
		if (list.size() > 0){
			CyberTrackerProperties templateProperties = list.get(0);
			
			CyberTrackerProperties newProperties = new CyberTrackerProperties();
			newProperties.setApplicationName(templateProperties.getApplicationName());
			newProperties.setAutoNext(templateProperties.isAutoNext());
			newProperties.setConservationArea(engine.getNewCa());
			newProperties.setExitPin(templateProperties.getExitPin());
			newProperties.setGpsTimeZone(templateProperties.getGpsTimeZone());
			newProperties.setKioskMode(templateProperties.isKioskMode());
			newProperties.setLargeScrollBars(templateProperties.isLargeScrollBars());
			newProperties.setSightingAccuracy(templateProperties.getSightingAccuracy());
			newProperties.setSightingFixCount(templateProperties.getSightingFixCount());
			newProperties.setSkipButtonTimeout(templateProperties.getSkipButtonTimeout());
			newProperties.setStorageTime(templateProperties.getStorageTime());
			newProperties.setWaypointTimer(templateProperties.getWaypointTimer());
			
			engine.getSession().save(newProperties);
			engine.getSession().flush();
		}
	}

}
