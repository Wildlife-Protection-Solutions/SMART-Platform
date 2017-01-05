package org.wcs.smart.i2.ui.views.query.dropitem;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.AreaFilter;
import org.wcs.smart.i2.query.observation.filter.BooleanFilter;
import org.wcs.smart.i2.query.observation.filter.BracketFilter;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.NotFilter;
import org.wcs.smart.util.UuidUtils;

public class DropItemFactory {

	public static List<DropItem> generateDropItems(IQueryFilter filter, Session session){
		return (new DropItemFactory(session)).generateDropItems(filter);
	}
		
	public static String generateName(Area area){
		return MessageFormat.format("{0} [{1}]", area.getName(), area.getType().name());
	}
	public static String generateName(IntelAttribute attribute, IntelEntityType type){
		if (type == null){
			return attribute.getName();
		}else{
			return MessageFormat.format("{0} ({1})", attribute.getName(), type.getName());
		}
	}
	
	
	public static String generateName(IntelEntity entity){
		return MessageFormat.format("{0} ({1})", entity.getIdAttributeAsText(), entity.getEntityType().getName() );
	}
	
	private Session session;
	
	private DropItemFactory(Session session){
		this.session = session;
	}
	
	
	public List<DropItem> generateDropItems(IQueryFilter filter){
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
		
		ErrorDropItem error = new ErrorDropItem(MessageFormat.format("The query filter type {0} is not supported", filter.getClass().getName()));
		return Collections.singletonList(error);
	}
	
	
	public List<DropItem> generateDropItem(AreaFilter filter){
		
		String queryKey = "area:" + filter.getType() + ":" + filter.getKey();
		Area a = (Area) session.createCriteria(Area.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("type", filter.getType()))
				.add(Restrictions.eq("keyId", filter.getKey()))
				.uniqueResult();
		if (a == null){
			ErrorDropItem item = new ErrorDropItem(MessageFormat.format("Unable to find area of type {0} with key {1}.", filter.getType(), filter.getKey()));
			return Collections.singletonList(item);	
		}
		return Collections.singletonList(new TextDropItem(generateName(a), queryKey));
		
	}
	
	public List<DropItem> generateDropItem(EntityFilter filter){
		IntelEntity entity = (IntelEntity) session.get(IntelEntity.class, filter.getEntityUuid());
		if (entity == null){
			ErrorDropItem item = new ErrorDropItem(MessageFormat.format("Unable to find entity with uuid: {0}", filter.getEntityUuid().toString()));
			return Collections.singletonList(item);	
			
		}
		return Collections.singletonList(new TextDropItem(generateName(entity), "entity:"+UuidUtils.uuidToString(entity.getUuid())));
	}
	
	public List<DropItem> generateDropItem(EntityTypeFilter filter){
		IntelEntityType type = null; 
		if (filter.getTypeKey() != null){
			type = (IntelEntityType) session.createCriteria(IntelEntityType.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.add(Restrictions.eq("keyId", filter.getTypeKey()))
					.uniqueResult();;
			if (type == null){
				ErrorDropItem item = new ErrorDropItem(MessageFormat.format("Unable to find intelligence entity type with key: {0}", filter.getTypeKey()));
				return Collections.singletonList(item);	
			}
		}
		return Collections.singletonList(new TextDropItem(type.getName(), "entitytype:" + type.getKeyId()));
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
		String queryKeyPart = "e_attribute:" + filter.getAttributeType().key + ":" + filter.getAttributeKey() + ":";
		if (filter.getEntityTypeKey() != null){
			queryKeyPart += filter.getEntityTypeKey();
		}
		
		IntelAttribute ia = (IntelAttribute) session.createCriteria(IntelAttribute.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.add(Restrictions.eq("keyId", filter.getAttributeKey()))
				.uniqueResult();
		
		if (ia == null){
			ErrorDropItem item = new ErrorDropItem(MessageFormat.format("Unable to find intelligence attribute with key: {0}", filter.getAttributeKey()));
			return Collections.singletonList(item);
		}
		
		IntelEntityType type = null; 
		if (filter.getEntityTypeKey() != null){
			type = (IntelEntityType) session.createCriteria(IntelEntityType.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.add(Restrictions.eq("keyId", filter.getEntityTypeKey()))
					.uniqueResult();;
			if (type == null){
				ErrorDropItem item = new ErrorDropItem(MessageFormat.format("Unable to find intelligence entity type with key: {0}", filter.getEntityTypeKey()));
				return Collections.singletonList(item);	
			}
		}
		String name = generateName(ia, type);
		
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
			labels.add("<ANY>"); //TODO: make these constants
			keys.add("any");
			
			if (ia.getAttributeList() != null){
				for (IntelAttributeListItem i : ia.getAttributeList()){
					labels.add(i.getName());
					keys.add(i.getKeyId());
				}
			}
			OptionDropItem item = new OptionDropItem(name, queryKeyPart, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]));
			item.setInitialValue(filter.getKeyValue());
			return Collections.singletonList(item);
		}
		return Collections.emptyList();
	}
}
