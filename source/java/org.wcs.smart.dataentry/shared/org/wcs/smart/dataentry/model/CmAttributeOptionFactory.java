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
package org.wcs.smart.dataentry.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
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
		if (attribute.getAttribute().getType().isGeometry()) {
			result.put(CmAttributeOption.ID_GEOM_COLLECTION_AUTO_GPS_SEC, createGeomCollectionGpsAutoSecOption(attribute));
			result.put(CmAttributeOption.ID_GEOM_COLLECTION_OP, createGeomCollectionOption(attribute));
		}
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
		option.setVisibleWhen(CmAttributeOption.VisibleWhen.ALWAYS, null);
		return option;
	}
	
	public static CmAttributeOption createDefaultValueOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_DEFAULT_VALUE);
		return option;
	}

	public static CmAttributeOption createGeomCollectionGpsAutoSecOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_GEOM_COLLECTION_AUTO_GPS_SEC);
		option.setDoubleValue(120.0);
		return option;
	}

	public static CmAttributeOption createGeomCollectionOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_GEOM_COLLECTION_OP);
		List<Attribute.GeometrySource> items = Arrays.asList(Attribute.GeometrySource.GPS_AUTO, Attribute.GeometrySource.GPS_MANUAL, Attribute.GeometrySource.MANUAL_DRAW);
		String defaultValue = encodeGeometryCollectionOption(items); 
		option.setStringValue(defaultValue);
		
		return option;
	}
	
	public static List<Attribute.GeometrySource> parseGeometryCollectionOption(CmAttributeOption option){
		if (!option.getOptionId().equals(CmAttributeOption.ID_GEOM_COLLECTION_OP)) return Collections.emptyList();
		String parts[] = option.getStringValue().split(","); //$NON-NLS-1$
		List<Attribute.GeometrySource> items = new ArrayList<>(parts.length);
		for (String part : parts) {
			try {
				items.add(Attribute.GeometrySource.valueOf(part));
			}catch (Exception ex) {}
		}
		return items;
	}
	
	public static String encodeGeometryCollectionOption(List<Attribute.GeometrySource> ops){
		StringJoiner joiner = new StringJoiner(","); //$NON-NLS-1$
		for (Attribute.GeometrySource o : ops) {
			joiner.add(o.name());
		}
		return joiner.toString();
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
	
	public static CmAttributeOption createEnterOnceOption(CmAttribute attribute) {
		CmAttributeOption option = new CmAttributeOption();
		option.setCmAttribute(attribute);
		option.setOptionId(CmAttributeOption.ID_ENTER_ONCES);
		option.setStringValue(EnterOnceType.NONE.toString()); //not enabled by default
		return option;
	}
}
