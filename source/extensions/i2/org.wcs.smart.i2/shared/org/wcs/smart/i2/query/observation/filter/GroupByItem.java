package org.wcs.smart.i2.query.observation.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

public class GroupByItem {

	public static enum GroupByType{
		ENTITYTYPE ("entitytype_gb"),
		ATTRIBUTE ("e_attribute_gb");
		
		String key;
		GroupByType(String key) {
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	public static enum DateOption{
		DAY("day"),
		MONTH("month"),
		YEAR("year");
		
		String key;
		
		DateOption(String key) {
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	public static GroupByItem parse(String part) {
		String[] bits = part.split(":");
		if (bits[0].equals(GroupByType.ENTITYTYPE.getKey())) {
			//remaining bits are keys
			List<String> ops = new ArrayList<>();
			for (int i = 1; i < bits.length; i ++) {
				String keyId = bits[i];
				ops.add(keyId);
			}
			return new GroupByItem(GroupByType.ENTITYTYPE, ops);
		}
		if (bits[0].equals(GroupByType.ATTRIBUTE.getKey())) {
			String attributeType = bits[1];
			String attributeKey = bits[2];
			String entityType = "";
			if (bits.length > 3 ) {
				entityType = bits[3];
			}
			IntelAttribute.AttributeType atype = null;
			for (IntelAttribute.AttributeType t : IntelAttribute.AttributeType.values()) {
				if (t.key.equalsIgnoreCase(attributeType)) {
					atype = t;
					break;
				}
			}
			
			if (attributeType.equals(IntelAttribute.AttributeType.LIST.key) || 
					attributeType.equals(IntelAttribute.AttributeType.EMPLOYEE.key) ) {
				List<String> ops = new ArrayList<>();
				for (int i = 4; i < bits.length; i++){
					String keyId = bits[i];
					ops.add(keyId);
				}
				return new GroupByItem(GroupByType.ATTRIBUTE, attributeKey, atype, entityType, ops);
				
			}else if (attributeType.equals(IntelAttribute.AttributeType.POSITION.key)) {
				String positionType = bits[4];
				List<String> ops = new ArrayList<>();
				for (int i = 5; i < bits.length; i++){
					String keyId = bits[i];
					ops.add(keyId);
				}
				Area.AreaType type = Area.AreaType.valueOf(positionType);
				
				return new GroupByItem(GroupByType.ATTRIBUTE, attributeKey, atype, entityType, type, ops);
				
			}else if (attributeType.equals(IntelAttribute.AttributeType.DATE.key)) {
				String dateOp = bits[4];
				DateOption op = null;
				for (DateOption key : DateOption.values()) {
					if (key.getKey().equals(dateOp)) op = key;
				}
				if (op == null) throw new IllegalStateException("Invalid date option: " + dateOp);
				return new GroupByItem(GroupByType.ATTRIBUTE, attributeKey, atype, entityType, op);
			}
			
		}
		
		return null;
	}

	
	private String entityTypeKey;
	private String attributeKey;
	private IntelAttribute.AttributeType attributeType;
	private List<String> optionKeys;
	private DateOption dateOption;
	private Area.AreaType areaKey;
	private GroupByType type;
	
	public GroupByItem(GroupByType type, List<String> options) {
		this.type = type;
		this.optionKeys = options;
	}
	
	private GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String entityTypeKey){
		this.type = type;
		this.attributeKey = attributeKey;
		this.entityTypeKey = entityTypeKey;
		this.attributeType = attributeType;
	}
	public GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String entityTypeKey, List<String> options) {
		this(type, attributeKey, attributeType, entityTypeKey);
		this.optionKeys = options;
	}
	
	public GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String entityTypeKey, DateOption op) {
		this(type, attributeKey, attributeType, entityTypeKey);
		this.dateOption = op;
	}

	public GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String entityTypeKey,  Area.AreaType areaType, List<String> options) {
		this(type, attributeKey, attributeType, entityTypeKey);
		this.areaKey = areaType;
		this.optionKeys = options;
	}
	
	public String getEntityTypeKey() {
		return this.entityTypeKey;
	}
	public String getAttributeKey() {
		return this.attributeKey;
	}
	public DateOption getDateOption() {
		return this.dateOption;
	}
	public Area.AreaType getAreaType() {
		return this.areaKey;
	}
	public GroupByType getGroupByType() {
		return this.type;
	}
	public List<String> getFilterOptions(){
		return this.optionKeys;
	}
	public IntelAttribute.AttributeType getAttributeType(){
		return this.attributeType;
	}
	
	
	public List<ListItem> getAllOptions(Session session, ConservationArea ca) {
		if(type == GroupByType.ENTITYTYPE) {
			List<ListItem> items = new ArrayList<>();
			List<IntelEntityType> types = QueryFactory.buildQuery(session, IntelEntityType.class, new Object[] {"conservationArea", ca}).list();
			for (IntelEntityType t : types) {
				items.add(new ListItem(t.getKeyId(), t.getName()));
			}
			return items;
		}
		
		if (type == GroupByType.ATTRIBUTE) {
			List<ListItem> items = new ArrayList<>();
			if (attributeType == AttributeType.EMPLOYEE) {
				List<Employee> types = QueryFactory.buildQuery(session, Employee.class, new Object[] {"conservationArea", ca}).list();
				for (Employee t : types) {
					items.add(new ListItem(UuidUtils.uuidToString(t.getUuid()), SmartLabelProvider.getFullLabel(t)));
				}	
			}else if (attributeType == AttributeType.LIST) {
				IntelAttribute temp = QueryFactory.buildQuery(session, IntelAttribute.class, 
						new Object[] {"conservationArea", ca},
						new Object[] {"keyId", attributeKey}).uniqueResult();
				for (IntelAttributeListItem i : temp.getAttributeList()) {
					items.add(new ListItem(i.getKeyId(), i.getName()));
				}
			}else if (attributeType == AttributeType.POSITION) {
				List<Area> areas = QueryFactory.buildQuery(session, Area.class, 
						new Object[] {"conservationArea", ca}, 
						new Object[] {"type", areaKey.name()}).list();
				for (Area i : areas) {
					items.add(new ListItem(i.getKeyId(), i.getName()));
				}
			}
			return items;
		}
		
		return Collections.emptyList();
		
	}
}
