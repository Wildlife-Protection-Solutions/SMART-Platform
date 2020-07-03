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
package org.wcs.smart.asset.query.ui.definition.dropItems;

import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.query.ui.itempanel.AttributeWrapper;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.query.ui.model.impl.AttributeDropItem;

/**
 * drop item for asset attributes
 * 
 * @author Emily
 *
 */
public class AssetAttributeDropItem extends AttributeDropItem {

	public AssetAttributeDropItem(AttributeWrapper att) {
		super(convertType(att.getAttribute()), att.getAttribute().getName(), "assetattribute:" + att.getType().key + ":" + att.getAttribute().getType().key + ":" + att.getAttribute().getKeyId());	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	
	private static Attribute.AttributeType convertType(AssetAttribute a){
		switch(a.getType()) {
		case BOOLEAN: return Attribute.AttributeType.BOOLEAN;
		case DATE: return Attribute.AttributeType.DATE;
		case LIST: return Attribute.AttributeType.LIST;
		case NUMERIC: return Attribute.AttributeType.NUMERIC;
		case TEXT: return Attribute.AttributeType.TEXT;
		default: return null;
		}
	}
}
