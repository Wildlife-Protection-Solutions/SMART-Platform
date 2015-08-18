package org.wcs.smart.query.model.summary;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;

public abstract class AbstractGroupByViewer<T extends IGroupBy> implements IGroupByViewer<T> {

	protected T groupBy;
	
	public AbstractGroupByViewer(T gb){
		this.groupBy = gb;
	}
	
	@Override
	public T getGroupBy() {
		return groupBy;
	}

	@Override
	public abstract DropItem asDropItem(Session session) throws Exception;

	@Override
	public abstract List<ListItem> getItems(Session session);

}
