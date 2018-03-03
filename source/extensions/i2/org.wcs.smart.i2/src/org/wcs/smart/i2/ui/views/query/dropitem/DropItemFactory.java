/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views.query.dropitem;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.IntelHibernateManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.AreaFilter;
import org.wcs.smart.i2.query.observation.filter.BooleanFilter;
import org.wcs.smart.i2.query.observation.filter.BracketFilter;
import org.wcs.smart.i2.query.observation.filter.DataModelFilter;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByPart;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;
import org.wcs.smart.i2.query.observation.filter.ValuePart;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Generates drop items from filters
 * 
 * @author Emily
 *
 */
public class DropItemFactory {

	public static final String ANY_LABEL = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IntelQueryColumnProvider.ANY_ITEM, Locale.getDefault());
	
	public static List<DropItem> generateDropItems(IQueryFilter filter, Session session){
		if (filter == null) return Collections.emptyList();
		return (new DropItemFactory(session)).generateDropItems(filter);
	}

	public static List<DropItem> generateDropItems(GroupByPart part, Session session){
		if (part == null || part.getItems().isEmpty()) return Collections.emptyList();
		return (new DropItemFactory(session)).generateDropItems(part);
	}
	

	public static List<DropItem> generateDropItems(ValuePart part, Session session){
		if (part == null ) return Collections.emptyList();
		return (new DropItemFactory(session)).generateDropItems(part);
	}
	private Session session;
	
	private DropItemFactory(Session session){
		this.session = session;
	}
	
	public List<DropItem> generateDropItems(ValuePart part){
		return Collections.singletonList(new ValueDropItem(part.getValueOption()));
	}
	
	public List<DropItem> generateDropItems(GroupByPart part){
		List<DropItem> allItems = new ArrayList<>();
		for (GroupByItem i : part.getItems()) {
			
			if (i.getGroupByType() == GroupByItem.GroupByType.ENTITYTYPE) {
				EntityTypeGroupByDropItem di = new EntityTypeGroupByDropItem();
				for (String entityTypeKey : i.getFilterOptions()) {
					IntelEntityType type = QueryFactory.buildQuery(session, IntelEntityType.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
							new Object[] {"keyId", entityTypeKey}).uniqueResult();
					if (type == null) {
						DropItem edi = new ErrorDropItem(MessageFormat.format("No entity type with key {0} found.", entityTypeKey));
						return Collections.singletonList(edi);
					}	
					di.addEntityType(type);
				}
				return Collections.singletonList(di);
			}else if (i.getGroupByType() == GroupByItem.GroupByType.ATTRIBUTE) {
				String attributeKey = i.getAttributeKey();
				
				IntelAttribute attribute = QueryFactory.buildQuery(session, IntelAttribute.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"keyId", attributeKey}).uniqueResult();
				if (attribute == null) {
					DropItem edi = new ErrorDropItem(MessageFormat.format("No attribute with key {0} found.", attributeKey));
					return Collections.singletonList(edi);
				}
				
				String entityTypeKey = i.getEntityTypeKey();
				IntelEntityType type = null;
				if (entityTypeKey != null) {
					type = QueryFactory.buildQuery(session, IntelEntityType.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
							new Object[] {"keyId", entityTypeKey}).uniqueResult();
					if (type == null) {
						DropItem edi = new ErrorDropItem(MessageFormat.format("No entity type with key {0} found.", entityTypeKey));
						return Collections.singletonList(edi);
					}	
				}
				
				AttributeGroupByDropItem di = null;
				if (type == null) {
					di = new AttributeGroupByDropItem(attribute);
				}else {
					IntelEntityTypeAttribute temp = new IntelEntityTypeAttribute();
					temp.setAttribute(attribute);
					temp.setEntityType(type);
					di = new AttributeGroupByDropItem(temp);
				}
				
				if (attribute.getType() == AttributeType.DATE) {
					di.setDateOption(i.getDateOption());
				}
				if (attribute.getType() == AttributeType.POSITION) {
					di.setAreaOption(i.getAreaType());
					
					List<String> keys = i.getFilterOptions();
					for (String areaKey : keys) {
						Area a = QueryFactory.buildQuery(session, Area.class, 
								new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
								new Object[] {"type", i.getAreaType().name()},
								new Object[] {"keyId", areaKey}).uniqueResult();
						if (a == null) {
							DropItem edi = new ErrorDropItem(MessageFormat.format("No area of type {0} with key {1} found.", i.getAreaType().name(), areaKey));
							return Collections.singletonList(edi);
						}
						di.addFilterOption(new ListItem(a.getKeyId(), a.getName()));
					}
					
				}
				if (attribute.getType() == AttributeType.LIST) {
					List<String> keys = i.getFilterOptions();
					for (String areaKey : keys) {
						for (IntelAttributeListItem listItem : attribute.getAttributeList()) {
							if (listItem.getKeyId().equals(areaKey)) {
								di.addFilterOption(new ListItem(listItem.getKeyId(), listItem.getName()));
								break;
							}
						}
					}
				}
				if (attribute.getType() == AttributeType.EMPLOYEE) {
					List<String> keys = i.getFilterOptions();
					for (String employeUuid : keys) {
						UUID eu = UuidUtils.stringToUuid(employeUuid);
						Employee e = session.get(Employee.class,  eu);
						if (e != null) {
							di.addFilterOption(new ListItem(UuidUtils.uuidToString(e.getUuid()), SmartLabelProvider.getFullLabel(e)));
						}
					}
				}
				allItems.add(di);
			}
		}
		return allItems;
	}
	
	public List<DropItem> generateDropItems(IQueryFilter filter){
		if (filter.getClass().equals(DataModelFilter.class))
			return generateDropItem((DataModelFilter) filter);
		
		if (filter.getClass().equals(NotFilter.class))
			return generateDropItem((NotFilter) filter);
		
		if (filter.getClass().equals(AreaFilter.class))
			return generateDropItem((AreaFilter) filter);
		
		if (filter.getClass().equals(EntityFilter.class))
			return generateDropItem((EntityFilter) filter);
		
		if (filter.getClass().equals(BooleanFilter.class))
			return generateDropItem((BooleanFilter) filter);
		
		if (filter.getClass().equals(BracketFilter.class))
			return generateDropItem((BracketFilter) filter);
		
		if (filter.getClass().equals(EntityTypeFilter.class))
			return generateDropItem((EntityTypeFilter) filter);
		
		if (filter.getClass().equals(IntelAttributeFilter.class))
			return generateDropItem((IntelAttributeFilter) filter);
		
		if (filter.getClass().equals(NotFilter.class))
			return generateDropItem((NotFilter) filter);
		
		ErrorDropItem error = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_FilterTypeNotSupported, filter.getClass().getName()));
		return Collections.singletonList(error);
	}
	
	
	public List<DropItem> generateDropItem(AreaFilter filter){
		
		String queryKey = "area:" + filter.getType() + ":" + filter.getKey(); //$NON-NLS-1$ //$NON-NLS-2$
		
		Area a = QueryFactory.buildQuery(session, Area.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"type", filter.getType()}, //$NON-NLS-1$
				new Object[] {"keyId", filter.getKey()}).uniqueResult(); //$NON-NLS-1$

		if (a == null){
			ErrorDropItem item = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_InvalidAreaType, filter.getType(), filter.getKey()));
			return Collections.singletonList(item);	
		}
		return Collections.singletonList(new TextDropItem(IntelQueryColumnProvider.generateName(a), queryKey));
		
	}
	
	public List<DropItem> generateDropItem(EntityFilter filter){
		IntelEntity entity = (IntelEntity) session.get(IntelEntity.class, filter.getEntityUuid());
		if (entity == null){
			ErrorDropItem item = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_InvalidEntity, filter.getEntityUuid().toString()));
			return Collections.singletonList(item);	
			
		}
		return Collections.singletonList(new TextDropItem(IntelQueryColumnProvider.generateName(entity, Locale.getDefault()), "entity:"+UuidUtils.uuidToString(entity.getUuid()))); //$NON-NLS-1$
	}
	
	public List<DropItem> generateDropItem(EntityTypeFilter filter){
		IntelEntityType type = null; 
		if (filter.getTypeKey() != null){
			type = IntelHibernateManager.getEntityType(filter.getTypeKey(),SmartDB.getCurrentConservationArea(), session);
			if (type != null){
				return Collections.singletonList(new TextDropItem(type.getName(), "entitytype:" + type.getKeyId())); //$NON-NLS-1$
			}
		}
		ErrorDropItem item = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_InvalidEntityType, filter.getTypeKey()));
		return Collections.singletonList(item);
	}
	

	
	public List<DropItem> generateDropItem(NotFilter filter){
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.add(new TextOperatorDropItem(Operator.NOT));
		items.addAll(generateDropItems(filter.getFilter()));
		return items;
	}
	
	public List<DropItem> generateDropItem(BooleanFilter filter){
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.addAll(generateDropItems(filter.getFilter1()));
		OptionDropItem booleanOp = OptionDropItem.createAndOrDropItem();
		booleanOp.setInitialValue(filter.getOperator().getKey());
		items.add(booleanOp);
		items.addAll(generateDropItems(filter.getFilter2()));
		return items;
	}
	
	public List<DropItem> generateDropItem(BracketFilter filter){
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.add(new TextOperatorDropItem(Operator.BRACKET_OPEN));
		items.addAll(generateDropItems(filter.getFilter()));
		items.add(new TextOperatorDropItem(Operator.BRACKET_CLOSE));
		return items;
	}
	
	
	public List<DropItem> generateDropItem(IntelAttributeFilter filter){
		String queryKeyPart = "e_attribute:" + filter.getAttributeType().key + ":" + filter.getAttributeKey() + ":"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (filter.getEntityTypeKey() != null){
			queryKeyPart += filter.getEntityTypeKey();
		}
		
		IntelAttribute ia = IntelHibernateManager.getAttribute(filter.getAttributeKey(), SmartDB.getCurrentConservationArea(), session);
		if (ia == null){
			ErrorDropItem item = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_InvalidAttributeKey, filter.getAttributeKey()));
			return Collections.singletonList(item);
		}
		
		IntelEntityType type = null; 
		if (filter.getEntityTypeKey() != null){
			type = IntelHibernateManager.getEntityType(filter.getEntityTypeKey(), SmartDB.getCurrentConservationArea(), session);
			if (type == null){
				ErrorDropItem item = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_InvalidEntityTypeKey, filter.getEntityTypeKey()));
				return Collections.singletonList(item);	
			}
		}
		String name = IntelQueryColumnProvider.generateName(ia, type);
		
		if (filter.getAttributeType() == IntelAttribute.AttributeType.NUMERIC){
			TextBoxDropItem item = new TextBoxDropItem(name, queryKeyPart, TextBoxDropItem.InputType.NUMERIC);
			item.setInitialValue(filter.getOperator(), filter.getNumberValue().toString());
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == IntelAttribute.AttributeType.TEXT){
			TextBoxDropItem item = new TextBoxDropItem(name, queryKeyPart, TextBoxDropItem.InputType.TEXT);
			item.setInitialValue(filter.getOperator(), filter.getStringValue());
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == IntelAttribute.AttributeType.DATE){
			DateDropItem item = new DateDropItem(name, queryKeyPart);
			item.setInitialValue(filter.getOperator(), filter.getDateValues()[0], filter.getDateValues()[1]);
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == IntelAttribute.AttributeType.BOOLEAN){
			TextDropItem item = new TextDropItem(name, queryKeyPart);
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == IntelAttribute.AttributeType.LIST){
			final List<String> labels = new ArrayList<String>();
			final List<String> keys = new ArrayList<String>();
			labels.add(ANY_LABEL);
			keys.add(IQueryFilter.ANY_OPTION_KEY);
			
			if (ia.getAttributeList() != null){
				for (IntelAttributeListItem i : ia.getAttributeList()){
					labels.add(i.getName());
					keys.add(i.getKeyId());
				}
			}
			OptionDropItem item = new OptionDropItem(name, queryKeyPart, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]));
			item.setInitialValue(filter.getKeyValue());
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == IntelAttribute.AttributeType.EMPLOYEE){
			final List<String> labels = new ArrayList<String>();
			final List<String> keys = new ArrayList<String>();
			labels.add(ANY_LABEL);
			keys.add(IQueryFilter.ANY_OPTION_KEY);

			List<Employee> emps = QueryFactory.buildQuery(session, Employee.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			for (Employee e : emps) {
				labels.add(SmartLabelProvider.getFullLabel(e));
				keys.add(UuidUtils.uuidToString(e.getUuid()));
			}
			OptionDropItem item = new OptionDropItem(name, queryKeyPart, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]));
			item.setInitialValue(filter.getKeyValue());
			return Collections.singletonList(item);
		}
		return Collections.emptyList();
	}
	
	public List<DropItem> generateDropItem(DataModelFilter filter){
		
		Category category = null;
		if (filter.getCategoryKey() != null){
			
			category = QueryFactory.buildQuery(session, Category.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"hkey", filter.getCategoryKey()}).uniqueResult(); //$NON-NLS-1$
			
			if (category == null){
				DropItem di = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_InvalidCategory, filter.getCategoryKey()));
				return Collections.singletonList(di);
			}
		}
		
		if (filter.getAttributeKey() == null){
			String queryKeyPart = "dm_category:" + filter.getCategoryKey(); //$NON-NLS-1$
			DropItem di = new TextDropItem(IntelQueryColumnProvider.generateName(null, category), queryKeyPart);
			return Collections.singletonList(di);
		}
		
		String queryKeyPart = "dm_attribute:" + filter.getAttributeType().typeKey + ":"; //$NON-NLS-1$ //$NON-NLS-2$
		if (filter.getCategoryKey() != null){
			queryKeyPart += filter.getCategoryKey();
		}
		queryKeyPart += ":" + filter.getAttributeKey(); //$NON-NLS-1$
		

		Attribute attribute = QueryFactory.buildQuery(session, Attribute.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"keyId", filter.getAttributeKey()}).uniqueResult(); //$NON-NLS-1$

		if (attribute == null){
			DropItem di = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_AttributeKeyNotFound, filter.getAttributeKey()));
			return Collections.singletonList(di);
		}
	
		String name = IntelQueryColumnProvider.generateName(attribute, category);
		
		if (filter.getAttributeType() == Attribute.AttributeType.NUMERIC){
			TextBoxDropItem item = new TextBoxDropItem(name, queryKeyPart, TextBoxDropItem.InputType.NUMERIC);
			item.setInitialValue(filter.getOperator(), filter.getNumberValue().toString());
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == Attribute.AttributeType.TEXT){
			TextBoxDropItem item = new TextBoxDropItem(name, queryKeyPart, TextBoxDropItem.InputType.TEXT);
			item.setInitialValue(filter.getOperator(), filter.getStringValue());
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == Attribute.AttributeType.DATE){
			DateDropItem item = new DateDropItem(name, queryKeyPart);
			item.setInitialValue(filter.getOperator(), filter.getDateValues()[0], filter.getDateValues()[1]);
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == Attribute.AttributeType.BOOLEAN){
			TextDropItem item = new TextDropItem(name, queryKeyPart);
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == Attribute.AttributeType.LIST){
			final List<String> labels = new ArrayList<String>();
			final List<String> keys = new ArrayList<String>();
			labels.add(ANY_LABEL);
			keys.add(IQueryFilter.ANY_OPTION_KEY);
			if (attribute.getAttributeList() != null){
				for (AttributeListItem i : attribute.getAttributeList()){
					labels.add(i.getName());
					keys.add(i.getKeyId());
				}
			}
			OptionDropItem item = new OptionDropItem(name, queryKeyPart, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]));
			item.setInitialValue(filter.getKeyValue());
			return Collections.singletonList(item);
		}else if (filter.getAttributeType() == Attribute.AttributeType.TREE){
			
			AttributeTreeNode treeNode = QueryFactory.buildQuery(session, AttributeTreeNode.class,
					new Object[] {"hkey", filter.getKeyValue()}, //$NON-NLS-1$
					new Object[] {"attribute", attribute}).uniqueResult(); //$NON-NLS-1$
	
			if (treeNode == null){
				DropItem di = new ErrorDropItem(MessageFormat.format(Messages.DropItemFactory_TreeNodeNotFound, filter.getKeyValue(),attribute.getName()));
				return Collections.singletonList(di);
			}
			
			AttributeTreeDropItem item = new AttributeTreeDropItem(name, queryKeyPart, attribute.getUuid());
			item.setInitialValue(treeNode);
			return Collections.singletonList(item);
		}
		return Collections.emptyList();
	}
}
