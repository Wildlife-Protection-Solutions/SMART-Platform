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
package org.wcs.smart.entity.query.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeGroupBy;
import org.wcs.smart.entity.query.ui.definition.EntityDropItemFactory;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

public class EntityAttributeGroupByViewer extends AbstractGroupByViewer<EntityAttributeGroupBy>{

	public EntityAttributeGroupByViewer(EntityAttributeGroupBy gb) {
		super(gb);
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		try {
			EntityAttribute ea = EntityHibernateManager.getInstance().getEntityAttribute(groupBy.getEntityKey(), groupBy.getEntityAttributeKey(), session);
			if (ea == null){
				throw new Exception(MessageFormat.format(Messages.EntityAttributeGroupBy_EntityAttributeNotFound1, new Object[]{groupBy.getEntityAttributeKey(), groupBy.getEntityKey()}));
			}
			//cache names
			ea.getEntityType().getName();
			ea.getName();
			
			Attribute attribute = QueryDataModelManager.getInstance().getAttribute(session, ea.getDmAttribute().getKeyId());
			if (attribute == null) {
				throw new Exception(MessageFormat.format(Messages.EntityAttributeGroupBy_AttributeNotFound,new Object[] { ea.getDmAttribute().getKeyId() }));
			}
			DropItem it = null;
			Attribute.AttributeType attributeType = groupBy.getAttributeType();
			if (attributeType == AttributeType.LIST) {
				it = EntityDropItemFactory.INSTANCE.createEntityAttributeListGroupByDropItem(ea);
			} else {
				it = EntityDropItemFactory.INSTANCE.createEntityAttributeTreeNodeGroupByDropItem(ea,groupBy.getTreeLevel());
			}

			if (attributeType == AttributeType.LIST) {
				String[] filterHkeys = groupBy.getFilterKeys();
				if (filterHkeys != null) {
					ArrayList<ListItem> items = new ArrayList<ListItem>();
					for (int i = 0; i < filterHkeys.length; i++) {
						AttributeListItem ali = QueryDataModelManager.getInstance().getAttributeListItem(session,attribute.getKeyId(), filterHkeys[i]);
						if (ali != null) {
							items.add(new ListItem(null, ali.getName(), ali.getKeyId()));
						}
					}
					it.initializeData(items);
				}

			} else if (attributeType == AttributeType.TREE) {
				String[] filterHkeys = groupBy.getFilterKeys();
				if (filterHkeys != null) {
					HashSet<String> keys = new HashSet<String>();
					for (int i = 0; i < filterHkeys.length; i++) {
						keys.add(filterHkeys[i]);
					}
					ArrayList<ListItem> items = new ArrayList<ListItem>();
					for (String hkey : keys) {
						AttributeTreeNode item = QueryDataModelManager.getInstance().getAttributeTreeNode(session,attribute.getKeyId(), hkey);
						if (item != null) {
							items.add(new ListItem(null, item.getName(), item.getHkey()));
						}
					}

					it.initializeData(items);
				}
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}

	@Override
	public List<ListItem> getItems(Session session) {
		//get children categories
		List<ListItem> items = new ArrayList<ListItem>();
		EntityAttribute ea = null;
		try{
			ea = EntityHibernateManager.getInstance().getEntityAttribute(groupBy.getEntityKey(), groupBy.getEntityAttributeKey(), session);
		}catch (Exception ex){
			EntityQueryPlugIn.displayLog(MessageFormat.format(Messages.EntityAttributeGroupBy_EntityAttributeNotFound, new Object[]{groupBy.getEntityKey(), groupBy.getEntityAttributeKey()}), ex);
			return items;
		}
		if (ea == null){
			EntityQueryPlugIn.displayLog(MessageFormat.format(Messages.EntityAttributeGroupBy_EntityAttributeNotFound, new Object[]{groupBy.getEntityKey(), groupBy.getEntityAttributeKey()}), null);
			return items;
		}
				
		Attribute att = ea.getDmAttribute();
		if (att.getType() == AttributeType.LIST){
			String[] filterHkeys = groupBy.getFilterKeys();
			if (filterHkeys != null) {
				for (AttributeListItem it : att.getAttributeList()) {
					for (int i = 0; i < filterHkeys.length; i++) {
						if (filterHkeys[i].equals(it.getKeyId())) {
							items.add(new ListItem(null, it.getName() + " [" + ea.getEntityType().getName()  + "]", it //$NON-NLS-1$ //$NON-NLS-2$
									.getKeyId()));
							break;
						}
					}
				}
			}else{				
				for (AttributeListItem it : QueryDataModelManager.getInstance().getActiveAttributeListItems(att, session)) {
					items.add(new ListItem(null, it.getName() + " [" + ea.getEntityType().getName()  + "]", it.getKeyId())); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}else if (att.getType() == AttributeType.TREE){
			String[] filterHkeys = groupBy.getFilterKeys();
			if (filterHkeys == null){
				//get all attribute nodes with given hkey length
				for(AttributeTreeNode child : QueryDataModelManager.getInstance().getAttributeTreeNodes(session, att, groupBy.getTreeLevel(), true)){
					items.add(new ListItem(null, child.getName() + " [" + ea.getEntityType().getName()  + "]", child.getHkey())); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}else{
				HashSet<String> keys = new HashSet<String>();
				for (int i = 0; i < filterHkeys.length; i ++){
					keys.add(filterHkeys[i]);
				}
				for(AttributeTreeNode child : QueryDataModelManager.getInstance().getAttributeTreeNodes(session, att, groupBy.getTreeLevel(), false)){
					if (keys.contains(child.getHkey())){
						items.add(new ListItem(null, child.getName() + " [" + ea.getEntityType().getName()  + "]", child.getHkey()));	 //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		return items;
	}

}
