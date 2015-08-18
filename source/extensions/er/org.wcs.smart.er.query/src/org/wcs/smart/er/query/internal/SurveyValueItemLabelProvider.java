package org.wcs.smart.er.query.internal;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.er.query.filter.summary.MissionValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ValueItemLabelProvider;

public class SurveyValueItemLabelProvider extends ValueItemLabelProvider {

	public static SurveyValueItemLabelProvider INSTANCE = new SurveyValueItemLabelProvider();
	
	protected SurveyValueItemLabelProvider() {
		super();
	}
	
	@Override
	public String getName(IValueItem item, Session session){
		if (item instanceof MissionValueItem){
			return getName((MissionValueItem)item, session);
		}
		return super.getName(item, session);
	}
	@Override
	public String getFullName(IValueItem item, Session session){
		if (item instanceof MissionValueItem){
			return getName((MissionValueItem)item, session);
		}
		return super.getFullName(item, session);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(MissionValueItem item, Session session){
		return item.getValueItem().getGuiName(Locale.getDefault());
	}


}
