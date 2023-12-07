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
package org.wcs.smart.query.model.filter;

import java.util.Objects;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.AttributeFilter.GeometryProperty;

/**
 * Simple class to track attribute information
 * about query filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeInfo {

	private String key;
	private AttributeType type;
	private AttributeFilter.GeometryProperty geometryProperty;
	/**
	 *
	 * @param key the attribute key 
	 * @param type the type of the attribute
	 */
	public AttributeInfo(String key, AttributeType type, AttributeFilter.GeometryProperty geometryProperty){
		this.key = key;
		this.type = type;
		this.geometryProperty = geometryProperty;
	}
	
	public AttributeFilter.GeometryProperty getGeometryProperty(){
		return this.geometryProperty ;
	}

	/**
	 * 
	 * @return the attribute key
	 */
	public String getAttributeKey() {
		return this.key;
	}
	
	/**
	 * 
	 * @return the name of the attribute for this column
	 */
	public String getColumnName() {
		if (this.geometryProperty == null) return "\"" + this.key.toLowerCase() + "\"";
		return "\"" + this.key + "_" + this.geometryProperty.name() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public String getColumnName(String prefix) {
		if (this.geometryProperty == null) return "\"" + prefix + this.key.toLowerCase() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		return "\"" + this.key + "_" + this.geometryProperty.name() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/**
	 * @return the attribute type
	 */
	public AttributeType getType(){
		return this.type;
	}
	
	/**
	 * @return the column name from the wp_observation_attributes table
	 *         associated with the given attribute type
	 */
	public String getTableColumn() {
		switch (this.type) {
		case LIST:
			return "list_element_uuid"; //$NON-NLS-1$
		case TREE:
			return "tree_element_uuid"; //$NON-NLS-1$
		case NUMERIC:
			return "number_value"; //$NON-NLS-1$
		case BOOLEAN:
			return "number_value"; //$NON-NLS-1$
		case TEXT:
			return "string_value"; //$NON-NLS-1$
		case DATE:
			return "string_value"; //$NON-NLS-1$
		case LINE:
		case POLYGON:
			if (this.geometryProperty == GeometryProperty.AREA) return "number_value_2"; //$NON-NLS-1$
			if (this.geometryProperty == GeometryProperty.PERIMETER) return "number_value"; //$NON-NLS-1$
			
		case MLIST:
			throw new IllegalStateException("multi list attributes not supported."); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (!other.getClass().equals(getClass())) return false;
		
		return Objects.equals(this.key, ((AttributeInfo)other).key ) &&
				Objects.equals(this.geometryProperty, ((AttributeInfo)other).geometryProperty); 
		
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.key, this.geometryProperty);
	}
}
