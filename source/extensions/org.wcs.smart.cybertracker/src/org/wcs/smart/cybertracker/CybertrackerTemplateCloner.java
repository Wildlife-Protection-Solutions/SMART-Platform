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
			newProperties.setConservationArea(engine.getNewCa());
			newProperties.setApplicationName(templateProperties.getApplicationName());
			newProperties.setUseTitleBar(templateProperties.isUseTitleBar());
			newProperties.setUseLargeTitles(templateProperties.isUseLargeTitles());
			newProperties.setLargeScrollBars(templateProperties.isLargeScrollBars());
			newProperties.setUseLargeTabs(templateProperties.isUseLargeTabs());
			newProperties.setAutoNext(templateProperties.isAutoNext());

			newProperties.setKioskMode(templateProperties.isKioskMode());
			newProperties.setDisableEditing(templateProperties.isDisableEditing());
			newProperties.setUseSdCard(templateProperties.isUseSdCard());
			newProperties.setTestTime(templateProperties.isTestTime());
			newProperties.setResetOnSync(templateProperties.isResetOnSync());
			newProperties.setResetOnNext(templateProperties.isResetOnNext());
			newProperties.setExitPin(templateProperties.getExitPin());
			newProperties.setStorageTime(templateProperties.getStorageTime());
			
			newProperties.setSightingAccuracy(templateProperties.getSightingAccuracy());
			newProperties.setSightingFixCount(templateProperties.getSightingFixCount());
			newProperties.setTrackAccuracy(templateProperties.getTrackAccuracy());
			newProperties.setWaypointTimer(templateProperties.getWaypointTimer());
			newProperties.setUseGpsTime(templateProperties.isUseGpsTime());
			newProperties.setGpsTimeZone(templateProperties.getGpsTimeZone());
			newProperties.setSkipButtonTimeout(templateProperties.getSkipButtonTimeout());
			newProperties.setManualGps(templateProperties.isManualGps());
			newProperties.setAllowSkipManualGps(templateProperties.isAllowSkipManualGps());

			newProperties.setFieldMapFilename(templateProperties.getFieldMapFilename());
			newProperties.setLock100(templateProperties.isLock100());
			newProperties.setUseMapOnSkip(templateProperties.isUseMapOnSkip());
			
			
			engine.getSession().save(newProperties);
			engine.getSession().flush();
		}
	}

}
