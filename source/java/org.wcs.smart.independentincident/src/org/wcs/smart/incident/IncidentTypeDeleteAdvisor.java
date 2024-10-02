/*
 * Copyright (C) 2024 Wildlife Conservation Society
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

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.incident.model.IncidentType;
import org.wcs.smart.observation.model.Waypoint;


/**
 * Delete advisor for incident types
 */
public class IncidentTypeDeleteAdvisor implements IDeleteAdvisor {

	public IncidentTypeDeleteAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof IncidentType type)) return "Invalid object type"; //$NON-NLS-1$
		
		if (type.isSystem()) return "Cannot delete system incident type.";
		
		Long cnt = QueryFactory.buildCountQuery(session, Waypoint.class, 
				new Object[] {"incidentTypeUuid", type.getUuid()});
		
		if (cnt > 0) {
			return MessageFormat.format("The incident type ''{0}'' is associated with {1} waypoints.", type.getName(), cnt);
		}
		return null;
	}

}
