package org.wcs.smart.asset.ui.views.station;

import org.eclipse.swt.graphics.Image;

public interface ITableColumn {
	
	public String getColumnName();
	
	public String getColumnValue(Object element);
	
	public default Image getImage( Object element ) { return null; }

}
