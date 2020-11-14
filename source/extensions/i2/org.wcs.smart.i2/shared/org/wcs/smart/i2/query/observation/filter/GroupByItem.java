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
package org.wcs.smart.i2.query.observation.filter;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Represents an entity summary query group by drop item
 * 
 * @author Emily
 *
 */
public class GroupByItem {

	public static final String INTERNAL_SEPERATOR = ":"; //$NON-NLS-1$
	
	public static enum GroupByType{
		ENTITYTYPE ("entitytype_gb"), //$NON-NLS-1$
		RECORDSOURCE ("recordsource_gb"), //$NON-NLS-1$
		RECORDSTATUS ("recordstatus_gb"), //$NON-NLS-1$
		ENTITY_ATTRIBUTE ("e_attribute_gb"), //$NON-NLS-1$
		RECORD_ATTRIBUTE ("r_attribute_gb"), //$NON-NLS-1$
		SYSTEM(SystemAttributeFilter.SA_KEY),
		CA("ca_gb"); //$NON-NLS-1$
		
		String key;
		GroupByType(String key) {
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	public static enum DateOption{
		DAY("day"), //$NON-NLS-1$
		MONTH("month"), //$NON-NLS-1$
		YEAR("year"); //$NON-NLS-1$
		
		String key;
		
		DateOption(String key) {
			this.key = key;
		}
		public String getKey() {
			return this.key;
		}
	}
	
	public static GroupByItem parse(String part) {
		String[] bits = part.split(INTERNAL_SEPERATOR);
		if (bits[0].equals(GroupByType.ENTITYTYPE.getKey())) {
			//remaining bits are keys
			List<String> ops = new ArrayList<>();
			for (int i = 1; i < bits.length; i ++) {
				String keyId = bits[i];
				ops.add(keyId);
			}
			return new GroupByItem(GroupByType.ENTITYTYPE, ops);
		}
		if (bits[0].equals(GroupByType.RECORDSOURCE.getKey())) {
			//remaining bits are keys
			List<String> ops = new ArrayList<>();
			for (int i = 1; i < bits.length; i ++) {
				String keyId = bits[i];
				ops.add(keyId);
			}
			return new GroupByItem(GroupByType.RECORDSOURCE, ops);
		}
		if (bits[0].equals(GroupByType.RECORDSTATUS.getKey())) {
			//remaining bits are keys
			List<String> ops = new ArrayList<>();
			for (int i = 1; i < bits.length; i ++) {
				String keyId = bits[i];
				ops.add(keyId);
			}
			return new GroupByItem(GroupByType.RECORDSTATUS, ops);
		}
		if (bits[0].equals(GroupByType.CA.getKey())) {
			//remaining bits are keys
			List<String> ops = new ArrayList<>();
			for (int i = 1; i < bits.length; i ++) {
				String keyId = bits[i];
				ops.add(keyId);
			}
			return new GroupByItem(GroupByType.CA, ops);
		}
		
		if (bits[0].equals(SystemAttributeFilter.SA_KEY)) {
			String sattribute = bits[1];
			String dateFilter = bits[2];
			
			SystemAttributeFilter.SystemAttribute attribute = SystemAttributeFilter.SystemAttribute.valueOf(sattribute.toUpperCase(Locale.ROOT));
			if (attribute == null) throw new IllegalStateException(MessageFormat.format("System attribute group by of {0} not supported", sattribute)); //$NON-NLS-1$
			
			DateOption op = null;
			for (DateOption key : DateOption.values()) {
				if (key.getKey().equals(dateFilter)) op = key;
			}
			if (op == null) throw new IllegalStateException("Invalid date option: " + dateFilter); //$NON-NLS-1$
			return new GroupByItem(attribute, op);
		}
		
		if (bits[0].equals(GroupByType.ENTITY_ATTRIBUTE.getKey())) {
			String attributeType = bits[1];
			String attributeKey = bits[2];
			String entityType = ""; //$NON-NLS-1$
			if (bits.length > 3 ) {
				entityType = bits[3];
			}
			IntelAttribute.AttributeType atype = IntelAttribute.AttributeType.parse(attributeType);
			
			if (atype == IntelAttribute.AttributeType.LIST || 
					atype == IntelAttribute.AttributeType.EMPLOYEE) {
				List<String> ops = new ArrayList<>();
				for (int i = 4; i < bits.length; i++){
					String keyId = bits[i];
					ops.add(keyId);
				}
				return new GroupByItem(GroupByType.ENTITY_ATTRIBUTE, attributeKey, atype, entityType, ops);
				
			}else if (atype == IntelAttribute.AttributeType.POSITION) {
				String positionType = bits[4];
				List<String> ops = new ArrayList<>();
				for (int i = 5; i < bits.length; i++){
					String keyId = bits[i];
					ops.add(keyId);
				}
				Area.AreaType type = Area.AreaType.valueOf(positionType);
				
				return new GroupByItem(GroupByType.ENTITY_ATTRIBUTE, attributeKey, atype, entityType, type, ops);
				
			}else if (atype == IntelAttribute.AttributeType.DATE) {
				String dateOp = bits[4];
				DateOption op = null;
				for (DateOption key : DateOption.values()) {
					if (key.getKey().equals(dateOp)) op = key;
				}
				if (op == null) throw new IllegalStateException("Invalid date option: " + dateOp); //$NON-NLS-1$
				return new GroupByItem(GroupByType.ENTITY_ATTRIBUTE, attributeKey, atype, entityType, op);
			}
			
		}
		if (bits[0].equals(GroupByType.RECORD_ATTRIBUTE.getKey())) {
			String attributeType = bits[1];
			String attributeKey = bits[2];
			String recordSource = bits[3]; 
			
			IntelAttribute.AttributeType atype = IntelAttribute.AttributeType.parse(attributeType);
			
			if (atype == IntelAttribute.AttributeType.LIST || 
					atype == IntelAttribute.AttributeType.EMPLOYEE) {
				List<String> ops = new ArrayList<>();
				for (int i = 4; i < bits.length; i++){
					String keyId = bits[i];
					ops.add(keyId);
				}
				return new GroupByItem(GroupByType.RECORD_ATTRIBUTE, attributeKey, atype, recordSource, ops);
				
			}else if (atype == IntelAttribute.AttributeType.POSITION) {
				String positionType = bits[4];
				List<String> ops = new ArrayList<>();
				for (int i = 5; i < bits.length; i++){
					String keyId = bits[i];
					ops.add(keyId);
				}
				Area.AreaType type = Area.AreaType.valueOf(positionType);
				
				return new GroupByItem(GroupByType.RECORD_ATTRIBUTE, attributeKey, atype, recordSource, type, ops);
				
			}else if (atype == IntelAttribute.AttributeType.DATE) {
				String dateOp = bits[4];
				DateOption op = null;
				for (DateOption key : DateOption.values()) {
					if (key.getKey().equals(dateOp)) op = key;
				}
				if (op == null) throw new IllegalStateException("Invalid date option: " + dateOp); //$NON-NLS-1$
				return new GroupByItem(GroupByType.RECORD_ATTRIBUTE, attributeKey, atype, recordSource, op);
			}
			
		}
		return null;
	}

	
	private String otherTypeKey; //for entit_attribute this is the entity key otherwise it is the recordsource key
	private String attributeKey; //for record_attribute this is the intelrecordsourceattribute key otherwise its the intelattribute key
	private IntelAttribute.AttributeType attributeType;
	private List<String> optionKeys;
	private DateOption dateOption;
	private Area.AreaType areaKey;
	private GroupByType type;
	
	private SystemAttributeFilter.SystemAttribute systemAttribute;
	
	public GroupByItem(GroupByType type, List<String> options) {
		this.type = type;
		this.optionKeys = options;
	}
	
	private GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String otherTypeKey){
		this.type = type;
		this.attributeKey = attributeKey;
		this.otherTypeKey = otherTypeKey;
		this.attributeType = attributeType;
	}
	public GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String otherTypeKey, List<String> options) {
		this(type, attributeKey, attributeType, otherTypeKey);
		this.optionKeys = options;
	}
	
