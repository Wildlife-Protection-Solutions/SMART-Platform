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
package org.wcs.smart.patrol.query.model;

import org.wcs.smart.query.model.filter.date.EndHourGroupBy;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.query.model.filter.date.StartHourGroupBy;
import org.wcs.smart.query.model.summary.DateGroupBy;

/**
 * Patrol group by date options.  This adds patrol start time
 * and end time to default date group by options.
 * 
 * @author egouge
 * @since 1.0.0
 * 
 */
public class PatrolDateGroupBy extends DateGroupBy {

	private static IDateGroupBy[] GROUPBYS = {
		StartHourGroupBy.INSTANCE,
		EndHourGroupBy.INSTANCE
	};
	
	public static DateGroupBy createGroupBy(String key){
		return new PatrolDateGroupBy(key);
	}
	
	public PatrolDateGroupBy(String key) {
		super(key);
	}
	
	@Override
	protected IDateGroupBy[] getSupportedGroupBys(){
		IDateGroupBy[] all = new IDateGroupBy[GROUPBYS.length + DateGroupBy.GROUPBYS.length];
		int i = 0;
		for (IDateGroupBy b : DateGroupBy.GROUPBYS){
			all[i++] = b;
		}
		for (IDateGroupBy b : GROUPBYS){
			all[i++] = b;
		}
		return all;
	}

}
