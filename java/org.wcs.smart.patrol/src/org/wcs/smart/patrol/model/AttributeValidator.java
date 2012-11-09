package org.wcs.smart.patrol.model;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;

public class AttributeValidator {

	/**
	 * Validates the given value for the given attribute
	 * @param attribute
	 * @param value 
	 * @return <code>null</code> if value valid, otherwise
	 * error string
	 */
	public static String validateAttribute(Attribute attribute, Object value){
		if (attribute.getType() == AttributeType.BOOLEAN){
			return validateBooean(attribute, value);
		}else if (attribute.getType() == AttributeType.NUMERIC){
			return validateNumeric(attribute, value);
		}else if (attribute.getType() == AttributeType.TEXT){
			return validateString(attribute, value);
		}else if (attribute.getType() == AttributeType.LIST){
			return validateList(attribute, value);
		}else if (attribute.getType() == AttributeType.TREE){
			return validateTree(attribute, value);
		}
		throw new IllegalStateException("Attribute type not supported.");
	}
	
	public static String validateBooean(Attribute attribute, Object value){


		if (attribute.getIsRequired() && value == null){
			return "A value for the attribute " + attribute.getName() + " must be provided.";
		}
		return null;
	}
	
	public static String validateNumeric(Attribute attribute, Object value){
		if (value != null && !(value instanceof Double)){
			return "Invalid attribute value for attribute " + attribute.getName();
		}
	
		if (attribute.getIsRequired() && value == null){
			return "A value for the attribute " + attribute.getName() + " must be provided.";
		}
		if (value != null ){
			if (attribute.getMinValue() != null){
				if ((Double)value < attribute.getMinValue()){
					return attribute.getName() + " must be greater than " + attribute.getMinValue();
				}
			}
			if (attribute.getMaxValue() != null){
				if ((Double)value > attribute.getMaxValue()){
					return attribute.getName() + " must be less than " + attribute.getMaxValue();
				}
			}
		}
		return null;
	}
	public static String validateString(Attribute attribute, Object value){
		if (value != null && !(value instanceof String)){
			return "Invalid attribute value for attribute " + attribute.getName();
		}
	
		if (attribute.getIsRequired() && value == null){
			return "A value for the attribute " + attribute.getName() + " must be provided.";
		}
		if ((String)value != null && attribute.getRegex() != null && attribute.getRegex().length() > 0){
			if (!((String)value).matches(attribute.getRegex())){
				return "The value '" + ((String)value) + "' for attribute " + attribute.getName() + " does not match the required expression '" + attribute.getRegex() + "'";
			}
		}
		return null;
	}
	
	public static String validateList(Attribute attribute, Object value){
		if (value != null && !(value instanceof AttributeListItem)){
			return "Invalid attribute value for attribute " + attribute.getName();
		}
	
		if (attribute.getIsRequired() && value == null){
			return "A value for the attribute " + attribute.getName() + " must be provided.";
		}
		return null;
	}
	
	public static String validateTree(Attribute attribute, Object value){
		if (value != null && !(value instanceof AttributeTreeNode)){
			return "Invalid attribute value for attribute " + attribute.getName();
		}
	
		if (attribute.getIsRequired() && value == null){
			return "A value for the attribute " + attribute.getName() + " must be provided.";
		}
		return null;
	}
	
}
