package org.wcs.smart.er.query.filter;

import org.hibernate.Session;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;

public class SurveyDesignFilter implements IFilter {

	
	/**
	 * Creates a survey design filter.
	 * 
	 * @return
	 */
	public static SurveyDesignFilter createStringFilter(String key){
		return new SurveyDesignFilter(key);
	}

	private String surveyDesignKey;

	
	/**
	 * Creates a new survey design filter
	 * 
	 * @param key survey design key
	 */
	public SurveyDesignFilter (String key){
		this.surveyDesignKey = key;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return "surveydesign: " + surveyDesignKey;  //$NON-NLS-1$ 
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);		
	}
	
	/**
	 * Drop item not supported for this filter
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		return null;
	}
}
