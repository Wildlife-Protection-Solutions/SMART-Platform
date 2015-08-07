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
package org.wcs.smart.entity.ui;

import java.util.Locale;

import org.wcs.smart.entity.IEntityLabelProvider;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;
/**
 * Entity label provider implementation.
 * 
 * @author Emily
 *
 */
public class EntityLabelProvider implements IEntityLabelProvider{

	public static final String ID_FIELD_NAME = Messages.Entity_IDFieldName;
	public static final String STATUS_FIELD_NAME = Messages.Entity_StatusFieldName;
	public static final String X_FIELD_NAME = Messages.Entity_XFieldName;
	public static final String Y_FIELD_NAME = Messages.Entity_YFieldName;
	public static final String CA_FIELD_NAME = Messages.Entity_CaIdFieldName;
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item == Status.ACTIVE) return Messages.Entity_ActiveStatusLabel;
		if (item == Status.INACTIVE) return Messages.Entity_InActiveStatusLabel;
		if (item == EntityType.Type.FIXED) return Messages.EntityType_FixedTypeLabel;
		if (item == EntityType.Type.TRANSIENT) return Messages.EntityType_TransientTypeLabel;

		return null;
	}

}
