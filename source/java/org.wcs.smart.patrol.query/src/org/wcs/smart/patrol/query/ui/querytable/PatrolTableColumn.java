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
package org.wcs.smart.patrol.query.ui.querytable;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolCategoryQueryColumn;
import org.wcs.smart.query.common.ui.QueryColumnLabelProvider;
import org.wcs.smart.query.common.ui.ReprojectingQueryColumnLabelProvder;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Utilities for finding table provider for given query column
 * @author Emily
 *
 */
public class PatrolTableColumn {

	public static ColumnLabelProvider getLabelProvider(QueryColumn column, List<QueryColumn> allColumns, IProjectionProvider prjProvider){
		if (column instanceof FixedQueryColumn){
			if (column.getKey().equalsIgnoreCase(FixedQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
				for (QueryColumn qc : allColumns){
					if (qc.getKey().equalsIgnoreCase(FixedQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
						return new ReprojectingQueryColumnLabelProvder(column,column,qc, prjProvider);
					}
				}
			}
			if (column.getKey().equalsIgnoreCase(FixedQueryColumn.FixedColumns.WAYPOINT_Y.getKey())){
				for (QueryColumn qc : allColumns){
					if (qc.getKey().equalsIgnoreCase(FixedQueryColumn.FixedColumns.WAYPOINT_X.getKey())){
						return new ReprojectingQueryColumnLabelProvder(column,qc,column, prjProvider);
					}
				}
			}
			return new QueryColumnLabelProvider(column);
		}else if (column instanceof PatrolAttributeQueryColumn){
			return new AttributeColumnLabelProvider(column);
		}else if (column instanceof PatrolCategoryQueryColumn){
			return new CategoryColumnLabelProvider(column);
		}else if (column instanceof GridQueryColumn){
			return new QueryColumnLabelProvider(column);
		}
		return null;
	}

}
