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
package org.wcs.smart.i2.query;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.query.engine.IntelObservationResultItem;
import org.wcs.smart.i2.query.observation.filter.IColumnIdentifierProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;

/**
 * A query column that represents a query filter. 
 * @author Emily
 *
 */
public class FilterQueryColumn extends AbstractQueryColumn {

	private String filterKey;
	
	public FilterQueryColumn(String columnName, String columnKey, IColumnIdentifierProvider filterKey) {
		super(columnName, columnKey);
		this.filterKey = filterKey.getUniqueColumnIdentifier();
	}

	public String getFilterKey(){
		return this.filterKey;
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IntelObservationResultItem){
			for (Entry<IQueryFilter, Boolean> filterValue : ((IntelObservationResultItem) item).getFilterValues().entrySet()){
				if (filterValue.getKey() instanceof IColumnIdentifierProvider){
					if (((IColumnIdentifierProvider)filterValue.getKey()).getUniqueColumnIdentifier().equals(filterKey)){
						return filterValue.getValue();
					}
				}
			}
			
		}
		return null;
	}
	
	@Override
	public String getValue(IResultItem item, Locale l){
		Object toFormat = getValue(item);
		if (toFormat == null) return ""; //$NON-NLS-1$
		if ((Boolean)toFormat){
			return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, Locale.getDefault());
		}else{
			return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, Locale.getDefault());
		}
	}

	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(getFilterKey(), ((FilterQueryColumn)other).getFilterKey());
	}
	
	public int hashCode(){
		return Objects.hash(getFilterKey());
	}

	@Override
	public Type getDataType() {
		return Type.BOOLEAN;
	}
}
