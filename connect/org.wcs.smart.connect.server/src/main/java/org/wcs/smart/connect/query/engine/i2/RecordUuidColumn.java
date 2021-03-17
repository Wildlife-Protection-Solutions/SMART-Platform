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
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.query.engine.IntelRecordResultItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Record uuid query column
 * 
 * @author Emily
 *
 */
public class RecordUuidColumn implements IQueryColumn{
	
	private Locale l;
	
	public RecordUuidColumn(Locale l) {
		this.l = l;
	}

		@Override
		public String getColumnName() {
			return Messages.getString("IntelObservationQueryEngine.RecordUuidColumnName", l); //$NON-NLS-1$
		}

		@Override
		public Type getDataType() {
			return Type.STRING;
		}

		@Override
		public String getKey() {
			return "record_uuid"; //$NON-NLS-1$
		}

		@Override
		public Object getValue(IResultItem item) {
			UUID record = null;
			if (item instanceof IntelRecordResultItem) {
				record = ((IntelRecordResultItem) item).getRecordUuid();
			}else if (item instanceof IntelObservationResultItem) {
				record = ((IntelObservationResultItem) item).getRecordUuid();
			}
			if (record == null) return null;
			return record;
		}

		@Override
		public String getValue(IResultItem item, Locale arg1) {
			return UuidUtils.uuidToString( (UUID)getValue(item) );
		}

		@Override
		public boolean isVisible() {
			return true;
		}
}
