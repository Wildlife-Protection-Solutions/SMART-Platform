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


import java.util.Date;
import java.util.Locale;

import org.wcs.smart.SmartContext;

/**
 * Custom date date filter.
 * @author Emily
 *
 */
public class CustomDateFilter implements IDateFilter {

	public static final String KEY = "custom"; //$NON-NLS-1$
	
	private Date startDate = new Date();
	private Date endDate = new Date();
	
	@Override
	public String getGuiName(Locale l) {
		return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getQueryKey() {
		return KEY; 
	}

	@Override
	public java.sql.Date[] getDates() {
		return new java.sql.Date[]{new java.sql.Date(startDate.getTime()), new java.sql.Date(endDate.getTime())};
	}
	
	public void setDates(Date start, Date end){
		this.startDate = start;
		this.endDate = end;
	}
	

	@Override
	public boolean isEndDateInclusive() {
		return true;
	}

	@Override
	public String getLabel() {
//		Date[] bits = getDates();
//		DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
//		return( "[" + formatter.format( bits[0] ) + " - " + formatter.format(bits[1]) +" ]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return ""; //$NON-NLS-1$
	}

	@Override
	public String validate() {
		if (startDate.after(endDate)){
			return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).
					getLabel(IQueryDateLabelProvider.START_BEFORE_END_ERR, Locale.getDefault());
		}
		return null;
	}
}

