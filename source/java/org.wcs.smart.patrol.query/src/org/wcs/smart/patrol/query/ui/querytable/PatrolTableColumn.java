package org.wcs.smart.patrol.query.ui.querytable;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolCategoryQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolGridQueryColumn;
import org.wcs.smart.query.common.ui.GridColumnLabelProvider;
import org.wcs.smart.query.model.QueryColumn;

public class PatrolTableColumn {

	public static ColumnLabelProvider getLabelProvider(QueryColumn column){
		if (column instanceof FixedQueryColumn){
			return new FixedColumnLabelProvider(column);
		}else if (column instanceof PatrolAttributeQueryColumn){
			return new AttributeColumnLabelProvider(column);
		}else if (column instanceof PatrolCategoryQueryColumn){
			return new CategoryColumnLabelProvider(column);
		}else if (column instanceof PatrolGridQueryColumn){
			return new GridColumnLabelProvider(column);
		}
		return null;
	}

}
