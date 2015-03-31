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
package org.wcs.smart.data.transforms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;

/**
 * <p>
 * This processor removes any days from the start and/or end of the patrol that have 
 * no observations.  Days in the middle without observations are not affected.  If this results
 * in a patrol with no days an error is printed to standard out.
 * <p>
 * 
 * @author Emily
 *
 */
public class LeadTailEmptyDayDeleter implements IDataProcessor {

	@Override
	public void processFile(File in, File out) throws Exception {
		PatrolType pt = DataUtils.readPatrol(in);
		
		boolean isBeginning = true;
		List<PatrolLegDayType> toDeleteStart = new ArrayList<PatrolLegDayType>();
		List<PatrolLegDayType> toDeleteEnd = new ArrayList<PatrolLegDayType>();
		
		int cnt = 0;
		for (PatrolLegType plt : pt.getLegs()){
			for (PatrolLegDayType day : plt.getDays()){
				cnt++;
				if (day.getWaypoints().isEmpty()){
					if (isBeginning){
						toDeleteStart.add(day);
					}
					toDeleteEnd.add(day);
				}else{
					isBeginning = false;
					toDeleteEnd.clear();
				}
			}
		}
			
		if (toDeleteEnd.size() == cnt){
			System.err.println("Removing empty patrol days will results in patrol with no days.  Patrol ID: " + pt.getId());
		}
		
		List<PatrolLegType> plToDelete = new ArrayList<PatrolLegType>();
		for (PatrolLegType plt: pt.getLegs()){
			plt.getDays().removeAll(toDeleteStart);
			plt.getDays().removeAll(toDeleteEnd);
			if (plt.getDays().isEmpty()){
				plToDelete.add(plt);
			}
		}
		pt.getLegs().removeAll(plToDelete);
		
		XMLGregorianCalendar startDate = null;
		XMLGregorianCalendar endDate = null;
		
		for (PatrolLegType plt: pt.getLegs()){
			XMLGregorianCalendar plstartDate = null;
			XMLGregorianCalendar plendDate = null;
			
			for (PatrolLegDayType day : plt.getDays()){
				if (plstartDate == null){
					plstartDate = day.getDate();
				}
				if (plendDate == null){
					plendDate = day.getDate();
				}
				if (day.getDate().compare(plstartDate) == DatatypeConstants.LESSER ){
					plstartDate = day.getDate();
				}
				if (day.getDate().compare(plendDate) == DatatypeConstants.GREATER){
					plendDate = day.getDate();
				}
			}
			
			plt.setStartDate(plstartDate);
			plt.setEndDate(plendDate);
			
			if (startDate == null){
				startDate = plt.getStartDate();
			}
			if (endDate == null){
				endDate = plt.getEndDate();
			}
			if ( startDate.compare(plt.getStartDate()) == DatatypeConstants.GREATER ){
				startDate = plt.getStartDate();
			}
			if ( endDate.compare(plt.getEndDate()) == DatatypeConstants.LESSER ){
				endDate = plt.getEndDate();
			}
		}
		
		pt.setStartDate(startDate);
		pt.setEndDate(endDate);
		
		DataUtils.writePatrol(out, pt);

	}
	
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new LeadTailEmptyDayDeleter());
	}

}
