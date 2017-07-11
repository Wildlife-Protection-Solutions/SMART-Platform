/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.er.hibernate;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.SingleItemDataProvider;
import org.wcs.smart.qa.auto.AutoValidateJob;
import org.wcs.smart.qa.er.ErWaypointDataProvider;
import org.wcs.smart.qa.er.TrackLocationData;
import org.wcs.smart.qa.er.WaypointLocationData;


/**
 * Hibernate listener for new patrol waypoint and track objects.  Registers
 * objects for validation when new objects are generated. 
 * 
 * @author Emily
 *
 */
public class NewErObjectEventListener implements PostInsertEventListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5331984461884980437L;

	@Override
	public void onPostInsert(PostInsertEvent event) {
		Object x = event.getEntity();
		if (x instanceof SurveyWaypoint){
			//register waypoint for validation
			WaypointLocationData data = new WaypointLocationData(((SurveyWaypoint) x).getWaypoint());
			SingleItemDataProvider provider = new SingleItemDataProvider(RoutineExtensionManager.INSTANCE.findDataProvider(ErWaypointDataProvider.ID), data);
			AutoValidateJob.INSTANCE.addTask(provider);
		}else if (x instanceof MissionTrack){
			//register track for validation
			TrackLocationData data = new TrackLocationData((MissionTrack)x);
			SingleItemDataProvider provider = new SingleItemDataProvider(RoutineExtensionManager.INSTANCE.findDataProvider(ErWaypointDataProvider.ID), data);
			AutoValidateJob.INSTANCE.addTask(provider);
		}
	}
}
