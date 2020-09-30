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
package org.wcs.smart.patrol.internal.ui.importwp.gpsbabel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Date time conversion tests.
 * 
 * @author Emily
 *
 */
public class CovertDateTimeTest {

	
	@Test
	public void testDateTimeConversion(){
		
		String dates[][] = {
			new String[]{"01-jan-2001 11:00:00AM","Mon Jan 01 11:00:00 PST 2001"},
			new String[]{"01-jan-2001 12:00:00AM","Mon Jan 01 00:00:00 PST 2001"},
			new String[]{"01-jan-2001 1:00:00PM","Mon Jan 01 13:00:00 PST 2001"},
			new String[]{"01-jan-2001 12:00:00PM","Mon Jan 01 12:00:00 PST 2001"},
			new String[]{"01-jan-2001 11:00:00", "Mon Jan 01 11:00:00 PST 2001"},
			new String[]{"01-jan-2001 12:00:00", "Mon Jan 01 12:00:00 PST 2001"},
			new String[]{"01-jan-2001 12:46:00", "Mon Jan 01 12:46:00 PST 2001"},
			new String[]{"01-jan-2001 13:00:00", "Mon Jan 01 13:00:00 PST 2001"},
			new String[]{"01-jan-2001 00:00:00", "Mon Jan 01 00:00:00 PST 2001"}
		};
		
		for (String[] str : dates){
			Date dt = convertDateTime(str[0]);
			System.out.println(str[0] + ":" + dt.toString());
			Assert.assertEquals(str[1], dt.toString());
		}
		
	}
	
	public Date convertDateTime(String date){
		Date wpdt = null;
		if (wpdt == null) {
			try {
				// am-pm
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy h:mm:ssa"); //$NON-NLS-1$
				wpdt = sdf.parse(date);
			} catch (ParseException e) {
			}
		}
		if (wpdt == null) {
			try {
				// 24hr
				SimpleDateFormat sdf = new SimpleDateFormat(
						"dd-MMM-yy HH:mm:ss"); //$NON-NLS-1$
				wpdt = sdf.parse(date);
			} catch (ParseException e) {
			}
		}
		if (wpdt == null) {
			try {
				// short
				wpdt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).parse(
						date);
			} catch (ParseException e) {
			}
		}
		if (wpdt == null) {
			try {
				// medium
				wpdt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).parse(
						date);
			} catch (ParseException e) {
			}
		}
		if (wpdt == null) {
			try {
				// medium
				wpdt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).parse(
						date);
			} catch (ParseException e) {
			}
		}

		if (wpdt == null) {
			try {
				// long
				wpdt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).parse(
						date);
			} catch (ParseException e) {
			}
		}
		return wpdt;
	}
}
