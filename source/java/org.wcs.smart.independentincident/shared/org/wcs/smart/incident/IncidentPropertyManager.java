/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Manager for processing incident properties
 * @author Emily
 *
 */
public enum IncidentPropertyManager {
	
	INSTANCE;
	
	public enum IncidentProperty{
	
		INTEGRATE_TO_PATROL_DISTANCE("incident.integratetopatrol.maxdistance", 50), //$NON-NLS-1$
		INTEGRATE_TO_PATROL_EXPIRE("incident.integratetopatrol.expire", 180); //$NON-NLS-1$
		
		public final String key;
		public final int defaultValue; 
		private IncidentProperty(String key, int defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}
	}

	public int getSetting(Session session, ConservationArea ca, IncidentProperty property) {
		
		ConservationAreaProperty prop = QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
			new Object[] {"conservationArea", ca}, //$NON-NLS-1$
			new Object[] {"key", property.key }).uniqueResult(); //$NON-NLS-1$
		
		if (prop == null) {
			return property.defaultValue;
		}
		return Integer.parseInt(prop.getValue());
	}
	
	public void updateSetting(Session session, ConservationArea ca, IncidentProperty property, int newValue) {
		ConservationAreaProperty prop = QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"key", property.key }).uniqueResult(); //$NON-NLS-1$
			
		if (prop == null) {
			prop = new ConservationAreaProperty ();
			prop.setConservationArea(ca);
			prop.setKey(property.key);
			session.persist(prop);
		}
		prop.setValue(String.valueOf(newValue));		
	}
}
