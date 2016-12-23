package org.wcs.smart.i2.ui.views.query;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;
import org.wcs.smart.util.UuidUtils;

public class EntityFilterItem extends BasicFilterItem {

	private UUID entity;
	private String typeKey;
	private BasicFilterItem parent;
	
	private String dropLabel = null;
	
	public EntityFilterItem(IntelEntityType type){
		super(MessageFormat.format("Any {0}", type.getName()));
		dropLabel = getName();
		entity = null;
		typeKey = type.getKeyId();
	}
	
	public EntityFilterItem(IntelEntity entity){
		super(entity.getIdAttributeAsText());
		this.entity = entity.getUuid();
		typeKey = entity.getEntityType().getKeyId();
		dropLabel = MessageFormat.format("{0} [{1}]", entity.getIdAttributeAsText(), entity.getEntityType().getName() );
	}
	
	@Override
	public List<FilterItem> getChildren() {
		return null;
	}

	@Override
	public DropItem[] asDropItem() {
		String queryKey = null;
		if (entity == null){
			queryKey = "entitytype:" + typeKey;  
		}else{
			queryKey = "entity:" + UuidUtils.uuidToString(entity);
		}
		return new DropItem[]{new TextDropItem(dropLabel, queryKey)};
	}

	@Override
	public FilterItem getParent() {
		return parent;
	}

}
