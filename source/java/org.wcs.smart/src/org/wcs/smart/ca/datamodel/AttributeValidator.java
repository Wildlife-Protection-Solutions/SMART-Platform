/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.ca.datamodel;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collection;

import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.GeometryAttribute;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.internal.Messages;

/**
 * Validates the value to be applied to the attributes
 * 
 * @author Emily
 *
 */
public class AttributeValidator {

	private static final String INVALID_ATT_VALUE_ERROR_MSG = Messages.AttributeValidator_InvalidAttributeValue;
	private static final String REQUIRED_ERROR_MSG = Messages.AttributeValidator_ValueRequired;

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
		}else if (attribute.getType() == AttributeType.MLIST){
			return validateMultiList(attribute, value);
		}else if (attribute.getType() == AttributeType.TREE){
			return validateTree(attribute, value);
		}else if (attribute.getType() == AttributeType.DATE){
			return validateDate(attribute, value);
		}else if (attribute.getType().isGeometry()) {
			return validateGeometry(attribute, value);
		}
		throw new IllegalStateException("Attribute type not supported."); //$NON-NLS-1$
	}
	
	public static String validateBooean(Attribute attribute, Object value){
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		return null;
	}
	
	public static String validateGeometry(Attribute attribute, Object value){
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		if (value == null) return null;
		
		if (!attribute.getType().isGeometry()) return null;
		
		if (!(value instanceof GeometryAttributeValue)) {
			return MessageFormat.format("Attribute value for {0} must be a Geometry.", new Object[]{ attribute.getName() });
		}
		if (attribute.getType() == AttributeType.POLYGON && !((GeometryAttributeValue)value).isPolygon()) {
			return MessageFormat.format("Geometry for attribute {0} must be a MultiPolygon.", new Object[]{ attribute.getName() });
		}
		if (attribute.getType() == AttributeType.LINE && value != null && !((GeometryAttributeValue)value).isLineString()) {
			return MessageFormat.format("Geometry for attribute {0} must be a MultiLineString.", new Object[]{ attribute.getName() });
		}
		return null;
	}
	
	public static String validateNumeric(Attribute attribute, Object value){
		if (value instanceof String ){
			//convert to double
			if (((String) value).trim().length() == 0){
				//empty string is null value
				value = null;
			}else{
				try{
					value = Double.parseDouble((String)value);
				}catch (Exception ex){
					return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
				}
			}
		}
		if (value != null && !(value instanceof Double)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
	
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		if (value != null ){
			if (attribute.getMinValue() != null){
				if ((Double)value < attribute.getMinValue()){
					return MessageFormat.format(Messages.AttributeValidator_ValueToSmall, new Object[]{attribute.getName(),attribute.getMinValue()});
				}
			}
			if (attribute.getMaxValue() != null){
				if ((Double)value > attribute.getMaxValue()){
					return MessageFormat.format(Messages.AttributeValidator_ValueToBig, new Object[]{attribute.getName(),attribute.getMaxValue()});
				}
			}
		}
		return null;
	}
	public static String validateString(Attribute attribute, Object value){
		if (value != null && !(value instanceof String)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
	
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		if ((String)value != null && attribute.getRegex() != null && attribute.getRegex().length() > 0){
			try {
				if (!((String)value).matches(attribute.getRegex())){
					return MessageFormat.format(Messages.AttributeValidator_RegexMatchFailed, new Object[]{
					((String)value),
					attribute.getName(),
					attribute.getRegex()});
				}
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
				return MessageFormat.format(Messages.AttributeValidator_RegexMatchFailed, new Object[]{
						((String)value),
						attribute.getName(),
						attribute.getRegex()});
			}
		}
		
		if (value != null && ((String)value).length() > Attribute.STRING_ATTRIBUTE_MAX_LENGTH){
			return MessageFormat.format(Messages.AttributeValidator_StringTooLong, 
					new Object[]{Attribute.STRING_ATTRIBUTE_MAX_LENGTH});
		}
		return null;
	}
	
	public static String validateList(Attribute attribute, Object value){
		if (value != null && !(value instanceof AttributeListItem)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
	
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		return null;
	}
	
	public static String validateMultiList(Attribute attribute, Object value){
		if (value != null && !(value instanceof Collection)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
		
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		
		if (value == null) return null;
		
		Collection<?> items = (Collection<?>) value;
		if (attribute.getIsRequired() && items.isEmpty()) {
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		
		for (Object x : items) {
			if (!(x instanceof AttributeListItem)) {
				return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
			}
		}
		return null;
	}
	
	public static String validateTree(Attribute attribute, Object value){
		if (value != null && !(value instanceof AttributeTreeNode)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
	
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		return null;
	}
	
	public static String validateDate(Attribute attribute, Object value){
		if (value != null && !(value instanceof LocalDate)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
	
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		return null;
	}
	
}
