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

import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;

/**
 * This function finds any patrol leg day with no observations and
 * prints the results to standard out (per day). 
 * 
 * @author Emily
 *
 */
public class EmptyDayFinder implements IDataProcessor {

	
	private void findEmptyLegDays(PatrolType patrol){
		
		List<XMLGregorianCalendar> emptyDays = new ArrayList<XMLGregorianCalendar>();
		for (PatrolLegType leg : patrol.getLegs()){
			for (PatrolLegDayType day : leg.getDays()){
				if (day.getWaypoints().size() == 0){
					if (!emptyDays.contains(day.getDate())){
						emptyDays.add(day.getDate());
					}
				}
			}
		}
		if (emptyDays.size() > 0){
			StringBuilder sb = new StringBuilder("Patrol: " + patrol.getId() + " - Empty Days (d/m/y):");
			for (XMLGregorianCalendar c : emptyDays){
				sb.append(c.getDay() + "/" + c.getMonth() + "/" + c.getYear() + "  ");
			}
			System.out.println(sb.toString());
		}
	}
	
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		findEmptyLegDays(pt);
	}
	
	
	public static void main(String args[]){
		DataUtils.processConfiguration(args, new EmptyDayFinder(), false);
	}
}
