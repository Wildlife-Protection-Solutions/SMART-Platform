package org.wcs.smart.er.query.ui.panels;

import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.query.model.Query;

public interface ISurveyPanel {
	
	public void refreshPanel(SurveyDesign currentDesign);
	
	public Query getQuery();
	
}
