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
package org.wcs.smart.patrol;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.IdGeneratorEngine;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;

/**
 * Patrol ID Generator
 * 
 * @author Emily
 *
 */
public enum PatrolIdGenerator {
	
	INSTANCE;
	
	private static NumberFormat PATROL_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$

	public static final String PATTERN_PROPERY_KEY = "patrol.id.pattern"; //$NON-NLS-1$
	public static final String UNIQUE_PROPERTY_KEY = "patrol.id.unique"; //$NON-NLS-1$
	
	public static final String UNIQUE_VALUE = "true"; //$NON-NLS-1$
	public static final String NOTUNIQUE_VALUE = "false"; //$NON-NLS-1$
	
	/**
	 * Computes the next patrol id;
	 * 
	 * @param p patrol to compute id for; the conservation area property must be set
	 * @param s active session (should be inside the transaction that is saving patrol)
	 * 
	 * @return patrol id for given patrol
	 */
	//TODO: consider implementing regex expression in derby
	//and using that instead of loading all patrol ids
	//(was testing for ~32000 and performance was not affect)
	public String generatePatrolId(Patrol p, Session s) {
		
		if (p.getConservationArea() == null) throw new RuntimeException("Conservation area required for computing patrol id"); //$NON-NLS-1$
		
		ConservationAreaProperty prop = QueryFactory.buildQuery(s, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", p.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"key", PATTERN_PROPERY_KEY}).uniqueResult(); //$NON-NLS-1$
		if (prop == null || prop.getValue() == null || prop.getValue().strip().isBlank()) {
		
			StringBuilder sb = new StringBuilder();
			sb.append(p.getConservationArea().getId());
	
			Query<?> q = s
					.createQuery("SELECT id FROM Patrol WHERE id like :id and conservationArea = :ca"); //$NON-NLS-1$
			q.setParameter("id", sb.toString() + "%_%"); //$NON-NLS-1$ //$NON-NLS-2$
			q.setParameter("ca", p.getConservationArea()); //$NON-NLS-1$
	
			long idNumber = 0;
			List<?> results = q.list();
			for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
				String localId = (String) iterator.next();
				try {
					int idx = localId.lastIndexOf('_');
					String keypart = localId.substring(0, idx);
					if (keypart.equalsIgnoreCase(p.getConservationArea().getId())){
						String numpart = localId.substring(idx+1);
						long tmp = Integer.parseInt(numpart);
						if (tmp > idNumber) idNumber = tmp;
					}
					//break;
				} catch (Exception ex) {
					// not of the form CAID_# skip this one
				}
			}
			sb.append("_"); //$NON-NLS-1$
			idNumber = (idNumber + 1) % 1000000;
			if (idNumber <= 0) {
				idNumber = 1;
			}
			sb.append(PATROL_ID_FORMATTER.format(idNumber));

			return sb.toString();
		}
		
		String pattern = prop.getValue();
		Employee leader = null;
		for (PatrolLeg l : p.getLegs()) {
			leader = l.getLeader().getMember();
		}
		
		//find earliest time
		LocalDateTime startDateTime = null;
		for (PatrolLeg leg : p.getLegs()) {
			for (PatrolLegDay d : leg.getPatrolLegDays()) {
				if (startDateTime == null || d.getDate().atTime(d.getStartTime()).isBefore(startDateTime)) {
					startDateTime = d.getDate().atTime(d.getStartTime());
				}
			}
		}
		if (startDateTime == null) startDateTime = p.getStartDate().atStartOfDay();
		
		
		HashMap<String, Employee> employees = new HashMap<>();
		employees.put(IdGeneratorEngine.LEADER_KEY,leader);
		String nextId = IdGeneratorEngine.INSTANCE.generateId(pattern, startDateTime, employees);

		prop = QueryFactory.buildQuery(s, ConservationAreaProperty.class, 
				new Object[] {"conservationArea", p.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"key", UNIQUE_PROPERTY_KEY}).uniqueResult(); //$NON-NLS-1$
		if (prop == null || prop.getValue() == null || prop.getValue().strip().isBlank() || prop.getValue().equalsIgnoreCase(NOTUNIQUE_VALUE)) {
			return nextId;
		}
		
		int cnt = 1;
		String id = nextId;
		
		while(true) {
			Long number = (Long)s.createQuery("SELECT count(*) FROM Patrol WHERE id = :id AND conservationArea = :ca") //$NON-NLS-1$
					.setParameter("id", id) //$NON-NLS-1$
					.setParameter("ca", p.getConservationArea()) //$NON-NLS-1$
					.uniqueResult();
			if (number == 0) return id;
			
			id = nextId + "_" + cnt; //$NON-NLS-1$
			if (id.length() > Patrol.MAX_ID_LENGTH) {
				String part ="_" + cnt; //$NON-NLS-1$
				id = nextId.substring(0,  nextId.length() - 1 - part.length()) + part;
			}
			cnt++;
		}
	}
}
