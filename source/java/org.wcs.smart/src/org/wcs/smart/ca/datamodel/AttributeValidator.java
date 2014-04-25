package org.wcs.smart.ca.datamodel;

import java.text.MessageFormat;
import java.util.Date;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.internal.Messages;


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
		}else if (attribute.getType() == AttributeType.TREE){
			return validateTree(attribute, value);
		}else if (attribute.getType() == AttributeType.DATE){
			return validateDate(attribute, value);
		}
		throw new IllegalStateException("Attribute type not supported."); //$NON-NLS-1$
	}
	
	public static String validateBooean(Attribute attribute, Object value){
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
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
			if (!((String)value).matches(attribute.getRegex())){
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
		if (value != null && !(value instanceof Date)){
			return MessageFormat.format(INVALID_ATT_VALUE_ERROR_MSG, new Object[]{ attribute.getName()});
		}
	
		if (attribute.getIsRequired() && value == null){
			return MessageFormat.format(REQUIRED_ERROR_MSG, new Object[]{ attribute.getName() });
		}
		return null;
	}
	
}
