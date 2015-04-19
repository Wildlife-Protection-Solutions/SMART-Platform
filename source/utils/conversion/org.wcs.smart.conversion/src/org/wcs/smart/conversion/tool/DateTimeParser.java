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
package org.wcs.smart.conversion.tool;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Converts string to date.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class DateTimeParser {

	private static final DateFormat df_dot = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
	private static final DateFormat df_ct = new SimpleDateFormat("MM/dd/yyyy"); //$NON-NLS-1$
	private static final DateFormat df_dash = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
	private static final DateFormat df_dot_text = new SimpleDateFormat("dd.MMM.yy", Locale.US); //$NON-NLS-1$

	public Date parseDate(String dateStr) throws ParseException {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		} else if (dateStr.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{4}")) { //$NON-NLS-1$
			return df_dot.parse(dateStr);
		} else if (dateStr.matches("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}")) { //$NON-NLS-1$
			return df_ct.parse(dateStr);
		} else if (dateStr.matches("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}(\\s.*)*")) { //$NON-NLS-1$
			return df_dash.parse(dateStr.split(" ")[0]); //$NON-NLS-1$
		} else if (dateStr.matches("[0-9]{1,2}\\.[a-zA-Z]+\\.[0-9]{2}")) { //$NON-NLS-1$
			return df_dot_text.parse(dateStr);
		}
		System.err.println("Cannot parse date: " + dateStr);
		throw new ParseException(dateStr, 0);
	}
	
	public Time parseTime(String timeStr) throws ParseException {
		if (timeStr == null || timeStr.isEmpty())
			return null;
		try {
			timeStr = timeStr.replaceAll(";", ":"); //$NON-NLS-1$ //$NON-NLS-2$
			if (timeStr.matches("[0-9]{1,2}:[0-9]{1,2}")) { //$NON-NLS-1$
				timeStr += ":00"; //$NON-NLS-1$
			}
			return Time.valueOf(timeStr);
		} catch (Exception e) {
			System.err.println("Cannot parse time: " + timeStr);
			throw e;
		}
	}
	
}
