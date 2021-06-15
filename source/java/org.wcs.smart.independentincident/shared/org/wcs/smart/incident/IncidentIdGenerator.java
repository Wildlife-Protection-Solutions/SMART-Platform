/*
 * Copyright (C) 2021 Wildlife Conservation Society
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.IdGeneratorEngine;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Incident Id Generator
 * 
 * @author Emily
 *
 */
public enum IncidentIdGenerator {
	
	INSTANCE;
	
	public static final String PATTERN_PROPERY_KEY = "incident.id.pattern"; //$NON-NLS-1$
	public static final String UNIQUE_PROPERTY_KEY = "incident.id.unique"; //$NON-NLS-1$
	
	public static final String UNQIUE_VALUE = "true"; //$NON-NLS-1$
	public static final String NOTUNIQUE_VALUE = "false"; //$NON-NLS-1$
	
	/**
	 * Computes the next incident ID
	 * 
	 * @param session
	 * @param incidentsources set of incident source to search for determining
	 * if generated id is unique or not
	 * 
	 * @return
	 */
	public String getNextIncidentId(Session session, ConservationArea ca, Set<String> incidentsources, Employee observer) {
		
		ConservationAreaProperty prop = QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"key", PATTERN_PROPERY_KEY}).uniqueResult(); //$NON-NLS-1$
		
		if (prop == null || prop.getValue() == null || prop.getValue().trim().isBlank()) {
			
			Query<?> q = session.createQuery("SELECT count(*) FROM Waypoint WHERE sourceId IN (:source) AND conservationArea = :ca"); //$NON-NLS-1$
			q.setParameterList("source", incidentsources); //$NON-NLS-1$
			q.setParameter("ca", ca); //$NON-NLS-1$
			List<?> maxIs = q.list();
			long id = 1;
			if (maxIs.size() > 0 && maxIs.get(0) != null ) id = (Long) maxIs.get(0);
			return String.valueOf(id);
		}
		
		//find observation
		Map<String, Employee> employees = new HashMap<>();
		employees.put(IdGeneratorEngine.OBSERVER_KEY, observer);
		String nextId = IdGeneratorEngine.INSTANCE.generateId(prop.getValue(), employees);
		
		prop = QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"key", UNIQUE_PROPERTY_KEY}).uniqueResult(); //$NON-NLS-1$
		if (prop == null || prop.getValue() == null || prop.getValue().equalsIgnoreCase(NOTUNIQUE_VALUE)) return nextId;
		
		//make this id unique by adding _x on the end
		int cnt = 1;
		String id = nextId;
		
		while(true) {
			Query<?> q = session.createQuery("SELECT count(*) FROM Waypoint WHERE sourceId IN (:source) AND conservationArea = :ca AND id = :id"); //$NON-NLS-1$
			q.setParameterList("source", incidentsources); //$NON-NLS-1$
			q.setParameter("ca", ca); //$NON-NLS-1$
			q.setParameter("id", id); //$NON-NLS-1$
			Long number = (Long) q.uniqueResult();
			if (number == 0) return id;
			
			id = nextId + "_" + cnt; //$NON-NLS-1$
			if (id.length() > Waypoint.ID_MAX_LENGTH) {
				String part ="_" + cnt; //$NON-NLS-1$
				id = nextId.substring(0,  nextId.length() - 1 - part.length()) + part;
			}
			cnt++;
		}
		
	}
}
