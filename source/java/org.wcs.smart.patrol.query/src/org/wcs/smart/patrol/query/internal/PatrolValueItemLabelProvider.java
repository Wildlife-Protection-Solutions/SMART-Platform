package org.wcs.smart.patrol.query.internal;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ValueItemLabelProvider;

public class PatrolValueItemLabelProvider extends ValueItemLabelProvider {

	public static PatrolValueItemLabelProvider INSTANCE = new PatrolValueItemLabelProvider();
	
	protected PatrolValueItemLabelProvider() {
		super();
	}
	
	@Override
	public String getName(IValueItem item, Session session){
		if (item instanceof PatrolValueItem){
			
			return getName((PatrolValueItem)item, session);
		}
		return super.getName(item, session);
	}
	@Override
	public String getFullName(IValueItem item, Session session){
		if (item instanceof PatrolValueItem){
			return getName((PatrolValueItem)item, session);
		}
		return super.getFullName(item, session);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(PatrolValueItem item, Session session){
		return item.getPatrolValueOption().getGuiName(Locale.getDefault());
	}


}
