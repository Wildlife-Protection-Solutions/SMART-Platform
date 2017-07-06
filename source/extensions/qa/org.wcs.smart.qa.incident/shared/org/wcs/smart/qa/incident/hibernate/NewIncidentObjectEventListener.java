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
package org.wcs.smart.qa.incident.hibernate;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.SingleItemDataProvider;
import org.wcs.smart.qa.auto.AutoValidateJob;
import org.wcs.smart.qa.incident.IncidentDataProvider;
import org.wcs.smart.qa.incident.IncidentLocationData;

/**
 * Hibernate listener for new incidents.  Registers
 * objects for validation when new objects are generated. 
 * 
 * @author Emily
 *
 */
public class NewIncidentObjectEventListener implements PostInsertEventListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5331984461884980437L;

	@Override
	public void onPostInsert(PostInsertEvent event) {
		Object x = event.getEntity();
		if (x instanceof Waypoint && ((Waypoint)x).getSourceId().equals(IndepedentIncidentSource.KEY)){
			//register waypoint for validation
			IncidentLocationData data = new IncidentLocationData((Waypoint) x);
			SingleItemDataProvider provider = new SingleItemDataProvider(RoutineExtensionManager.INSTANCE.findDataProvider(IncidentDataProvider.ID), data);
			AutoValidateJob.INSTANCE.addTask(provider);
		}
	}
}
