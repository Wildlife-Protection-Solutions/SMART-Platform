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
package org.wcs.smart.event.i2.entity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Attribute mapping for Create Entity action
 * @author Emily
 *
 */
public class EntityMapping {

	private static final String DATE_FORMAT = "yyyy-MM-dd"; //$NON-NLS-1$
	
	//JSON keys
	private static final String JSON_FIXED2_KEY = "fixed2"; //$NON-NLS-1$
	private static final String JSON_FIXED_KEY = "fixed"; //$NON-NLS-1$
	private static final String JSON_DMATTRIBUTE_KEY = "dmattribute"; //$NON-NLS-1$
	private static final String JSON_LIST_KEY = "listmapping"; //$NON-NLS-1$
	private static final String JSON_INTELATTRIBUTE_KEY = "intelattribute"; //$NON-NLS-1$
	private static final String JSON_TYPE_KEY = "type"; //$NON-NLS-1$

	/**
	 * Type of mapping
	 *
	 */
	public enum Type{
		FIXED,DM,POSITION;
	}
	
	/**
	 * Parses the json string into a list of mappings
	 * 
	 * @param jsonstring
	 * @param session
	 * @param ca
	 * @return
	 */
	public static List<EntityMapping> parse(String jsonstring, Session session, ConservationArea ca) {
		JSONArray json = null;
		try {
			json = (JSONArray) (new JSONParser()).parse(jsonstring);
		}catch (Exception ex) {
			//TODO:
			ex.printStackTrace();
			return null;
		}
		List<EntityMapping> mappings = new ArrayList<>();
		
		for (Object oitem : json) {
			JSONObject item = (JSONObject)oitem;
			
			String typeString = (String) item.get(JSON_TYPE_KEY);
			Type type = Type.valueOf(typeString.toUpperCase());
			
			EntityMapping mapping = new EntityMapping(type);
			
			String intelAttributeKey = (String) item.get(JSON_INTELATTRIBUTE_KEY);
			IntelAttribute intelAttribute = QueryFactory.buildQuery(session, IntelAttribute.class, 
					new Object[] {"conservationArea", ca}, //$NON-NLS-1$
					new Object[] {"keyId", intelAttributeKey}).uniqueResult(); //$NON-NLS-1$
			if (intelAttribute == null) continue;
			mapping.setEntityAttribute(intelAttribute);
			
			if (type == Type.DM) {
				String dmAttributeKey = (String)item.get(JSON_DMATTRIBUTE_KEY);
				Attribute dmAttribute = QueryFactory.buildQuery(session, Attribute.class,
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"keyId", dmAttributeKey}).uniqueResult(); //$NON-NLS-1$
				if (dmAttribute == null) continue;
				mapping.setDataModelAttribute(dmAttribute);
				
				if (intelAttribute.getType() == AttributeType.LIST) {
					JSONArray listMappings = (JSONArray) item.get(JSON_LIST_KEY);
					if (listMappings != null) {
						for (Object olist : listMappings) {
							JSONObject llist = (JSONObject)olist;
							String iKey = (String) llist.keySet().iterator().next();
							String dmKey = (String) llist.get(iKey);
							mapping.addListItemMapping(iKey, dmKey);
						}
					}
				}
			}else {
				switch(intelAttribute.getType()) {
				case BOOLEAN:
					mapping.setFixedValue((Boolean)item.get(JSON_FIXED_KEY));
					break;
				case DATE:
					try {
						Date d = (new SimpleDateFormat(DATE_FORMAT)).parse((String)item.get(JSON_FIXED_KEY));
						mapping.setFixedValue(d);
					}catch (Exception ex) {
						EventPlugIn.log(ex.getMessage(),  ex);
					}
					break;
				case EMPLOYEE:
					String employeeKey = (String)item.get(JSON_FIXED_KEY);
					Employee e = session.get(Employee.class, UuidUtils.stringToUuid(employeeKey));
					mapping.setFixedEmployee(e);
					break;
				case LIST:
					String itemKey = (String)item.get(JSON_FIXED_KEY);
					for (IntelAttributeListItem iitem : intelAttribute.getAttributeList()) {
						if (iitem.getKeyId().equalsIgnoreCase(itemKey)) {
							mapping.setIntelListItem(iitem);
							break;
						}
					}		
					break;
				case NUMERIC:
					mapping.setFixedValue((Double)item.get(JSON_FIXED_KEY));
					break;
				case POSITION:
					mapping.setFixedValue((Double)item.get(JSON_FIXED_KEY), (Double)item.get(JSON_FIXED2_KEY));
					break;
				case TEXT:
					mapping.setFixedValue((String)item.get(JSON_FIXED_KEY));
					break;
				default:
					break;
				}
			}
			mappings.add(mapping);	
		}
		
