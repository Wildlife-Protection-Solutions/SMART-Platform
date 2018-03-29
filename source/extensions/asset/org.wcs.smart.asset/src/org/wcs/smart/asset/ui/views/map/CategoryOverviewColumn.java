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
package org.wcs.smart.asset.ui.views.map;

import org.json.simple.JSONObject;

/**
 * Asset map overview column that summarizes incidents based on
 * category and attribute filters.
 * 
 * @author Emily
 *
 */
public class CategoryOverviewColumn implements IOverviewTableColumn{

	private String categoryKey;
	private String attributeFilter;
	private String name;
	
	private String key;
	
	/**
	 * Creates a new column without a key. Keys are required and should be set before saving
	 * 
	 * @param name
	 * @param categoryKey
	 * @param attributeFilter
	 */
	public CategoryOverviewColumn(String name, String categoryKey, String attributeFilter) {
		this.categoryKey = categoryKey;
		this.attributeFilter = attributeFilter;
		this.name = name;
	}
	
	/**
	 * Updates the name, category and attribute filters associated with the column
	 * @param name
	 * @param categoryKey
	 * @param attributeFilter
	 */
	public void updateValues(String name, String categoryKey, String attributeFilter) {
		this.name = name;
		this.categoryKey = categoryKey;
		this.attributeFilter = attributeFilter;
	}
	
	/**
	 * 
	 * @return the category filter
	 */
	public String getCategoryKey() {
		return this.categoryKey;
	}

	/**
	 * 
	 * @return the attribute filter
	 * 
	 */
	public String getAttributeFilter() {
		return this.attributeFilter;
	}
	
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the column key
	 */
	public void setKey(String key) {
		this.key = key;
	}
	
	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public Object getValue(StationData data) {
		return data.getColumnValue(this);
	}

	@Override
	public ColumnType getType() {
		return ColumnType.INTEGER;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject serialize() {
		JSONObject json = new JSONObject();
		json.put("type", "category"); //$NON-NLS-1$ //$NON-NLS-2$
		json.put("key", key); //$NON-NLS-1$
		json.put("name", name); //$NON-NLS-1$
		json.put("category", categoryKey); //$NON-NLS-1$
		json.put("attribute",  attributeFilter); //$NON-NLS-1$
		return json;
	}
	
	public static CategoryOverviewColumn deserialize(JSONObject json) {
		if (json.containsKey("type")) { //$NON-NLS-1$
			if (json.get("type").equals("category")) { //$NON-NLS-1$ //$NON-NLS-2$
				String key = (String)json.get("key"); //$NON-NLS-1$
				String name = (String)json.get("name"); //$NON-NLS-1$
				String category = (String)json.get("category"); //$NON-NLS-1$
				String attribute = (String)json.get("attribute"); //$NON-NLS-1$
				if (key != null && name != null && category != null && attribute != null) {
					CategoryOverviewColumn c = new CategoryOverviewColumn(name, category, attribute);
					c.setKey(key);
					return c;
				}
			}
		}
		return null;
	}

}
