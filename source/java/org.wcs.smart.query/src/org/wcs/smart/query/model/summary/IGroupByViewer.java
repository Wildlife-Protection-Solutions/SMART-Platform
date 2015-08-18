package org.wcs.smart.query.model.summary;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;

public interface IGroupByViewer<T extends IGroupBy> {

	public T getGroupBy();
	
	public DropItem asDropItem(Session session) throws Exception;
	
	/**
	 * @param session
	 * @return a collection of items that make up the group by part
	 */
	public List<ListItem> getItems(Session session);
	
}
