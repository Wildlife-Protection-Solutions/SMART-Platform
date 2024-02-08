/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.model.observation;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column for patrol track
 * 
 */
public class TrackGeometryQueryColumn extends QueryColumn implements IGeometryColumn{

	public static final String KEY = "track:geometry"; //$NON-NLS-1$
	
	private Locale locale;
	public TrackGeometryQueryColumn(Locale l) {
		super(SmartContext.INSTANCE.getClass(IQueryPatrolLabelProvider.class).getLabel(KEY,l), KEY, ColumnType.GEOMETRY);
		this.locale = l;
	}

	public boolean isDefaultGeometryColumn() {
		return true;
	}

	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IGeometryResultItem iitem) {
			return iitem.asGeometry();
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		TrackGeometryQueryColumn clone = new TrackGeometryQueryColumn(this.locale);
		return clone;
	}


	@Override
	public Type getGeometryType() {
		return Type.MULTILINESTRING;
	}
}
