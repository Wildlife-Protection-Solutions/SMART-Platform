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
package org.wcs.smart.qa.routine;

import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;

/**
 * Provides data to QA routines for validation.
 * 
 * @author Emily
 *
 */
public abstract class IQaDataProvider {

	/**
	 * A unique identifier for the data provider.
	 * @return
	 */
	public abstract String getId();
	
	/**
	 * The name of the data provider
	 * 
	 * @param l
	 * @return
	 */
	public abstract String getName(Locale l);
	
	/**
	 * Find all the data between the start and end date.
	 * 
	 * @param startDate
	 * @param endDate
	 * 
	 * @return
	 */
	public abstract Collection<?> getData(Session session, ConservationArea conservationArea, Date startDate, Date endDate);
	
	/**
	 * Determines if a given QA Routine Type supports data
	 * from this data provider.
	 * 
	 * @param type
	 * @return
	 */
	public abstract boolean supportsRoutine(IQaRoutineType type);
	
	/**
	 * For an object returned by getData call, this function determines the user
	 * friendly ID that should be displayed in the results
	 * 
	 * @param session
	 * @param obj
	 * @return
	 */
	public abstract String getFeatureId(Session session, Object obj);
	
	
	/**
	 * From an object returned by the getData call, the function determines
	 * the UUID that uniquely identifies the data in error
	 * @param session
	 * @param obj
	 * @return
	 */
	public abstract UUID getFeatureSource(Session session, Object obj);
	
	@Override
	public boolean equals(Object other){
		if (other == null) return false;
		if (!other.getClass().equals(this.getClass())) return false;
		return Objects.equals(getId(), ((IQaDataProvider)other).getId());
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(getId());
	}

}
