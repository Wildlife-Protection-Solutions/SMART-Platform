package org.wcs.smart.query.model.filter.date;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;

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
		}
		return null;
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
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea  IN(:ca)"; //$NON-NLS-1$
				Query q = session.createQuery(hql);
				q.setParameterList(
						"ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$

				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = (java.sql.Timestamp) data.get(0);
				}
			} else {
				Date[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);

		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);

		while (cals.before(cale)
				|| (dateFilter.isEndDateInclusive() && cals.equals(cale))) {
			java.sql.Date dd = new java.sql.Date(cals.getTime().getTime());
			String key = dd.toString();
			items.add(new ListItem(null, key, key));
			cals.add(Calendar.DAY_OF_MONTH, 1);
		}
		return items;
	}

	public List<ListItem> getMonthItems(Session session, IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea  IN(:ca)"; //$NON-NLS-1$
				Query q = session.createQuery(hql);
				q.setParameterList(
						"ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$

				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = (java.sql.Timestamp) data.get(0);
				}
			} else {
				Date[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);

		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);

		// each month between start and end of
		// form "m/yyyy"
		SimpleDateFormat nameFormat = new SimpleDateFormat("MM/yyyy"); //$NON-NLS-1$
		SimpleDateFormat keyFormat = new SimpleDateFormat(
				"M/yyyy", Locale.ENGLISH); //$NON-NLS-1$

		cals.set(Calendar.DAY_OF_MONTH, 1);
		cale.set(Calendar.DAY_OF_MONTH,
				cale.getActualMaximum(Calendar.DAY_OF_MONTH));
		while (cals.before(cale)) {
			String key = keyFormat.format(cals.getTime());
			String name = nameFormat.format(cals.getTime());
			items.add(new ListItem(null, name, key));
			cals.add(Calendar.MONTH, 1);
		}
		return items;
	}

	public List<ListItem> getYearItems(Session session, IDateFilter dateFilter) {

		ArrayList<ListItem> items = new ArrayList<ListItem>();
		Date startdate = new Date();
		Date enddate = new Date();
		if (dateFilter == null) {
			throw new IllegalStateException(Messages.DateGroupBy_InvalidFilter);
		} else {
			if (dateFilter.getDates() == null) {
				// all daytes
				String hql = "SELECT min(dateTime) from Waypoint WHERE conservationArea  IN(:ca)"; //$NON-NLS-1$
				Query q = session.createQuery(hql);
				q.setParameterList(
						"ca", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$

				List<?> data = q.list();
				if (data != null && data.size() >= 1 && data.get(0) != null) {
					startdate = (java.sql.Timestamp) data.get(0);
				}
			} else {
				Date[] d = dateFilter.getDates();
				if (d.length >= 1) {
					startdate = d[0];
				}
				if (d.length >= 2) {
					enddate = d[1];
				}
			}
		}
		Calendar cals = Calendar.getInstance();
		cals.setTime(startdate);

		Calendar cale = Calendar.getInstance();
		cale.setTime(enddate);

		// each year in form yyyy
		int year = cals.get(Calendar.YEAR);
		int year2 = cale.get(Calendar.YEAR);
		GregorianCalendar gcal = new GregorianCalendar();
		while (year <= year2) {
			Calendar c = Calendar.getInstance();
			c.set(year, 0, 1);
			gcal.setTime(c.getTime());

			items.add(new ListItem(null, String.valueOf(year), String
					.valueOf(gcal.get(Calendar.YEAR))));
			year++;
		}
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		return BasicDropItemFactory.INSTANCE.createDateGroupByDropItem(this);
	}

}