package org.wcs.smart.observation.query.ui;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.model.filter.WaypointSourceGroupBy;
import org.wcs.smart.observation.query.ui.definition.WaypointSourceGroupByDropItem;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;

public class WaypointSourceGroupByViewer extends AbstractGroupByViewer<WaypointSourceGroupBy> {
	
	
	public WaypointSourceGroupByViewer(WaypointSourceGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		String[] keys = groupBy.getKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		if (keys == null){
			for(IWaypointSource src : WaypointSourceEngine.INSTANCE.getSupportedSources()){
				items.add(new ListItem(null,src.getName(), src.getKey()));
			}
		}else{
			for (String k : keys){
				IWaypointSource c = WaypointSourceEngine.INSTANCE.getSource(k);
				if (c != null){
					items.add(new ListItem(null, c.getName(), c.getKey()));
				}
			}
		}
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		WaypointSourceGroupByDropItem dropItem = new WaypointSourceGroupByDropItem();
		dropItem.initializeData(getItems(null));
		return dropItem;
	}

}
