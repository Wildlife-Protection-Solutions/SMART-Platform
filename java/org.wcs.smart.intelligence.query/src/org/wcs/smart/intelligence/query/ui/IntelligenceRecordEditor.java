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
package org.wcs.smart.intelligence.query.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.intelligence.query.IntelligenceQueryFactory;
import org.wcs.smart.intelligence.query.map.udig.QueryService;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.ReceivedDateFilter;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

/**
 * Intelligence record query editor.
 * @author Emily
 *
 */
public class IntelligenceRecordEditor extends QueryResultsEditor {

	public static final String ID = "org.wcs.smart.intelligence.query.record.editor"; //$NON-NLS-1$

	@Override
	public Query createNewQuery(IQueryType type) {
		if (type.getKey().equals(IntelligenceRecordQuery.KEY)){
			return IntelligenceQueryFactory.createIntelligenceRecordQuery();
		}
		return null;
	}

	@Override
	public IQueryService createQueryService() {
		return new QueryService((Query)getQueryInternal());
	}

	@Override
	protected IDateFieldFilter[] getDateFilterOptions() {
		return new IDateFieldFilter[]{ReceivedDateFilter.INSTANCE};
	}

	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column) {
		if (column instanceof FixedQueryColumn){
			return new FixedColumnLableProvider((QueryColumn)column);
		}
		return null;
	}

	public class FixedColumnLableProvider extends ColumnLabelProvider{

		private QueryColumn col;
		
		public FixedColumnLableProvider(QueryColumn col){
			this.col = col;
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof IResultItem){
				return  col.getValueAsString(col.getValue((IResultItem)element));
			}
			return super.getText(element);
		}
		
	}
}