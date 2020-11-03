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
package org.wcs.smart.entity.query.model;

import java.util.HashMap;

import org.wcs.smart.entity.query.model.columns.EntityAttributeQueryColumn;
import org.wcs.smart.query.common.engine.test.WaypointQueryResultItem;

/**
 * A class to hold the results of a waypoint 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EntityWaypointResultItem extends WaypointQueryResultItem {
	private HashMap<String, Object> entityAttributes = new HashMap<String, Object>();
	
	/**
	 * Adds an entity attribute value
	 * @param entityKey the entity key
	 * @param entityAttribute the entity attribute key
	 * @param value the value
	 */
	public void addEntityAttribute(String entityKey, String entityAttribute, Object value){
		entityAttributes.put(EntityAttributeQueryColumn.buildColumnKey(entityKey, entityAttribute), value);
	}
	
	/**
	 * 
	 * @param columnKey key of the form "<entityKey>:<entityAttributeKey>".
	 * See EntityAttributeQueryColumn.buildColumnKey
	 * 
	 * @return
	 */
	public Object getEntityAttributeValue(String columnKey){
		return entityAttributes.get(columnKey);
	}

}
