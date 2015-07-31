package org.wcs.smart.internal;

import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.LabelConstants;

/**
 * The SMART label provide must provide implementations for:
 * Boolean objects(both true and false) and for
 * Employee object (the full name and id).
 * 
 * @author Emily
 *
 */
public class SmartLabelProvider implements ICoreLabelProvider {

	@Override
	public String getLabel(Object value, Locale l) {
		if (value instanceof Boolean){
			if ((Boolean)value){
				return LabelConstants.BOOLEAN_TRUE_LABEL;
			}else{
				return LabelConstants.BOOLEAN_FALSE_LABEL;
			}
		}else if (value instanceof Employee){
			return LabelConstants.getFullLabel((Employee)value);
		}
		return null;
	}

}