	public GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String otherTypeKey, DateOption op) {
		this(type, attributeKey, attributeType, otherTypeKey);
		this.dateOption = op;
	}
	
	public GroupByItem(SystemAttributeFilter.SystemAttribute attribute, DateOption op) {
		this.type = GroupByType.SYSTEM;
		this.systemAttribute = attribute;
		this.dateOption = op;
	}

	public GroupByItem(GroupByType type, String attributeKey, IntelAttribute.AttributeType attributeType, String otherTypeKey,  Area.AreaType areaType, List<String> options) {
		this(type, attributeKey, attributeType, otherTypeKey);
		this.areaKey = areaType;
		this.optionKeys = options;
	}
	
	
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type.getKey());
		sb.append(INTERNAL_SEPERATOR);
		if (type == GroupByType.ENTITYTYPE ||
				type == GroupByType.RECORDSOURCE ||
				type == GroupByType.RECORDSTATUS ||
				type == GroupByType.CA) {
				
			for (String o : optionKeys) {
				sb.append(o);
				sb.append(INTERNAL_SEPERATOR);
			}
			sb.deleteCharAt(sb.length() - 1);
		}else if (type == GroupByType.SYSTEM) {
			sb.append(systemAttribute.getKey());
			sb.append(INTERNAL_SEPERATOR);
			sb.append(dateOption.key);
		}else if (type == GroupByType.ENTITY_ATTRIBUTE) {
			sb.append(attributeType.key);
			sb.append(INTERNAL_SEPERATOR);
			sb.append(attributeKey);
			sb.append(INTERNAL_SEPERATOR);
			if (this.otherTypeKey != null) {
				sb.append(otherTypeKey);
			}
			
			if (attributeType == AttributeType.LIST || attributeType == AttributeType.EMPLOYEE) {
				for (String o : optionKeys) {
					sb.append(INTERNAL_SEPERATOR);
					sb.append(o);	
				}
			}else if (attributeType == AttributeType.POSITION) {
				sb.append(INTERNAL_SEPERATOR);
				sb.append(areaKey.name());
				for (String o : optionKeys) {
					sb.append(INTERNAL_SEPERATOR);
					sb.append(o);	
				}
			}else if (attributeType == AttributeType.DATE) {
				sb.append(INTERNAL_SEPERATOR);
				sb.append(dateOption.key);
			}
			
			
		}else if (type == GroupByType.RECORD_ATTRIBUTE) {
			sb.append(attributeType.key);
			sb.append(INTERNAL_SEPERATOR);
			sb.append(attributeKey);
			sb.append(INTERNAL_SEPERATOR);
			sb.append(otherTypeKey);
			
			if (attributeType == AttributeType.LIST || attributeType == AttributeType.EMPLOYEE) {
				for (String o : optionKeys) {
					sb.append(INTERNAL_SEPERATOR);
					sb.append(o);	
				}
			}else if (attributeType == AttributeType.POSITION) {
				sb.append(INTERNAL_SEPERATOR);
				sb.append(areaKey.name());
				for (String o : optionKeys) {
					sb.append(INTERNAL_SEPERATOR);
					sb.append(o);	
				}
			}else if (attributeType == AttributeType.DATE) {
				sb.append(INTERNAL_SEPERATOR);
				sb.append(dateOption.key);
			}
			
		}
		return sb.toString();
	}
	
	/**
	 * For ENTITY_ATTRIBUTE this returns the entity type key.  For RECORD_ATTRIBUTE this
	 * returns the record source key;
	 * @return
	 */
	public String getOtherKey() {
		return this.otherTypeKey;
	}
	
	/**
	 * For ENTITY_ATTRIBUTE this returns the attribute key.  For RECORD_ATTRIBUTE this
	 * returns the RecordSourceAttribute key
	 * @return
	 */
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
	
	public SystemAttributeFilter.SystemAttribute getSystemAttribute(){
		return systemAttribute;
	}

	
	public List<ListItem> getAllOptions(Session session, Set<UUID> profiles, IQueryItemProvider itemProvider, LocalDate[] dateRange, Locale l) {
		if(type == GroupByType.ENTITYTYPE) {
			List<ListItem> items = new ArrayList<>();
			for (IntelEntityType t : itemProvider.getEntityTypes(profiles, session)) {
				items.add(new ListItem(t.getKeyId(), t.getName(), t.getName()));
			}
			return items;
		}
		if(type == GroupByType.RECORDSOURCE) {
			List<ListItem> items = new ArrayList<>();
			for (IntelRecordSource t : itemProvider.getRecordSources(profiles, session)) {
				items.add(new ListItem(t.getKeyId(), t.getName(), t.getName()));
			}
			return items;
		}
		if(type == GroupByType.RECORDSTATUS) {
			List<ListItem> items = new ArrayList<>();
			for (IntelRecord.Status s : IntelRecord.Status.values()) {
				items.add(new ListItem(s.name(), SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(s, Locale.getDefault())));
			}
			return items;
		}
		if (type == GroupByType.CA) {
			List<ListItem> items = new ArrayList<>();
			for (ConservationArea ca : itemProvider.getConservationAreas()) {
				items.add(new ListItem(UuidUtils.uuidToString(ca.getUuid()), ca.getNameLabel(), ca.getNameLabel()));
			}
			return items;
		}
		
		if (type == GroupByType.ENTITY_ATTRIBUTE) {
			
			String entityType = null;
			if (otherTypeKey != null && !otherTypeKey.isEmpty()) {
				IntelEntityType type = itemProvider.getEntityType(otherTypeKey, session);
				if (type == null) return Collections.emptyList();
				entityType = type.getName();
			}
			
			IntelAttribute intelAttribute = itemProvider.getAttribute(attributeKey, session);
			if (intelAttribute == null) return Collections.emptyList();
			
			List<ListItem> items = new ArrayList<>();
			if (attributeType == AttributeType.EMPLOYEE) {
				List<Employee> types = itemProvider.getEmployees(session);
				for (Employee t : types) {
					String name = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(t, l);
					String fullName = name;
					if (entityType != null) {
						fullName = fullName + " [" + intelAttribute.getName() + ": " + entityType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}else {
						fullName = fullName + " [" + intelAttribute.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					items.add(new ListItem(UuidUtils.uuidToString(t.getUuid()), name, fullName));
				}	
			}else if (attributeType == AttributeType.LIST) {
				List<IntelAttributeListItem> listItems = itemProvider.getAttributeListItems(intelAttribute.getKeyId(), session);
				for (IntelAttributeListItem i : listItems) {
					String name = i.getName();
					String fullName = name;
					if (entityType != null) {
						fullName = fullName + " [" + intelAttribute.getName() + ": " + entityType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}else {
						fullName = fullName + " [" + intelAttribute.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					items.add(new ListItem(i.getKeyId(), name, fullName));
				}
			}else if (attributeType == AttributeType.POSITION) {
				List<Area> areas = itemProvider.getAreas(areaKey, session);
				
				for (Area i : areas) {
					String name = i.getName();
					String fullName = name;
					if (entityType != null) {
						fullName = name + " [" + SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(i.getType(), l) + ": " + intelAttribute.getName() + ": " + entityType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					}else {
						fullName = name + " [" + SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(i.getType(), l) + ": " + intelAttribute.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					
					items.add(new ListItem(i.getType().name() + "_" + i.getKeyId(), name, fullName)); //$NON-NLS-1$
				}
			}else if (attributeType == AttributeType.DATE) {
				return getDateOptions(dateRange, entityType, intelAttribute.getName());
			}
			return items;
		}
		if (type == GroupByType.RECORD_ATTRIBUTE) {
			
			IntelRecordSource type = itemProvider.getRecordSource(otherTypeKey, session);
			if (type == null) return Collections.emptyList();
			
			IntelRecordSourceAttribute intelAttribute = null;
			for (IntelRecordSourceAttribute a : type.getAttributes()) {
				if (a.getKeyId().equalsIgnoreCase(attributeKey)) {
					intelAttribute = a;
					break;
				}
			}
			if (intelAttribute == null) return Collections.emptyList();

			String attName = IIntelligenceLabelProvider.getName(intelAttribute);
			List<ListItem> items = new ArrayList<>();
			if (intelAttribute.getAttribute().getType() == AttributeType.EMPLOYEE) {
				List<Employee> types = itemProvider.getEmployees(session);
				for (Employee t : types) {
					String name = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(t, l);
					String fullName = name + " [" + attName + "]"; //$NON-NLS-1$ //$NON-NLS-2$					
					items.add(new ListItem(UuidUtils.uuidToString(t.getUuid()), name, fullName));
				}	
			}else if (attributeType == AttributeType.LIST) {
				List<IntelAttributeListItem> listItems = itemProvider.getAttributeListItems(intelAttribute.getAttribute().getKeyId(), session);
				for (IntelAttributeListItem i : listItems) {
					String name = i.getName();
					String fullName = name + " [" + attName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
					items.add(new ListItem(i.getKeyId(), name, fullName));
				}
			}else if (attributeType == AttributeType.POSITION) {
				List<Area> areas = itemProvider.getAreas(areaKey, session);
				
				for (Area i : areas) {
					String name = i.getName();
					String fullName = name + " [" + SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(i.getType(), l) + ": " + attName + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					items.add(new ListItem(i.getType().name() + "_" + i.getKeyId(), name, fullName)); //$NON-NLS-1$
				}
			}else if (attributeType == AttributeType.DATE) {
				return getDateOptions(dateRange, null, attName);
			}
			return items;
		}
		if (type == GroupByType.SYSTEM) {
			return getDateOptions(dateRange, null, null);
		}
		return Collections.emptyList();
		
	}
	
	private List<ListItem> getDateOptions(LocalDate[] dateRange, String entityType, String attributeName) {
		List<ListItem> items = new ArrayList<>();

		if (dateRange == null || dateRange[0] == null || dateRange[1] == null) return items;
		
		if (dateOption == DateOption.YEAR) {
			int startYear = dateRange[0].getYear();
			int endYear = dateRange[1].getYear();
			while(startYear <= endYear) {
				String name = String.valueOf(startYear);
				String fullName = name;
				if (entityType != null) {
					fullName = name + " [" + attributeName + ": " + entityType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}else {
					fullName = name + " [" + attributeName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				items.add(new ListItem(name, name, fullName));
				startYear ++;
			}
		}else if (dateOption == DateOption.MONTH) {
			LocalDate start = LocalDate.of(dateRange[0].getYear(), dateRange[0].getMonth(), 1);
			LocalDate end = LocalDate.of(dateRange[1].getYear(), dateRange[1].getMonth(), 1);
			
			DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-M"); //$NON-NLS-1$
			DateTimeFormatter nameFormatter = DateTimeFormatter.ofPattern("MMM, yyyy"); //$NON-NLS-1$
			
			while(start.isBefore(end) || start.isEqual(end)) {
				String key = start.format(keyFormatter);
				
				String name = start.format(nameFormatter);
				String fullName = name;
				if (entityType != null) {
					fullName = name + " [" + attributeName + ": " + entityType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}else {
					fullName = name + " [" + attributeName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				items.add(new ListItem(key, name, fullName));
				
				start = start.plusMonths(1);
			}
			
		}else if (dateOption == DateOption.DAY) {
			LocalDate start = LocalDate.from(dateRange[0]);
			LocalDate end = dateRange[1];
			DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$
			DateTimeFormatter nameFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy"); //$NON-NLS-1$
			
			while(start.isBefore(end) || start.isEqual(end)) {
				String key = start.format(keyFormatter);
				String name = start.format(nameFormatter);
				String fullName = name;
				if (entityType != null) {
					fullName = name + " [" + attributeName + ": " + entityType + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}else {
					fullName = name + " [" + attributeName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				items.add(new ListItem(key, name, fullName));
				start = start.plusDays(1);
			}
		}
		return items;
	}
}
