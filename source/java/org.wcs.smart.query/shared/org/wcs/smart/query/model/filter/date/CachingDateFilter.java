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
import java.util.Locale;

/**
 * A warpper around a DateFilter that caches the
 * dates computed.  In a normal dateFilter the getDates()
 * function re-computes the filter dates each time it is called.
 * This function caches the results the first time getDates() is called
 * and subsequent calls to getDates() return these cached results. 
 * 
 * <p>This is useful in query processing so the date filter always returns
 * the same dates even if used in multiple places in the query processing.</p>
 * @author Emily
 *
 */
public class CachingDateFilter implements IDateFilter {

	private IDateFilter wrapper;
	private Date[] cachedDates;
	
	public CachingDateFilter(IDateFilter wrapper){
		this.wrapper = wrapper;
	}

	@Override
	public String getQueryKey() {
		return wrapper.getQueryKey();
	}

	@Override
	public String getLabel() {
		return wrapper.getLabel();
	}

	@Override
	public Date[] getDates() {
		if (cachedDates == null){
			cachedDates = wrapper.getDates();
		}
		return cachedDates;
	}

	@Override
	public String validate() {
		return wrapper.validate();
	}

	@Override
	public boolean isEndDateInclusive() {
		return wrapper.isEndDateInclusive();
	}

	@Override
	public String getGuiName(Locale l) {
		return wrapper.getGuiName(l);
	}

}
