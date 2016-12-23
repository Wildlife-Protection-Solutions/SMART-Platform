package org.wcs.smart.i2.ui.views.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SmartLabelProvider;

public class AreaTypeFilterItem extends DeferredFilterItem {

	private Object LOCK = new Object();
	
	private Area.AreaType type;
	
	public AreaTypeFilterItem(Area.AreaType type) {
		super(SmartLabelProvider.getAreaTypeName(type));
		this.type = type;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List<FilterItem> getChildren() {
		if (kids == null ){
			synchronized (LOCK) {
				if (kids == null){
					System.out.println("loading kids:" + getName());
					Session s = HibernateManager.openSession();
					try{
						ArrayList<FilterItem> temp = new ArrayList<>();
						List<Area> items = HibernateManager.loadAreas(type, s);
						if (items != null){
							for (Area a : items){
								temp.add(new AreaFilterItem(a));
							}
						}
						kids = temp;
					}finally{
						s.close();
					}
				}
			}
			
		}
		if (kids == null) return null;
		return Collections.unmodifiableList(kids);
	}
}
