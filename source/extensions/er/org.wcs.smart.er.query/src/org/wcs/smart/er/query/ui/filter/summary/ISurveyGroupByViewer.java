package org.wcs.smart.er.query.ui.filter.summary;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.query.ui.model.ListItem;

public interface ISurveyGroupByViewer{

	public List<ListItem> getItems(Session session, SurveyDesignFilter filter);
	
}
