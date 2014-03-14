package org.wcs.smart.entity.query.ui.definition;

import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.query.ui.model.impl.AttributeDropItem;

public class EntityAttributeDropItem extends AttributeDropItem {

	private EntityAttribute ea;
	
	public EntityAttributeDropItem(EntityAttribute ea){
		super(ea.getDmAttribute());
		this.ea = ea;
		
		
		this.key = "entity:" + ea.getEntityType().getKeyId() + ":attribute:" + ea.getDmAttribute().getType().typeKey + ":" + ea.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
		this.text = ea.getEntityType().getName() + " - " + ea.getName();
	}
	
	
	
	

}
