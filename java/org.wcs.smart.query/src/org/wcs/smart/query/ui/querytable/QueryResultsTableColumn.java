package org.wcs.smart.query.ui.querytable;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TableViewer;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.query.ui.querytable.column.AttributeTableColumn;
import org.wcs.smart.query.ui.querytable.column.CategoryTableColumn;
import org.wcs.smart.query.ui.querytable.column.FixedTableColumn;
import org.wcs.smart.query.ui.querytable.column.QueryTableColumn;
import org.wcs.smart.query.ui.querytable.column.QueryTableViewerColumn;

public class QueryResultsTableColumn {

	
	public static QueryTableColumn[] getTableColumns(DataModel dm) {
		ArrayList<QueryTableColumn> cols = new ArrayList<QueryTableColumn>();
		for (int i = 0; i < FixedTableColumn.FIXED_COLUMN.values().length; i++) {
			cols.add(new FixedTableColumn(FixedTableColumn.FIXED_COLUMN
					.values()[i]));
		}

		// add data model category columns
		int numCategory = 0;
		for (Category cat : dm.getActiveCategories()) {
			numCategory = Math.max(numCategory, getDepth(cat));
		}

		for (int i = 0; i < numCategory; i++) {
			cols.add(new CategoryTableColumn("Observation Level " + i, i));
		}

		for (Attribute att : dm.getAttributes()) {
			cols.add(new AttributeTableColumn(att.getName(), att.getKeyId()));
		}

		return cols.toArray(new QueryTableColumn[cols.size()]);

	}

	public static QueryTableViewerColumn[] createColumns(TableViewer viewer,
			DataModel dm) {
		
		QueryTableColumn[] columns = getTableColumns(dm);
		QueryTableViewerColumn[] viewers = new QueryTableViewerColumn[columns.length];
		for (int i = 0; i < columns.length; i++) {
			viewers[i] = new QueryTableViewerColumn(viewer,columns[i]);
		}
		return viewers;
	}

	private static int getDepth(Category cat) {
		int maxDepth = 0;
		for (Category child : cat.getChildren()) {
			if (child.getIsActive()) {
				maxDepth = Math.max(maxDepth, getDepth(child));
			}
		}
		return maxDepth + 1;

	}

}
