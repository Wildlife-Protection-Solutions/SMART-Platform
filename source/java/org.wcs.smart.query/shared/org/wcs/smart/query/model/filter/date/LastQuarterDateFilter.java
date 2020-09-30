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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.wcs.smart.SmartContext;

/**
 * Last quarter date filter
 * @author Emily
 *
 */
public enum LastQuarterDateFilter implements IDateFilter {

	INSTANCE;

	@Override
	public String getGuiName(Locale l) {
		return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getQueryKey() {
		return "lastquarter"; //$NON-NLS-1$
	}

	@Override
	public LocalDate[] getDates() {
		int thisQuarter = (LocalDate.now().getMonthValue()-1) / 3;
		int thisYear = LocalDate.now().getYear();
		
		int lastQuarter = thisQuarter;
		int lastYear = thisYear;
		lastQuarter = lastQuarter -1;
		if (lastQuarter < 0){
			lastQuarter = 3;
			lastYear = lastYear - 1;
		}
		
		LocalDate from = LocalDate.of(lastYear, lastQuarter*3+1, 1);
		LocalDate to = LocalDate.of(thisYear, thisQuarter*3+1, 1);
		return new LocalDate[] {from, to};
	}

	@Override
	public String getLabel() {
		LocalDate[] bits = getDates();
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
		return( "[" + formatter.format( bits[0] ) + " - " + formatter.format(bits[1]) +" ]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public boolean isEndDateInclusive() {
		return false;
	}
}

