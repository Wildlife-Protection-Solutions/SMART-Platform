package org.wcs.smart.query.common.ui;

import java.util.Locale;

import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.IGridQueryColumnLabelProvider;

public class GridQueryColumnLabelProvider implements IGridQueryColumnLabelProvider {

	@Override
	public String getLabel(Object key, Locale l) {
		if (key instanceof GridQueryColumn.GridColumns){
			switch((GridQueryColumn.GridColumns)key){
				case TILE_X:return Messages.GridQueryColumn_TileXIdColumnName;
				case TILE_Y:return Messages.GridQueryColumn_TileYIdColumnName;
				case VALUE:return Messages.GridQueryColumn_ValueColumnName;
			}
		}
		if (key.equals(GRID_TO_BIG_KEY)){
			return Messages.Grid_GridToLargeError;
		}
		return null;
	}
}
