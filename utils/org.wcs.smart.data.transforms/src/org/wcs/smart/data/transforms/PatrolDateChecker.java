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
import java.util.Calendar;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolType;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

/**
 * This function finds any patrols longer than a given time frame
 * or with dates outside a range.
 * 
 * <p>This checker can take two parameters: first is the length of the patrol,
 * the second is the earliest date for patrols in the form "d-m-yyyy".  If not
 * provided default values of 30 and jan 1 1980 are used.</p>
 * 
 * @author Emily
 *
 */
public class PatrolDateChecker implements IDataProcessor {

	private int maxPatrolLength = 30;
	private XMLGregorianCalendar earliestDate = null;
	
	public PatrolDateChecker(){
		earliestDate = new XMLGregorianCalendarImpl();
		earliestDate.setDay(1);
		earliestDate.setMonth(1);
		earliestDate.setYear(1980);
		
		maxPatrolLength = 30;
	}
	private void findEmptyLegDays(PatrolType patrol){
		XMLGregorianCalendar today = new XMLGregorianCalendarImpl();
		Calendar c = Calendar.getInstance();
		today.setDay(c.get(Calendar.DAY_OF_MONTH));
		today.setMonth(c.get(Calendar.MONTH));
		today.setYear(c.get(Calendar.YEAR));
		
		if (patrol.getStartDate().compare(earliestDate) == DatatypeConstants.LESSER ||
				patrol.getEndDate().compare(today) == DatatypeConstants.GREATER){
			System.out.println("Patrol " + patrol.getId() + " outside date range (" + patrol.getStartDate() + " to " + patrol.getEndDate() + ")");
			
		}
	
		int cnt = 0;
		for (PatrolLegType leg : patrol.getLegs()){
			cnt+= leg.getDays().size();
		}
		if (cnt > maxPatrolLength){
			System.out.println("Patrol " + patrol.getId() + " has more than " + maxPatrolLength + " leg days (" + cnt + ")");
		}
		
	}
	
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		findEmptyLegDays(pt);
	}
	
	
	public static void main(String args[]){

		PatrolDateChecker pdc = new PatrolDateChecker();
		if (args.length >=3 ){
			pdc.maxPatrolLength = Integer.parseInt(args[2]);
		}
		if (args.length >= 4){
			String[] bits = args[3].split("-");
			XMLGregorianCalendarImpl date = new XMLGregorianCalendarImpl();
			date.setDay(Integer.parseInt(bits[0]));
			date.setMonth(Integer.parseInt(bits[1]));
			date.setYear(Integer.parseInt(bits[2]));
			pdc.earliestDate = date;
		}
		
		System.out.print("Running Patrol Date Checker: ");
		System.out.print("Maxiumum Patrol Length: " + pdc.maxPatrolLength);
		System.out.println(";  Earliest Patrol Date: " + pdc.earliestDate);
		DataUtils.processConfiguration(args, pdc, false);
	}
}
