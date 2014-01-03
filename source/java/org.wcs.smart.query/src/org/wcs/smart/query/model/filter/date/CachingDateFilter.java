package org.wcs.smart.query.model.filter.date;

import java.sql.Date;

public class CachingDateFilter implements IDateFilter {

	private IDateFilter wrapper;
	private Date[] cachedDates;
	
	public CachingDateFilter(IDateFilter wrapper){
		this.wrapper = wrapper;
	}
	
	@Override
	public String getGuiName() {
		return wrapper.getGuiName();
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

}
