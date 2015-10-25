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
package org.wcs.smart.cybertracker.survey.importer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Container that record information about some data that was valid for a period of time.
 * For example that data a particular sampling unit was recording from dateFrom to dateTo
 * 
 * @author elitvin
 * @since 3.3.0
 * @param <T>
 */
public class TimeDataContainer<T> {
	
	private List<T> dataValues;
	private List<Date> dateCuts;

	public TimeDataContainer(T initial) {
		this(initial, new Date(0L));
	}

	private TimeDataContainer(T initial, Date from) {
		dataValues = new ArrayList<>();
		dateCuts = new ArrayList<>();
		dataValues.add(initial);
		dateCuts.add(from);
	}

	public void add(T data, Date when) {
		if (when == null) {
			throw new IllegalArgumentException("Date cannot be null"); //$NON-NLS-1$
		}
		int index = dateCuts.size()-1;
		Date last = dateCuts.get(index);
		if (last.after(when)) {
			throw new IllegalStateException("New date must be after last in TimeDataContainer"); //$NON-NLS-1$
		}
		dataValues.add(data);
		dateCuts.add(when);
	}
	
	public List<TimeCut<T>> getTimeCuts(Date from, Date to) {
		List<TimeCut<T>> cuts = new ArrayList<>();
		if (from == null || to == null || !to.after(from))
			return cuts;
		
		int endIndex = dateCuts.size()-1;
		while (!to.after(dateCuts.get(endIndex)) && endIndex >= 0) {
			endIndex--;
		} //endIndex will point to a last time point before "to"
		
		Date lastEnd = from;
		T data = null;
		for (int i = 0; i <= endIndex; i++) {
			Date time = dateCuts.get(i);
			if (time.after(from)) {
				cuts.add(new TimeCut<T>(lastEnd, time, data));
				lastEnd = time;
			}
			data = dataValues.get(i);
		}
		cuts.add(new TimeCut<T>(lastEnd, to, data));
		return cuts;
	}

	/**
	 * Specific time cut.
	 * 
	 * @author elitvin
	 * @since 3.3.0
	 * @param <T>
	 */
	public static final class TimeCut<T> {
		private Date start;
		private Date end;
		private T data;
		
		public TimeCut(Date start, Date end, T data) {
			super();
			this.start = start;
			this.end = end;
			this.data = data;
		}

		public Date getStart() {
			return start;
		}
		
		public Date getEnd() {
			return end;
		}
		
		public T getData() {
			return data;
		}
	}
}
