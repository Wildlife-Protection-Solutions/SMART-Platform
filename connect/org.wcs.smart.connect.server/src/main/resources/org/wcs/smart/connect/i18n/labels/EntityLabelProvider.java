/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.entity.IEntityLabelProvider;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;

/**
 * Entity label provider implementation.
 * 
 * @author Emily
 *
 */
public class EntityLabelProvider implements IEntityLabelProvider{

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == Status.ACTIVE) return Messages.getString("EntityLabelProvider.ActiveLabel", l); //$NON-NLS-1$
		if (item == Status.INACTIVE) return Messages.getString("EntityLabelProvider.InActiveLabel", l); //$NON-NLS-1$
		if (item == EntityType.Type.FIXED) return Messages.getString("EntityLabelProvider.FixedLabel", l); //$NON-NLS-1$
		if (item == EntityType.Type.TRANSIENT) return Messages.getString("EntityLabelProvider.TransientLabel", l); //$NON-NLS-1$

		if (item.equals(ENTITY_TYPE_REPORT_KEY )) return "{0} [Entity Type]";
		if (item.equals(ID_FIELD_KEY)) return "ID";
		if (item.equals(STATUS_FIELD_KEY)) return "Status";
		if (item.equals(X_FIELD_KEY)) return "X Position";
		if (item.equals(Y_FIELD_KEY)) return "Y Position";
		if (item.equals(CA_FIELD_KEY)) return "Conservation Area ID";
		return null;
	}

}
