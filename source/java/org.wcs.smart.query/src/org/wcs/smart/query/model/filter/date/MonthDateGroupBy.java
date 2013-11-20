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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.summary.IGroupBy.GroupByType;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Group by month option
 * @author Emily
 *
 */
public class MonthDateGroupBy implements IDateGroupBy {
	
	public static final MonthDateGroupBy INSTANCE = new MonthDateGroupBy();
	
	@Override
	public String getKey() {
		return "month";
	}

	@Override
	public String getGuiName() {
		return "Month";
	}

	@Override
	public GroupByType getType() {
		return GroupByType.STRING;
	}

	@Override
	public List<ListItem> getItems(Session session, IDateFilter dateFilter) {
		
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null){
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		}else{
			if (dateFilter.getDates() == null){
				//all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea  IN(:ca)"; //$NON-NLS-1$
				Query q = session.createQuery(hql);
				q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
				
				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null){
					startdate = (java.sql.Timestamp)data.get(0);
				}
			}else{
				Date[] d = dateFilter.getDates();
				if (d.length >= 1){
					startdate = d[0];
				}
				if (d.length >=2){
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);
		
		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);
		
		//each month between start and end of 
		//form "m/yyyy"
		SimpleDateFormat nameFormat = new SimpleDateFormat("MM/yyyy"); //$NON-NLS-1$
		SimpleDateFormat keyFormat = new SimpleDateFormat("M/yyyy", Locale.ENGLISH); //$NON-NLS-1$
		
		cals.set(Calendar.DAY_OF_MONTH, 1);
		cale.set(Calendar.DAY_OF_MONTH, cale.getActualMaximum(Calendar.DAY_OF_MONTH));
		while(cals.before(cale)){
			String key = keyFormat.format(cals.getTime());
			String name = nameFormat.format(cals.getTime());
			items.add(new ListItem(null, name, key));
			cals.add(Calendar.MONTH, 1);
		}
		return items;
	}

	@Override
	public Image getImage() {
		return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.CALENDAR_MONTH_ICON);
	}

}
