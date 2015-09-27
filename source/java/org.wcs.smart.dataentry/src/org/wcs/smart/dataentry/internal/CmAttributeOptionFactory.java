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
package org.wcs.smart.dataentry.internal;

import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeOption.EnterOnceType;

/**
 * Factory provides common set of options used for different attributes
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CmAttributeOptionFactory {
	
	public static Map<String, CmAttributeOption> buildDefaultOptions(CmAttribute attribute, AttributeType type) {
		Map<String, CmAttributeOption> result = new HashMap<String, CmAttributeOption>();
		result.put(CmAttributeOption.ID_IS_VISIBLE, createIsVisibleOption(attribute));
		switch (type) {
		case NUMERIC:
			result.put(CmAttributeOption.ID_NUMERIC, createNumericOption(attribute));
			break;
		case LIST:
			result.put(CmAttributeOption.ID_MULTISELECT, createMultiselectOption(attribute));
			break;
		case TREE:
			result.put(CmAttributeOption.ID_FLATTEN_TREE, createFlattenTreeOption(attribute));
			break;
		default:
			break;
		}
		return result;
	}
	
	private static CmAttributeOption createIsVisibleOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_IS_VISIBLE);
		option.setDoubleValue(1.0); //true by default
		return option;
	}
	
	public static CmAttributeOption createDefaultValueOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_DEFAULT_VALUE);
		return option;
	}

	private static CmAttributeOption createMultiselectOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_MULTISELECT);
		option.setDoubleValue(0.0); //false by default
		return option;
	}

	private static CmAttributeOption createFlattenTreeOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_FLATTEN_TREE);
		option.setDoubleValue(0.0); //false by default
		return option;
	}

	private static CmAttributeOption createNumericOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_NUMERIC);
		option.setDoubleValue(0.0); //false by default
		return option;
	}
	
	public static CmAttributeOption createCustomCofigOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_CUSTOM_CONFIG);
		option.setDoubleValue(0.0); //false by default
		return option;
	}

	public static CmAttributeOption createEnterOnceOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_ENTER_ONCES);
		option.setStringValue(EnterOnceType.NONE.toString()); //not enabled by default
		return option;
	}
}
