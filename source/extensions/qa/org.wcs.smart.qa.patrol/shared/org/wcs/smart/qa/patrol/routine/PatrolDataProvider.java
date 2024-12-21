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
package org.wcs.smart.qa.patrol.routine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.patrol.ILabelProvider;
import org.wcs.smart.qa.patrol.ILabelProvider.Key;

/**
 * Data provider for patrol waypoints.
 * 
 * @author Emily
 *
 */
public class PatrolDataProvider extends IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.patrol"; //$NON-NLS-1$
		
	@Override
	public String getName(Locale l) {
		return ILabelProvider.getLabel(Key.PatrolDataProvider_Name, l);
	}

	public String getId(){
		return ID;
	}
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, LocalDate startDate, LocalDate endDate) {
		List<Patrol> patrols = session.createQuery("FROM Patrol WHERE conservationArea = :ca and startDate <= :end and endDate >=  :start", Patrol.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.setParameter("start", startDate) //$NON-NLS-1$
			.setParameter("end", endDate) //$NON-NLS-1$
			.list();
		//TODO: limit to maximum number?
		List<PatrolLocationData> items = new ArrayList<>(patrols.size());
		for (Patrol p : patrols) {
			items.add(new PatrolLocationData(p));
		}
		
		return items;
	}

	@Override
	public String getFeatureId(Session session, Object obj, Locale l){
		Patrol p = session.get(Patrol.class,  ((PatrolLocationData)obj).getPatrol().getUuid());
		if (p == null){
			return ILabelProvider.getLabel(Key.PatrolDataProvider_PatrolNotFound, l);
		}
		return p.getId();		
	}
	
	@Override
	public boolean supportsRoutine(IQaRoutineType type) {
		if (type.getId().equals(EmptyEndPatrolDaysType.ID)) return true;
		return false;
	}

	@Override
	public UUID getFeatureSource(Session session, Object obj) {
		return ((PatrolLocationData)obj).getPatrol().getUuid();
	}

	@Override
	public boolean recheck(Session session, QaError error) {
		Patrol patrol = session.get(Patrol.class, error.getSourceId());
		if (patrol == null) return false;
		
		//we don't check geometries for this one
		//its not part of the auto qa check.
		return true;		
	}
}
