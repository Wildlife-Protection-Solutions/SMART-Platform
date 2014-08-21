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
package org.wcs.smart.observation;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationOptions;

/**
 * Extension of the smart hibernate manager for observation related data.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationHibernateManager extends HibernateManager{
	
	/**
	 * Loads the patrol options for a given conservation area.
	 * <p>
	 * This is executed inside a transaction so you cannot
	 * run this inside a session with an open transaction.</p>
	 * <p>
	 * If patrol options does not exist, 
	 * it will create a new one with default values.
	 * </p>
	 * 
	 * @param ca conservation area 
	 * @param s active session
	 * @return the patrol options
	 */
	public static ObservationOptions getPatrolOptions(ConservationArea ca, Session s){
		try{
			ObservationOptions op = (ObservationOptions) s.get(ObservationOptions.class, ca.getUuid());
			if (op == null){
				op = createPatrolOption(ca, s);
			}
			return op;
		}catch (Exception ex){
			s.close();
			ObservationPlugIn.displayLog(Messages.PatrolHibernateManager_Error_CouldNoLoadPatrolOptions + ex.getLocalizedMessage(), ex);
		}
		return null;
	}

	public static Projection getCurrentViewProjection() {
		Session s = HibernateManager.openSession();
		try {
			return getCurrentViewProjection(s);
		} finally {
			s.close();
		}
	}

	public static Projection getCurrentViewProjection(Session s) {
		ObservationOptions observationOptions = getPatrolOptions(SmartDB.getCurrentConservationArea(), s);
		if (observationOptions != null) {
			Projection p = observationOptions.getViewProjection();
			if (p != null) {
				p.getDefinition(); //lazy load
			}
			return p;
		}
		return null;
	}
	
	/**
	 * Creates new patrol options for a given conservation area and saves it to the database.
	 * 
	 * @param ca conservation area 
	 * @param s active session
	 * @return
	 */
	public static ObservationOptions createPatrolOption(ConservationArea ca, Session s){
		ObservationOptions po = new ObservationOptions();
		po.setTrackDistanceDirection(false);
		po.setEditTime(null);
		po.setUuid(ca.getUuid());
		po.setTrackObserver(false);
		s.saveOrUpdate(po);
		return po;
	}
	
	
}
