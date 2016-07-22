package org.wcs.smart.cybertracker;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.UuidUtils;

public class JsonUtils {
	public static DateFormat JSON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	
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
					warnings.add(MessageFormat.format("No attribute found for uuid {0}.  The default setting for this attribute will be ignored: " + ex.getMessage(), key));
				}
				
				if (a == null){
					warnings.add(MessageFormat.format("No attribute found for uuid {0}.  The default setting for this attribute will be ignored.", key));
				}else{
					try{
						WaypointObservationAttribute woa = new WaypointObservationAttribute();
						woa.setAttribute(a);
						if (setAttributeValue(woa, value, session, warnings)){
							defaultAttributes.add(woa);
						}
					}catch (Exception ex){
						CyberTrackerPlugIn.log(ex.getMessage(), ex);
						warnings.add(MessageFormat.format("The value for attribute {0} could not be parsed: ''{1}''. {2}", a.getName(), value.toString(), ex.getMessage()));
					}
				}
			}
		}
		return new ParseResult(defaultAttributes, warnings);
	}

	public static boolean setAttributeValue(WaypointObservationAttribute toUpdate, Object value, Session session, List<String> warnings ) throws Exception{
		Attribute att = toUpdate.getAttribute();
	
		if (att.getType() == AttributeType.BOOLEAN){
			if (Boolean.valueOf((String)value)){
				toUpdate.setNumberValue(1.0);
			}else{
				toUpdate.setNumberValue(0.0);
			}	
		}else if (att.getType() == AttributeType.DATE){
			Date date = JSON_DATE_FORMAT.parse((String)value);
			toUpdate.setDateValue(date);
			
		}else if (att.getType() == AttributeType.LIST){
			String listElement = (String) value;
			if (!listElement.startsWith(JsonKey.ATTRIBUTE_LIST.key + CyberTrackerConfExporter.KEY_SEP)) throw new Exception("Invalid value for list attribute: " +listElement);
			
			AttributeListItem li = findAttributeListItem(listElement.substring(2), session);	
			if (li == null){
				warnings.add(MessageFormat.format("No list item found for uuid {0}.  No value will be set for the attribute {1}.", listElement, toUpdate.getAttribute().getName()));
				return false;
			}
			toUpdate.setAttributeListItem(li);
		}else if (att.getType() == AttributeType.NUMERIC){
			Double value2 = null;
			if (value instanceof Long){
				value2 = ((Long)value).doubleValue();
			}else if (value instanceof Double){
				value2 = (Double) value;
			}
			toUpdate.setNumberValue(value2);
		}else if (att.getType() == AttributeType.TEXT){
			toUpdate.setStringValue((String)value);
		}else if (att.getType() == AttributeType.TREE){
			String treeElement = (String)value;
			if (!treeElement.startsWith(JsonKey.ATTRIBUTE_TREE.key + CyberTrackerConfExporter.KEY_SEP)) throw new Exception("Invalid value for tree attribute: " +treeElement);
			AttributeTreeNode li = findAttributeTreeNode(treeElement.substring(2), session);
			if (li == null){
				warnings.add(MessageFormat.format("No tree node found for uuid {0}.  No value will be set for the attribute {1}.", treeElement, toUpdate.getAttribute().getName()));
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
