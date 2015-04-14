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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Converts string to date.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class DateParser {

	private static final DateFormat df_dot = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
	private static final DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); //$NON-NLS-1$

	public Date parse(String dateStr) throws ParseException {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		} else if (dateStr.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{4}")) { //$NON-NLS-1$
			return df_dot.parse(dateStr);
		} else if (dateStr.matches("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}")) { //$NON-NLS-1$
			return df.parse(dateStr);
		}
		System.err.println("Cannot parse date: " + dateStr);
		throw new ParseException(dateStr, 0);
	}
	
}
