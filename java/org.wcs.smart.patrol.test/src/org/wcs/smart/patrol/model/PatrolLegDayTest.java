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
package org.wcs.smart.patrol.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;

public class PatrolLegDayTest {

	
	@Test
	public void testPatrolLegDayCreation2(){
		Patrol p = new Patrol();
		p.setStartDate(getDate(2011, 0, 22, 0, 0, 0));
		p.setEndDate(getDate(2011, 0, 29, 0, 0, 0));
		p.setLegs(new ArrayList<PatrolLeg>());
		
		
		//create a patrol leg from start to 3rd [end of day]
		PatrolLeg leg1 = new PatrolLeg();
		leg1.setStartDate(getDate(2011, 0, 22, 10, 0, 0));
		leg1.setEndDate(getDate(2011, 0, 24, 0, 0, 0));
		p.getLegs().add(leg1);
		
		PatrolLeg leg2 = new PatrolLeg();
		leg2.setStartDate(getDate(2011, 0, 24, 0, 0, 0));
		leg2.setEndDate(getDate(2011, 0, 29, 23, 59, 59));
		p.getLegs().add(leg2);
		
		p.createLegDays(null);
		
		Assert.assertEquals(2, leg1.getPatrolLegDays().size());
		Assert.assertEquals(6, leg2.getPatrolLegDays().size());
		
		Assert.assertTrue(dateEquals(leg1.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 22, 10,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 22, 10,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 22, 23,59,59)));
		Assert.assertTrue(dateEquals(leg1.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 23, 0,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 23, 0,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 23, 23,59,59)));
		
		for (int i = 24; i <= 29; i ++){
			Assert.assertTrue(dateEquals(leg2.getPatrolLegDays().get(i-24).getDate(), getDate(2011, 0, i, 0,0,0)));
			Assert.assertTrue(timeEquals(leg2.getPatrolLegDays().get(i-24).getStartTime(), getDate(2011, 0, i, 0,0,0)));
			Assert.assertTrue(timeEquals(leg2.getPatrolLegDays().get(i-24).getEndTime(), getDate(2011, 0, i, 23,59,59)));	
		}
	}
	
	
	@Test
	public void testPatrolLegDayCreation(){
		
		
		
		Patrol p = new Patrol();
		p.setStartDate(getDate(2011, 0, 1, 0, 0, 0));
		p.setEndDate(getDate(2011, 0, 31, 0, 0, 2));
		p.setLegs(new ArrayList<PatrolLeg>());
		
		//create a patrol leg from start to 3rd [end of day]
		PatrolLeg leg1 = new PatrolLeg();
		leg1.setStartDate(getDate(2011, 0, 1, 8, 0, 0));
		leg1.setEndDate(getDate(2011, 0, 3, 23, 59, 59));
		p.getLegs().add(leg1);
		
		//from 3rd [end of day] to 5th [end of day]
		PatrolLeg leg2 = new PatrolLeg();
		leg2.setStartDate(getDate(2011, 0, 3, 23, 59, 59 ));
		leg2.setEndDate(getDate(2011, 0, 5, 23, 59, 59 ));
		p.getLegs().add(leg2);

		//from 3rd [end of day] to 5th [end of day]
		PatrolLeg leg3 = new PatrolLeg();
		leg3.setStartDate(getDate(2011, 0, 3, 23, 59, 59 ));
		leg3.setEndDate(getDate(2011, 0, 5, 23, 59, 59 ));
		p.getLegs().add(leg3);

		//from 6th start of day to 7th start of day
		PatrolLeg leg4 = new PatrolLeg();
		leg4.setStartDate(getDate(2011, 0, 6, 0, 0, 0 ));
		leg4.setEndDate(getDate(2011, 0, 7, 0, 0, 0 ));
		p.getLegs().add(leg4);
		
		//from 7th start of day to 8 @ 10aM
		PatrolLeg leg5 = new PatrolLeg();
		leg5.setStartDate(getDate(2011, 0, 7, 0, 0, 0 ));
		leg5.setEndDate(getDate(2011, 0, 8, 10, 0, 0 ));
		p.getLegs().add(leg5);

		//from 8 @ 10aM to 10th end of day
		PatrolLeg  leg6 = new PatrolLeg();
		leg6.setStartDate(getDate(2011, 0, 8, 10, 0, 0 ));
		leg6.setEndDate(getDate(2011, 0, 10, 23, 59, 59 ));
		p.getLegs().add(leg6);

		//from 8 @ 10aM to 10th end of day
		PatrolLeg leg7 = new PatrolLeg();
		leg7.setStartDate(getDate(2011, 0, 8, 10, 0, 0 ));
		leg7.setEndDate(getDate(2011, 0, 10, 23, 59, 59 ));
		p.getLegs().add(leg7);
		
		//from 7th start of day to 10 end of day
		PatrolLeg leg8 = new PatrolLeg();
		leg8.setStartDate(getDate(2011, 0, 7, 0, 0, 0 ));
		leg8.setEndDate(getDate(2011, 0, 10, 23, 59, 59 ));
		p.getLegs().add(leg8);
		
		//end of day 10 to start of day 31		 
		PatrolLeg leg9 = new PatrolLeg();
		leg9.setStartDate(getDate(2011, 0, 10, 23, 59, 59 ));
		leg9.setEndDate(getDate(2011, 0, 31, 0, 0, 2 ));
		p.getLegs().add(leg9);
		
		
		p.createLegDays(null);
		
		////create a patrol leg from start to 3rd [end of day]
		//leg1 (1 @ 8AM to 3 [end of day]
		Assert.assertEquals(3, leg1.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg1.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 1, 8,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 1, 8,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 1, 23,59,59)));
		Assert.assertTrue(dateEquals(leg1.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 2, 0, 0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 2, 0,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 2, 23,59,59)));
		Assert.assertTrue(dateEquals(leg1.getPatrolLegDays().get(2).getDate(), getDate(2011, 0, 3, 0, 0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(2).getStartTime(), getDate(2011, 0, 3, 0,0,0)));
		Assert.assertTrue(timeEquals(leg1.getPatrolLegDays().get(2).getEndTime(), getDate(2011, 0, 3, 23,59,59)));
		
		
		//from 3rd [end of day] to 5th [end of day]		
		Assert.assertEquals(2, leg2.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg2.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 4, 0,0,0)));
		Assert.assertTrue(timeEquals(leg2.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 4, 0,0,0)));
		Assert.assertTrue(timeEquals(leg2.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 4, 23,59,59)));
		Assert.assertTrue(dateEquals(leg2.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 5, 0, 0,0)));
		Assert.assertTrue(timeEquals(leg2.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 5, 0,0,0)));
		Assert.assertTrue(timeEquals(leg2.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 5, 23,59,59)));
		
		//from 3rd [end of day] to 5th [end of day]
		Assert.assertEquals(2, leg3.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg3.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 4, 0,0,0)));
		Assert.assertTrue(timeEquals(leg3.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 4, 0,0,0)));
		Assert.assertTrue(timeEquals(leg3.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 4, 23,59,59)));
		Assert.assertTrue(dateEquals(leg3.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 5, 0, 0,0)));
		Assert.assertTrue(timeEquals(leg3.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 5, 0,0,0)));
		Assert.assertTrue(timeEquals(leg3.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 5, 23,59,59)));
		
		//from 6th start of day to 7th start of day
		Assert.assertEquals(1, leg4.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg4.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 6, 0,0,0)));
		Assert.assertTrue(timeEquals(leg4.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 6, 0,0,0)));
		Assert.assertTrue(timeEquals(leg4.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 6, 23,59,59)));

		//from 7th start of day to 8 @ 10aM
		Assert.assertEquals(2, leg5.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg5.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 7, 0,0,0)));
		Assert.assertTrue(timeEquals(leg5.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 7, 0,0,0)));
		Assert.assertTrue(timeEquals(leg5.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 7, 23,59,59)));
		Assert.assertTrue(dateEquals(leg5.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 8, 0,0,0)));
		Assert.assertTrue(timeEquals(leg5.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 8, 0,0,0)));
		Assert.assertTrue(timeEquals(leg5.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 8, 10,0,0)));
		
		
		//from 8 @ 10aM to 10th end of day
		Assert.assertEquals(3, leg6.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg6.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 8, 0,0,0)));
		Assert.assertTrue(timeEquals(leg6.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 8, 10,0,0)));
		Assert.assertTrue(timeEquals(leg6.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 8, 23,59,59)));
		Assert.assertTrue(dateEquals(leg6.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 9, 0,0,0)));
		Assert.assertTrue(timeEquals(leg6.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 9, 0,0,0)));
		Assert.assertTrue(timeEquals(leg6.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 9, 23,59,59)));
		Assert.assertTrue(dateEquals(leg6.getPatrolLegDays().get(2).getDate(), getDate(2011, 0, 10, 0,0,0)));
		Assert.assertTrue(timeEquals(leg6.getPatrolLegDays().get(2).getStartTime(), getDate(2011, 0, 10, 0,0,0)));
		Assert.assertTrue(timeEquals(leg6.getPatrolLegDays().get(2).getEndTime(), getDate(2011, 0, 10, 23,59,59)));
		
		//from 8 @ 10aM to 10th end of day
		Assert.assertEquals(3, leg7.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg7.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 8, 0,0,0)));
		Assert.assertTrue(timeEquals(leg7.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 8, 10,0,0)));
		Assert.assertTrue(timeEquals(leg7.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 8, 23,59,59)));
		Assert.assertTrue(dateEquals(leg7.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 9, 0,0,0)));
		Assert.assertTrue(timeEquals(leg7.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 9, 0,0,0)));
		Assert.assertTrue(timeEquals(leg7.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 9, 23,59,59)));
		Assert.assertTrue(dateEquals(leg7.getPatrolLegDays().get(2).getDate(), getDate(2011, 0, 10, 0,0,0)));
		Assert.assertTrue(timeEquals(leg7.getPatrolLegDays().get(2).getStartTime(), getDate(2011, 0, 10, 0,0,0)));
		Assert.assertTrue(timeEquals(leg7.getPatrolLegDays().get(2).getEndTime(), getDate(2011, 0, 10, 23,59,59)));
		
		//from 7th start of day to 10 end of day
		Assert.assertEquals(4, leg8.getPatrolLegDays().size());
		Assert.assertTrue(dateEquals(leg8.getPatrolLegDays().get(0).getDate(), getDate(2011, 0, 7, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(0).getStartTime(), getDate(2011, 0, 7, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(0).getEndTime(), getDate(2011, 0, 7, 23,59,59)));
		Assert.assertTrue(dateEquals(leg8.getPatrolLegDays().get(1).getDate(), getDate(2011, 0, 8, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(1).getStartTime(), getDate(2011, 0, 8, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(1).getEndTime(), getDate(2011, 0, 8, 23,59,59)));
		Assert.assertTrue(dateEquals(leg8.getPatrolLegDays().get(2).getDate(), getDate(2011, 0, 9, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(2).getStartTime(), getDate(2011, 0, 9, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(2).getEndTime(), getDate(2011, 0, 9, 23,59,59)));
		Assert.assertTrue(dateEquals(leg8.getPatrolLegDays().get(3).getDate(), getDate(2011, 0, 10, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(3).getStartTime(), getDate(2011, 0, 10, 0,0,0)));
		Assert.assertTrue(timeEquals(leg8.getPatrolLegDays().get(3).getEndTime(), getDate(2011, 0, 10, 23,59,59)));
		
		//end of day 10 to start of day 31
		Assert.assertEquals(21, leg9.getPatrolLegDays().size());
		for (int i = 0; i < 20; i ++){
			Assert.assertTrue(dateEquals(leg9.getPatrolLegDays().get(i).getDate(), getDate(2011, 0, 11+i, 0,0,0)));
			Assert.assertTrue(timeEquals(leg9.getPatrolLegDays().get(i).getStartTime(), getDate(2011, 0, 11+i, 0,0,0)));
			Assert.assertTrue(timeEquals(leg9.getPatrolLegDays().get(i).getEndTime(), getDate(2011, 0, 11+i, 23,59,59)));	
		}
		Assert.assertTrue(dateEquals(leg9.getPatrolLegDays().get(20).getDate(), getDate(2011, 0, 31, 0,0,2)));
		Assert.assertTrue(timeEquals(leg9.getPatrolLegDays().get(20).getStartTime(), getDate(2011, 0, 31, 0,0,0)));
		Assert.assertTrue(timeEquals(leg9.getPatrolLegDays().get(20).getEndTime(), getDate(2011, 0, 31, 0,0,2)));
		 
		
	}
	
	private boolean timeEquals(Date d1, Date d2){
		Calendar cal1 = GregorianCalendar.getInstance();
		cal1.setTime(d1);
		Calendar cal2 = GregorianCalendar.getInstance();
		cal2.setTime(d2);
		
		return cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) &&
				cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE) && 
				cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND);
	}
	
	private boolean dateEquals(Date d1, Date d2){
		Calendar cal1 = GregorianCalendar.getInstance();
		cal1.setTime(d1);
		Calendar cal2 = GregorianCalendar.getInstance();
		cal2.setTime(d2);
		
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && 
				cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
	}
	private Date getDate(int year, int month, int day, int hour, int minute, int second){
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		return cal.getTime();
	}
}
