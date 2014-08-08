package org.wcs.smart.er.query.model;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.wcs.smart.er.model.SurveyDesign;

public interface ISurveyQuery {

	public void setSurveyDesign(String key);
	
	public String getSurveyDesign();
	
	public void setSurveyDesign(SurveyDesign design);
	
	public SurveyDesign getSurveyDesignAsObject(); 
}
