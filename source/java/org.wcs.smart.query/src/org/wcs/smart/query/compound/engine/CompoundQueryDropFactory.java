package org.wcs.smart.query.compound.engine;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.compound.ui.CompoundDefinitionPanel;
import org.wcs.smart.query.compound.ui.QueryDropItem;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDropItemFactory;

public class CompoundQueryDropFactory implements IDropItemFactory{

	public static CompoundQueryDropFactory INSTANCE = new CompoundQueryDropFactory();
	
	private CompoundQueryDropFactory(){	
	}
	
	@Override
	public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
		if (source instanceof Query){
			Query q = (Query)source;
			return new DropItem[]{ new QueryDropItem(q) };
		}
		if (source instanceof QueryEditorInput){
			QueryEditorInput in = (QueryEditorInput)source;
			return new DropItem[]{ new QueryDropItem(in.getUuid(), in.getType(), in.getName())};
		}
		return null;
	}

	@Override
	public void generateDropItems(QueryProxy q, Session session) {
		List<DropItem> items = new ArrayList<DropItem>();
		CompoundMapQuery query = (CompoundMapQuery)q.getQuery();
		for (CompoundMapQueryLayer layer : query.getLayers()){
			IQueryType type = QueryTypeManager.INSTANCE.findQueryType(layer.getQueryType());
					
			Query childquery = QueryHibernateManager.getInstance().findQuery(session, layer.getQueryUuid(), type);
			items.add(generateDropItem(childquery, null)[0]);
		}
		q.setDropItems(CompoundDefinitionPanel.ID, items);
		
	}

}
