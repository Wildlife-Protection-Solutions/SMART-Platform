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
package org.wcs.smart.query.model.filter.date;

import java.sql.Date;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.wcs.smart.SmartContext;

/**
 * Last 30 days date filter
 * @author Emily
 *
 */
public enum Last30DaysDateFilter implements IDateFilter {

	INSTANCE;

	@Override
	public String getGuiName(Locale l) {
		return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getQueryKey() {
		return "last30days"; //$NON-NLS-1$
	}

	@Override
	public Date[] getDates() {
		Calendar cal = Calendar.getInstance();
		java.sql.Date currentDate = new java.sql.Date(cal.getTimeInMillis());
		cal.setTimeInMillis((long) (cal.getTime().getTime() - (30 * 24 * 60 * 60 * 1000.0)));
		java.sql.Date d = new java.sql.Date(cal.getTimeInMillis());
		return new java.sql.Date[] { d, currentDate };
	}
	@Override
	public String getLabel() {
		Date[] bits = getDates();
		DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
		return( "[" + formatter.format( bits[0] ) + " - " + formatter.format(bits[1]) +" ]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	@Override
	public String validate() {
		return null;
	}
	

	@Override
	public boolean isEndDateInclusive() {
		return true;
	}
}

