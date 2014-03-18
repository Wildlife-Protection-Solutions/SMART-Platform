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
package org.wcs.smart.entity.query.ui.definition;

import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;

/**
 * Attribute list type drop item for filtering
 * list entity attributes 
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EntityAttributeListDropItem extends AttributeListDropItem {

	public EntityAttributeListDropItem(EntityAttribute ea) {
		super(ea.getDmAttribute());

		this.key = "entity:" + ea.getEntityType().getKeyId() + ":attribute:" + ea.getDmAttribute().getType().typeKey + ":" + ea.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.text = ea.getEntityType().getName() + " - " + ea.getName(); //$NON-NLS-1$
	}
}