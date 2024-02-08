/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.asset.query.model;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.ui.IQueryAssetLabelProvider;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Asset Point geometry column 
 * 
 * @sine 8.0
 */
public class PointGeometryQueryColumn extends QueryColumn implements IGeometryColumn{

	public static final String KEY = "wp:geometry"; //$NON-NLS-1$
	
	private Locale locale;
	public PointGeometryQueryColumn(Locale l) {
		super(SmartContext.INSTANCE.getClass(IQueryAssetLabelProvider.class).getLabel(KEY, l), KEY, ColumnType.GEOMETRY);
		this.locale = l;
	}

	public boolean isDefaultGeometryColumn() {
		return true;
	}

	@Override
	public Object getValue(IResultItem item) {
		return null;
	}

	@Override
	public QueryColumn clone() {
		PointGeometryQueryColumn clone = new PointGeometryQueryColumn(locale);
		return clone;
	}

	@Override
	public Type getGeometryType() {
		return Type.POINT;
	}
}
