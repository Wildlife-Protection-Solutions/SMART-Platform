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
package org.wcs.smart.cybertracker;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.util.UuidUtils;

/**
 * Utilities for supporting JSON values.
 * 
 * @author Emily
 *
 */
public class JsonUtils {
	
	//date attributes come through in a different format; only applicable to date attributes
	public static final String JSON_ATTRIBUTE_DATE_FORMAT_STR = "yyyy/MM/dd";  //$NON-NLS-1$
	
	/**
	 * JSON dates come in the format "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" where
	 * the time is the local time and the time we want to use.  We want to 
	 * throw out the timezone information because we don't care about that 
	 * and it causes issues with conversions if the local computer is in 
	 * a different timezone from other computers.
	 * @param value
	 * @throws ParseException 
	 */
	//example string: "2019-12-30T22:48:26.0-08:00"
	private static final String JSON_DATE_FORMAT_STRX = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; //$NON-NLS-1$
	private static final String JSON_DATE_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSS"; //$NON-NLS-1$
	

	public static LocalDateTime parseJsonDateTime(String value) throws DateTimeParseException {
		try {
			//this parses the date/time string throwing out any timezone information
			return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(JSON_DATE_FORMAT_STRX));
		}catch (DateTimeParseException ex) {
			
		}
		return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(JSON_DATE_FORMAT_STR));

	}
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ParseResult parseDefaultAttributeValues(JSONObject defaultValues, Session session){
		if (defaultValues == null) return new ParseResult();
		List<WaypointObservationAttribute> defaultAttributes = new ArrayList<>();
		List<String> warnings = new ArrayList<String>();
		for (Entry defaultValue : (Set<Entry>)defaultValues.entrySet()){
			
			String key = (String) defaultValue.getKey();
			String value = (String) defaultValue.getValue();
			
			String part = JsonKey.ATTRIBUTE.key + CyberTrackerConfExporter.KEY_SEP;
			if (key.startsWith(part)){
				Attribute a = null;
				try{
					a = findAttribute(key.substring(part.length()), session);
				}catch (Exception ex){
					warnings.add(MessageFormat.format(Messages.JsonUtils_AttributeNotFound + ex.getMessage(), key));
				}
				
				if (a == null){
					warnings.add(MessageFormat.format(Messages.JsonUtils_AttributeNotFound, key));
				}else{
					try{
						WaypointObservationAttribute woa = new WaypointObservationAttribute();
						woa.setAttribute(a);
						if (setAttributeValue(woa, value, session, warnings)){
							defaultAttributes.add(woa);
						}
					}catch (Exception ex){
						CyberTrackerPlugIn.log(ex.getMessage(), ex);
						warnings.add(MessageFormat.format(Messages.JsonUtils_CouldNotParseValue, a.getName(), value.toString(), ex.getMessage()));
					}
				}
			}
		}
		return new ParseResult(defaultAttributes, warnings);
	}

	/**
	 * Converts the value to an object.  Well return null if value
	 * is null or value is not an instanceo of a String or Boolean
	 * 
	 * @param value
	 * @return
	 */
	public static Boolean convertToBoolean(Object value) {
		if (value == null) return null;
		if (value instanceof Boolean) {
			return (Boolean)value;
		}else if (value instanceof String) {
			return (Boolean.valueOf((String)value));
		}
		return null;
	}
		
	public static boolean setAttributeValue(WaypointObservationAttribute toUpdate, Object value, Session session, List<String> warnings ) throws Exception{
		Attribute att = toUpdate.getAttribute();
	
		if (att.getType() == AttributeType.BOOLEAN){
			Boolean v = convertToBoolean(value);
			if (v == null) {
				//data type not supported
				warnings.add(MessageFormat.format(Messages.JsonUtils_CannotConvertToBoolean, value.toString(), att.getName()));	
			}else if (v) {
				toUpdate.setNumberValue(1.0);
			}else if (!v) {
				toUpdate.setNumberValue(0.0);
			}
		}else if (att.getType() == AttributeType.DATE){
			LocalDateTime date = null;
			try {
				date = parseJsonDateTime((String)value);				
			}catch (Exception ex) {}
			try {
				date = LocalDate.parse((String)value, DateTimeFormatter.ofPattern(JSON_ATTRIBUTE_DATE_FORMAT_STR)).atStartOfDay();
			}catch (Exception ex) {}
			
			if (date == null) throw new Exception(MessageFormat.format(Messages.JsonUtils_InvalidDate, value, JSON_DATE_FORMAT_STR, JSON_ATTRIBUTE_DATE_FORMAT_STR));

			toUpdate.setDateValue(date.toLocalDate());
		}else if (att.getType() == AttributeType.LIST){
			String listElement = (String) value;
			if (!listElement.startsWith(JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP)) throw new Exception(Messages.JsonUtils_InvalidListAttribute +listElement);
			
			AttributeListItem li = findAttributeListItem(listElement.substring(2), session);	
			if (li == null){
				warnings.add(MessageFormat.format(Messages.JsonUtils_ListItemNotFound, listElement, toUpdate.getAttribute().getName()));
				return false;
			}
			toUpdate.setAttributeListItem(li);
		}else if (att.getType() == AttributeType.MLIST) {
			List<AttributeListItem> items = (List<AttributeListItem>)value;
			
			List<WaypointObservationAttributeList> listitems = new ArrayList<>();
			for (AttributeListItem li : items) {
				WaypointObservationAttributeList woli = new WaypointObservationAttributeList();
				woli.setAttributeLisItem(li);
				woli.setObservationAttribute(toUpdate);
				listitems.add(woli);
			}
			//nothing to add
			if (listitems.isEmpty()) return false;
			
			toUpdate.setAttributeListItems(listitems);
			
		}else if (att.getType() == AttributeType.NUMERIC){
			Double value2 = null;
			if (value instanceof Number){
				value2 = ((Number)value).doubleValue();
			}
			toUpdate.setNumberValue(value2);
		}else if (att.getType() == AttributeType.TEXT){
			toUpdate.setStringValue((String)value);
		}else if (att.getType() == AttributeType.TREE){
			String treeElement = (String)value;
			if (!treeElement.startsWith(JsonKey.ATTRIBUTE_TREE.key + CyberTrackerConfExporter.KEY_SEP)) throw new Exception(Messages.JsonUtils_InvalidTreeAttribute +treeElement);
			AttributeTreeNode li = findAttributeTreeNode(treeElement.substring(2), session);
			if (li == null){
				warnings.add(MessageFormat.format(Messages.JsonUtils_TreeNodeNotFound, treeElement, toUpdate.getAttribute().getName()));
				return false;
			}
			toUpdate.setAttributeTreeNode(li);
			
		}
		return true;
	}
	
	/**
	 * Can return null if attribute not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static Attribute findAttribute(String uuid, Session session) throws Exception{
		UUID attributeUuid = UuidUtils.stringToUuid(uuid);
		Attribute a = (Attribute) session.get(Attribute.class, attributeUuid);
		return a;
	}
	
	/**
	 * Can return null if not list item found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static AttributeListItem findAttributeListItem(String uuid, Session session) throws Exception{
		AttributeListItem ai = (AttributeListItem)session.get(AttributeListItem.class, UuidUtils.stringToUuid(uuid));
		return ai;
	}
	
	/**
	 * Can return null if the tree node is not found
	 * @param uuid
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public static AttributeTreeNode findAttributeTreeNode(String uuid, Session session) throws Exception{
		AttributeTreeNode ai = (AttributeTreeNode)session.get(AttributeTreeNode.class, UuidUtils.stringToUuid(uuid));
		return ai;
	}	
	
	public static class ParseResult{
		private List<WaypointObservationAttribute> attributes;
		private List<String> warnings;
		
		public ParseResult(){
			this.attributes = Collections.emptyList();
			this.warnings = Collections.emptyList();
		}
		
		public ParseResult(List<WaypointObservationAttribute> attributes, List<String> warnings){
			this.attributes = attributes;
			this.warnings = warnings;
		}
		
		public List<String> getWarnings(){
			return this.warnings;
		}
		public List<WaypointObservationAttribute> getAttributes(){
			return attributes;
		}
	}
}
