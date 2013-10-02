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
package org.wcs.smart.query.parser.internal.summary;


import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Date Group By option.
 * @author egouge
 * @since 1.0.0
 */
public class DateGroupBy implements IGroupBy {

	public static final DateGroupBy createGroupBy(String key){
		return new DateGroupBy(key);
	}
	
	private DateGroupByOption op;
	
	/**
	 * Creates a new date group by part.
	 * @param key the date group by key
	 */
	public DateGroupBy(String key){
		String opPart = key.split(":")[1];		 //$NON-NLS-1$
		this.op = PatrolQueryOptions.findDateGroupByOption(opPart);
		if (this.op == null){
			throw new IllegalStateException(MessageFormat.format(
					Messages.DateGroupBy_GroupByNoSupported, new Object[]{opPart}));
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		return "date:" + op.getKey(); //$NON-NLS-1$
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		return getKeyPart();
	}


	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		if (op == DateGroupByOption.DAY){
			return GroupByType.DATE;
		}else{
			return GroupByType.STRING;
		}
	}
	
	/**
	 * @return the patrol group by option
	 */
	public DateGroupByOption getOption(){
		return this.op;
	}

	
	private DateFilter df;
	/**
	 * Sets the date filter associated with the query using this
	 * option.  This is used to know how many block
	 * to break the query into.
	 * 
	 * @see getItems(org.hibernate.Session)
	 * 
	 * @param df
	 */
	public void setDateFilter(DateFilter df){
		this.df = df;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (df == null){
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		}else{
			if (df.getDateFilterOption() == PatrolQueryOptions.DATE_FILTER_OP.ALL){
				String hql = "SELECT min(startDate) from Patrol WHERE conservationArea  IN(:ca)"; //$NON-NLS-1$
				Query q = session.createQuery(hql);
				q.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
				
				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null){
					startdate = (java.sql.Timestamp)data.get(0);
				}
			}else{
				Date[] d = df.getDates();
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
		
		if (op == DateGroupByOption.DAY){
			//each date between start and end
			while(cals.before(cale)){
				java.sql.Date dd = new java.sql.Date(cals.getTime().getTime());
				String key = dd.toString();
				items.add(new ListItem(null, key,key));
				cals.add(Calendar.DAY_OF_MONTH, 1);
			}
		}else if (op == DateGroupByOption.MONTH){
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
			
		}else if (op == DateGroupByOption.YEAR){
			//each year in form yyyy
			int year = cals.get(Calendar.YEAR);
			int year2 = cale.get(Calendar.YEAR);
			GregorianCalendar gcal = new GregorianCalendar();
			while(year <= year2){
				Calendar c = Calendar.getInstance();
				c.set(year, 0, 1);
				gcal.setTime(c.getTime());
				
				items.add(new ListItem(null, String.valueOf(year), String.valueOf(gcal.get(Calendar.YEAR))));
				year ++;
			}
			
		}
		
		return items;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) {
		return DropItemFactory.INSTANCE.createDateGroupByDropItem(op);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#hasCategory()
	 */
	public boolean hasCategory(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#hasAttribute()
	 */
	public boolean hasAttribute(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#validateAndImport(org.hibernate.Session)
	 */
	public List<String> validateAndImport(String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception{
		return null;
	}
}
