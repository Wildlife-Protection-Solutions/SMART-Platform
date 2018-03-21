/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event;

import java.util.Locale;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Hibernate listener for audit items to update
 * date created, date modified, and employees fields
 * 
 * @author Emily
 *
 */
public class EventHibernateListener implements PostInsertEventListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void onPostInsert(PostInsertEvent event) {	
		if (event.getEntity() instanceof WaypointObservation) {
			WaypointObservation wo = (WaypointObservation)event.getEntity();
			StringBuilder sb = new StringBuilder();
			sb.append("NEW OBSERVATION: " + wo.getCategory().getHkey());
			for (WaypointObservationAttribute aa : wo.getAttributes()) {
				sb.append( " : " + aa.getAttribute().getKeyId() + " : " + aa.getAttributeValueAsString(Locale.getDefault()));
			}
			System.out.println(sb.toString());
			EventProcessingJob.getInstance().addObservation(wo);
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return false;
	}

	
}
