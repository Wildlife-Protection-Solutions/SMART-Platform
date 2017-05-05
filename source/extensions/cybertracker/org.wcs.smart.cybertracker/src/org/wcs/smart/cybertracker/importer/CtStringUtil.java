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
package org.wcs.smart.cybertracker.importer;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Generic util to convert passed data to string.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CtStringUtil {

	public static final String listAsString(List<String> members, String separator) {
		StringBuilder result = new StringBuilder();
		for (Iterator<String> i = members.iterator(); i.hasNext();) {
			String e = i.next();
			result.append(e);
			if (i.hasNext())
				result.append(separator);
		}
		return result.toString();
	}

	public static final String dateAsString(Date date) {
		if (date == null) {
			return ""; //$NON-NLS-1$
		}
		return DateFormat.getDateTimeInstance().format(date);
	}
	
}
