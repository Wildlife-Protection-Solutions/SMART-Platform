package org.wcs.smart.er.query.ui.panels.item;

import org.wcs.smart.er.query.model.MissionTrackQueryType;
import org.wcs.smart.query.QueryTypeManager;

public class MissionTrackFilterItemPanel extends FilterItemPanel{
	
	public static final String ID = "org.wcs.smart.er.query.survey.filterItemPanel.misisonTrack"; //$NON-NLS-1$
	
	public MissionTrackFilterItemPanel(){
		super(QueryTypeManager.getInstance().findQueryType(MissionTrackQueryType.KEY));
	}
	
	@Override
	public String getId() {
		return ID;
	}
}
