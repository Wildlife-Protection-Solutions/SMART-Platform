package org.wcs.smart.patrol.query.ui;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.ui.model.ListItem;

public interface IPatrolOptionData {

	public void setPatrolOption(IPatrolQueryOption option);
	
	public List<ListItem> getValues(Session session, String[] keys);
	
	public List<ListItem> getAllActiveValues(Session session);
	
	public ListItem getDefaultListItem();
	
	public Image getImage();
}
