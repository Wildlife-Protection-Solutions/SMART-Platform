package org.wcs.smart.entity.query;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;

public class EntityFilter implements IFilter {

	public enum EntityFilterType{
		
		ALL("All"),
		ALLACTIVE("Active Only"),
		CUSTOM("Custom");
		
		private String guiName;
		private EntityFilterType(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	};
	
	
	private EntityFilterType type;
	private List<Entity> customEntities;
	
	public EntityFilter(EntityFilterType type){
		this.type = type;
		customEntities = null;
	}
	
	public EntityFilter(EntityFilterType type, List<Entity> entities){
		this(type);
		this.customEntities = entities;
	}
	
	public EntityFilterType getType(){
		return this.type;
	}
	
	public List<Entity> getEntities(){
		return this.customEntities;
	}
	public void setEntities(List<Entity> entities){
		this.customEntities = customEntities;
	}
	
	@Override
	public String asString() {
		return type.getGuiName();
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		return null;
	}

}
