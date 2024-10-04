package org.wcs.smart.observation.query.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.query.model.IncidentTypeProviderManager;
import org.wcs.smart.observation.query.model.QueryIncidentType;
import org.wcs.smart.observation.query.model.filter.IncidentTypeGroupBy;
import org.wcs.smart.observation.query.ui.definition.IncidentTypeGroupByDropItem;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

public class IncidentTypeGroupByViewer extends AbstractGroupByViewer<IncidentTypeGroupBy> {
	
	
	public IncidentTypeGroupByViewer(IncidentTypeGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		String[] keys = groupBy.getKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		
		Collection<QueryIncidentType> types = IncidentTypeProviderManager.INSTANCE.getTypes(session, SmartDB.getConservationAreaConfiguration().getConservationAreas());
		
		if (keys == null){
			for (QueryIncidentType type : types) {
				items.add(new ListItem(null, type.getName(), type.getKey()));
			}
		}else{
			Set<String> keyset = new HashSet<>();
			for (String key : keys) keyset.add(key);
			for (QueryIncidentType type : types) {
				if (!keyset.contains(type.getKey())) continue;
				items.add(new ListItem(null, type.getName(), type.getKey()));
			}			
		}
		Collections.sort(items);
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		IncidentTypeGroupByDropItem dropItem = new IncidentTypeGroupByDropItem();
		dropItem.initializeData(getItems(session));
		return dropItem;
	}

}
