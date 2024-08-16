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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

public class DateGroupByViewer extends AbstractGroupByViewer<DateGroupBy> {

	public DateGroupByViewer(DateGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		if (groupBy.getOption() instanceof DayDateGroupBy) {
			return getDayItems(session, groupBy.getDateFilter());
		} else if (groupBy.getOption() instanceof MonthDateGroupBy) {
			return getMonthItems(session, groupBy.getDateFilter());
		} else if (groupBy.getOption() instanceof YearDateGroupBy) {
			return getYearItems(session, groupBy.getDateFilter());
		} else if (groupBy.getOption() instanceof StartHourGroupBy ||
				groupBy.getOption() instanceof EndHourGroupBy){
			return getHourItems(session, null);
		}else if (groupBy.getOption() instanceof QuarterDateGroupBy) {
			return getQuarterItems(session, groupBy.getDateFilter());
		}
		return null;
	}

	public String getText() {
		return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).getLabel(
				getGroupBy().getOption(),
				Locale.getDefault());
	}
	
	public Image getImage() {
		if (groupBy.getOption() instanceof DayDateGroupBy) {
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.CALENDAR_DAY_ICON);
		} else if (groupBy.getOption() instanceof MonthDateGroupBy) {
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.CALENDAR_MONTH_ICON);
		} else if (groupBy.getOption() instanceof YearDateGroupBy) {
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.CALENDAR_YEAR_ICON);
		} else if (groupBy.getOption() instanceof StartHourGroupBy){
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.START_HOUR_ICON);
		} else if (groupBy.getOption() instanceof EndHourGroupBy){
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.END_HOUR_ICON);
		} else if (groupBy.getOption() instanceof QuarterDateGroupBy){
			return QueryPlugIn.getDefault().getImageRegistry()
					.get(QueryPlugIn.CALENDAR_QUARTER_ICON);
		}
		return null;
	}

	public List<ListItem> getHourItems(Session session, IDateFilter dateFilter) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (int i = 0 ; i < 48; i ++){
			String key = String.valueOf(i/2);
			if (i % 2 == 0){
				key += ":00"; //$NON-NLS-1$
			}else{
				key += ":30"; //$NON-NLS-1$
			}
			items.add(new ListItem(null, key, key ));
		}
		return items;
		
	}

	public List<ListItem> getDayItems(Session session, IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				LocalDateTime ld = getMinDate(session);
				if (ld != null) startdate = ld.toLocalDate();
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}

		LocalDate working = LocalDate.from(startdate);
		while (working.isBefore(enddate)
				|| (dateFilter.isEndDateInclusive() && working.isEqual(enddate))) {
			
			String key = DateTimeFormatter.ISO_LOCAL_DATE.format(working);
			items.add(new ListItem(null, key, key));

			working = ChronoUnit.DAYS.addTo(working, 1);
		}
		return items;
	}

	public List<ListItem> getMonthItems(Session session, IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				LocalDateTime ld = getMinDate(session);
				if (ld != null) startdate = ld.toLocalDate();
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}

		// each month between start and end of
		// form "m/yyyy"
		DateTimeFormatter nameFormat = DateTimeFormatter.ofPattern("MM/yyyy"); //$NON-NLS-1$
		DateTimeFormatter keyFormat = DateTimeFormatter.ofPattern("M/yyyy", Locale.ENGLISH); //$NON-NLS-1$

		LocalDate working = LocalDate.of(startdate.getYear(), startdate.getMonth(), 1);

		while (working.isBefore(enddate) || (dateFilter.isEndDateInclusive() && working.isEqual(enddate))) {
			String key = keyFormat.format(working);
			String name = nameFormat.format(working);
			items.add(new ListItem(null, name, key));
			working = ChronoUnit.MONTHS.addTo(working, 1);
		}
		
		return items;
	}

	public List<ListItem> getQuarterItems(Session session, IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				LocalDateTime ld = getMinDate(session);
				if (ld != null) startdate = ld.toLocalDate();
				
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}

		// each month between start and end of
		// form "m/yyyy"
		DateTimeFormatter nameFormat = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$
		DateTimeFormatter keyFormat = DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH); //$NON-NLS-1$

		LocalDate working = LocalDate.of(startdate.getYear(), startdate.getMonth(), 1);

		while (working.isBefore(enddate) || (dateFilter.isEndDateInclusive() && working.isEqual(enddate))) {
			int quarter =  ((working.getMonthValue()-1) / 3) + 1;
			String key = keyFormat.format(working) + "_" + quarter ; //$NON-NLS-1$
			String name = MessageFormat.format(Messages.DateGroupByViewer_QuarterYearFormatting, quarter, nameFormat.format(working));
			items.add(new ListItem(null, name, key));
			working = ChronoUnit.MONTHS.addTo(working, 3);
		}
		
		return items;
	}
	
	public List<ListItem> getYearItems(Session session, IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		LocalDate startdate = LocalDate.now();
		LocalDate enddate = LocalDate.now();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				LocalDateTime ld = getMinDate(session);
				if (ld != null) startdate = ld.toLocalDate();
			} else {
				LocalDate[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}

		// each year in form yyyy
		int year = startdate.getYear();
		int year2 = enddate.getYear();
		
		while (year <= year2) {
			items.add(new ListItem(null, String.valueOf(year), String.valueOf(year)));
			year++;
		}
		return items;
	}

	private LocalDateTime getMinDate(Session session) {
		String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea  IN(:ca)"; //$NON-NLS-1$
		LocalDateTime ld = session.createQuery(hql, LocalDateTime.class)
			.setParameterList("ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()) //$NON-NLS-1$
			.uniqueResult();
		return ld;
	}
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		return BasicDropItemFactory.INSTANCE.createDateGroupByDropItem(this);
	}

}