		return mappings;
	}
	
	
	private Type type;
	
	private IntelAttribute intelAttribute;
	private Attribute dataModelAttribute;
	
	private String fixedStringValue;
	private Date fixedDateValue;
	private Boolean fixedBooleanValue;
	private Double fixedDoubleValue1;
	private Double fixedDoubleValue2;
	private IntelAttributeListItem intelListItem;
	private Employee fixedEmployee;
	
	private HashMap<String,String> listItemMappings = null;
	
	public EntityMapping(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public String getFixedValueAsString() {
		if (type != Type.FIXED) return ""; //$NON-NLS-1$
		switch (intelAttribute.getType()) {
		case BOOLEAN:
			if (fixedBooleanValue) return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
			return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
		case DATE:
			return DateFormat.getDateInstance().format(fixedDateValue);
		case EMPLOYEE:
			return SmartLabelProvider.getShortLabel(fixedEmployee);
		case LIST:
			if (intelListItem == null) return "";
			return intelListItem.getName();
		case NUMERIC:
			return fixedDoubleValue1.toString();
		case POSITION:
			return "POINT(" + fixedDoubleValue1.toString() + ", " + fixedDoubleValue2.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		case TEXT:
			return fixedStringValue;
		}
		return ""; //$NON-NLS-1$
	}
	
	public String getFixedStringValue() {
		return this.fixedStringValue;
	}
	public Double getFixedDouble1Value() {
		return this.fixedDoubleValue1;
	}
	public Double getFixedDouble2Value() {
		return this.fixedDoubleValue2;
	}
	public Date getFixedDateValue() {
		return this.fixedDateValue;
	}
	public Boolean getFixedBooleanValue() {
		return this.fixedBooleanValue;
	}
	public Employee getFixedEmployee() {
		return this.fixedEmployee;
	}
	
	public void setFixedEmployee(Employee value) {
		this.fixedEmployee = value;
	}
	public void setFixedValue(String value) {
		this.fixedStringValue = value;
	}
	public void setFixedValue(Date date) {
		this.fixedDateValue = date;
	}
	public void setFixedValue(Boolean bool) {
		this.fixedBooleanValue = bool;
	}
	public void setFixedValue(Double db) {
		this.fixedDoubleValue1 = db;
	}
	public void setFixedValue(Double db, Double db2) {
		this.fixedDoubleValue1 = db;
		this.fixedDoubleValue2 = db2;
	}
	public Attribute getDataModelAttribute() {
		return dataModelAttribute;
	}

	public void setDataModelAttribute(Attribute dataModelAttribute) {
		this.dataModelAttribute = dataModelAttribute;
	}


	public IntelAttribute getEntityAttribute() {
		return intelAttribute;
	}

	public void setEntityAttribute(IntelAttribute entityAttribute) {
		this.intelAttribute = entityAttribute;
	}

	public IntelAttributeListItem getIntelListItem() {
		return intelListItem;
	}

	public void setIntelListItem(IntelAttributeListItem listItem) {
		this.intelListItem = listItem;
	}
	
	public void addListItemMapping(String iitem, String dmItem) {
		if (listItemMappings == null) listItemMappings = new HashMap<>();
		listItemMappings.put(iitem, dmItem);
	}
	
	public void addListItemMapping(IntelAttributeListItem iitem, AttributeListItem dmItem) {
		addListItemMapping(iitem.getKeyId(), dmItem.getKeyId());
	}
	
	public Map<String,String> getListItemMappings(){
		if (listItemMappings == null) return Collections.emptyMap();
		return this.listItemMappings;
	}

	/**
	 * Converts the mapping to a json object
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject item = new JSONObject();
		item.put(JSON_TYPE_KEY, type.name());
		item.put(JSON_INTELATTRIBUTE_KEY, intelAttribute.getKeyId());
		
		if (type == Type.DM) {
			item.put(JSON_DMATTRIBUTE_KEY, dataModelAttribute.getKeyId());
			if (intelAttribute.getType() == IntelAttribute.AttributeType.LIST && listItemMappings != null) {
				JSONArray items = new JSONArray();
				for (Entry<String,String> map : listItemMappings.entrySet()) {
					JSONObject jmap = new JSONObject();
					jmap.put(map.getKey(), map.getValue());
					items.add(jmap);
				}
				item.put(JSON_LIST_KEY, items);
			}
		}else {
			switch(intelAttribute.getType()) {
			case BOOLEAN:
				item.put(JSON_FIXED_KEY, fixedBooleanValue);
				break;
			case DATE:
				item.put(JSON_FIXED_KEY, (new SimpleDateFormat(DATE_FORMAT)).format(fixedDateValue));
				break;
			case EMPLOYEE:
				item.put(JSON_FIXED_KEY, UuidUtils.uuidToString(fixedEmployee.getUuid()));
				break;
			case LIST:
				item.put(JSON_FIXED_KEY, intelListItem.getKeyId());
				break;
			case NUMERIC:
				item.put(JSON_FIXED_KEY, fixedDoubleValue1);
				break;
			case POSITION:
				item.put(JSON_FIXED_KEY, fixedDoubleValue1);
				item.put(JSON_FIXED2_KEY, fixedDoubleValue2);
				break;
			case TEXT:
				item.put(JSON_FIXED_KEY, fixedStringValue);
				break;
			default:
				break;
			
			}
		}
		return item;
	}

}
