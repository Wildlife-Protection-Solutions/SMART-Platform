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
package org.wcs.smart.ui.ca.datamodel;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;

/**
 * Factory for creating fields for entering/editing
 * observation attributes.
 * 
 * @author egouge
 *
 */
public class AttributeFieldFactory {

	/**
	 * Creates a new attribute field based on the given attribute.
	 * 
	 * @param attribute
	 * @return
	 */
	public static IAttributeField<?> findAttributeField(Attribute attribute){
		if (attribute.getType() == AttributeType.BOOLEAN){
			return new BooleanAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.TEXT){
			return new StringAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.NUMERIC){
			return new NumericAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.LIST){
			return new ListAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.MLIST){
			return new MultiListAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.TREE){
			return new TreeAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.DATE){
			return new DateAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.TIME){
			return new TimeAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.POLYGON){
			return new GeometryAttributeField(attribute);
		}else if (attribute.getType() == AttributeType.LINE){
			return new GeometryAttributeField(attribute);
		}
		throw new IllegalStateException("Invalid attribute type."); //$NON-NLS-1$
	}
}
