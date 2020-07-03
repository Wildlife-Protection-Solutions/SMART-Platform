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
package org.wcs.smart.asset.query.parser.internal.filter;

import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * Filter of the form: assetattribute:<a|s|l|d>:<s|n|l|b|d>:<keyid>
 * 
 * @author Emily
 *
 */
public class AssetAttributeFilter extends AttributeFilter{
	
	public enum Source{
		ASSET("a"), //$NON-NLS-1$
		STATION("s"), //$NON-NLS-1$
		STATIONLOCATION("l"), //$NON-NLS-1$
		DEPLOYMENT("d"); //$NON-NLS-1$
		
		public String key;
		
		Source(String key){
			this.key = key;
		}
	}
	
	/**
	 * Creates a new boolean attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:b:<key>"
	 * @return
	 */
	public static AssetAttributeFilter createBooleanFilter(String attributeIdentifier){
		return new AssetAttributeFilter(attributeIdentifier, null, null);
	}
	
	/**
	 * Creates a new value attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:n:<key>"
	 * @param op the operator
	 * @param Double value the filter value
	 * @return
	 */
	public static AssetAttributeFilter createValueFilter(String attributeIdentifier, Operator op, Double value){
		return new AssetAttributeFilter(attributeIdentifier, op, value);
	}
	/**
	 * Creates a new text attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:s:<key>"
	 * @param op the string operator
	 * @param value the filter value
	 * @return
	 */
	public static AssetAttributeFilter createStringFilter(String attributeIdentifier, Operator op, String value){
		value = SharedUtils.stripQuotes(value);
		return new AssetAttributeFilter(attributeIdentifier,  op, value);
	}

	/**
	 * Creates a new date attribute filter
	 * 
	 * Date filters are of the form: <DATE> BETWEEN <DATE1> AND <DATE2>
	 * 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:s:<key>"
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return
	 */
	public static AssetAttributeFilter createDateFilter(String attributeIdentifier, String date1, String date2, Operator op){
		return new AssetAttributeFilter(attributeIdentifier, op, date1, date2);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:l:<key>"
	 * @param op the list operator
	 * @param attributeItemKey the list item key
	 * @return
	 */
	public static AssetAttributeFilter createListItemFilter(String attributeIdentifier, Operator op, String attributeItemKey){
		return new AssetAttributeFilter(attributeIdentifier,  op, attributeItemKey);
	}
			
	
	private Source source;
	
	public AssetAttributeFilter(String key, Operator op, Object value1) {
		this(key, op, value1, null);
	}
	
	public AssetAttributeFilter(String key, Operator op, Object value1, Object value2) {
		super(createAttPart(key), op, value1, value2);
		
		fullIdentifier = key;
		
		String target = key.split(":")[1]; //$NON-NLS-1$
		for (Source c : Source.values()) {
			if (c.key.equalsIgnoreCase(target)) {
				source = c;
				break;
			}
		}
		if (source == null) throw new IllegalStateException("No attribute source found for key " + target); //$NON-NLS-1$
		
	}

	public Source getSource() {
		return this.source;
	}
	
	private static String createAttPart(String attpart) {
		String[] parts = attpart.split(":"); //$NON-NLS-1$
		return "attribute:" + parts[2] + ":" + parts[3]; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
