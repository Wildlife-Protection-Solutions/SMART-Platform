package org.wcs.smart.er.query.filter.summary;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.model.ListItem;

public interface ISurveyGroupBy extends IGroupBy{

	public List<ListItem> getItems(Session session, SurveyDesignFilter filter);
	
}
