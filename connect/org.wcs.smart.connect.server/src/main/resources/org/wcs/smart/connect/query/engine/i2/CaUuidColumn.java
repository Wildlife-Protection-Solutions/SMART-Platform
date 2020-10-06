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
package org.wcs.smart.connect.query.engine.i2;

import java.util.Locale;
import java.util.UUID;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.engine.EntityRecordQueryResultItem;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.query.engine.IntelRecordResultItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Profile conservation area uuid column.
 * 
 * @author Emily
 *
 */
public class CaUuidColumn implements IQueryColumn{
	
	private Locale l;
	
	public CaUuidColumn(Locale l) {
		this.l = l;
	}
	
	@Override
	public String getColumnName() {
		return Messages.getString("CaUuidColumn.CaUuidColumnName", l); //$NON-NLS-1$
	}
	@Override
	public Type getDataType() {
		return Type.STRING;
	}
	@Override
	public String getKey() {
		return "ca_uuid"; //$NON-NLS-1$
	}
	
	@Override
	public Object getValue(org.wcs.smart.i2.query.IResultItem item) {
		UUID ca = null;
		if (item instanceof IntelRecordResultItem) {
			ca = ((IntelRecordResultItem) item).getConservationAreaUuid();
		}else if (item instanceof IntelObservationResultItem) {
			ca = ((IntelObservationResultItem) item).getConservationAreaUuid();
		}else if (item instanceof EntityRecordQueryResultItem) {
			ca = ((EntityRecordQueryResultItem) item).getConservationAreaUuid();
		}
		if (ca == null) return null;
		return ca;
	}
	
	@Override
	public String getValue(org.wcs.smart.i2.query.IResultItem item, Locale locale) {
		return UuidUtils.uuidToString( (UUID)getValue(item) );
	}
	
	@Override
	public boolean isVisible() {
		return true;
	}
}
