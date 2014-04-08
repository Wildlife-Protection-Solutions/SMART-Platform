/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.entity.query.parser.internal;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.ui.definition.EntityDropItemFactory;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.AttributeDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Filter for entity type filters; extends the existing attribute file.
 * @author Emily
 *
 */
public class EntityTypeFilter extends AttributeFilter {
	
	/*
	 * Creates a new entity type filter
	 */
	public static EntityTypeFilter createListItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		
		String[] bits = attributeIdentifier.split(":"); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		for (int i = 2; i < bits.length; i ++){
			sb.append(bits[i]);
			if (i != bits.length-1){
				sb.append(":"); //$NON-NLS-1$
			}
		}
		return new EntityTypeFilter(bits[1], sb.toString(),  op, attributeItemKey);
	}
	
	
	private String entityKey;
	private EntityType entityType;
	
	/**
	 * Creates a new attribute filter with a given key and type.
	 * 
	 * @param attributeIdentifier
	 * @param type
	 */
	private EntityTypeFilter(String entityKey, String attributeIdentifier, Operator op, String itemKey){
		super(attributeIdentifier, op, itemKey);
		this.entityKey = entityKey;
	}
	
	/**
	 * 
	 * @return the entity type key
	 */
	public String getEntityTypeKey(){
		return this.entityKey;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return "entitytype:" + entityKey + ":" + super.asString(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * 
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * 
	 * @return {@link AttributeDropItem} or {@link AttributeListDropItem} 
	 * or {@link AttributeTreeDropItem} depending on 
	 * attribute type.
	 */
	public DropItem[] getDropItems(Session session) throws Exception{
		try{
			EntityType ea = getEntityType(session);
			if (ea == null){
				throw new Exception(MessageFormat.format(Messages.EntityTypeFilter_EntityTypeNotFound, new Object[]{entityKey}));
			}
			DropItem it = EntityDropItemFactory.INSTANCE.createEntityTypeDropItem(ea);
			
			initDropItem(it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	
	/**
	 * Loads the attribute from the database
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public EntityType getEntityType(Session session) throws Exception{
		if (entityType == null){
			entityType = EntityHibernateManager.getEntityType(entityKey, session);
		}		
		return entityType;
	}
}
