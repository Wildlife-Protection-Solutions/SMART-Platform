package org.wcs.smart.asset.ui.views.map;

import org.eclipse.swt.widgets.TableColumn;

public class OverviewTableColumnWrapper {

	private IOverviewTableColumn column;
	private boolean isVisible;
	private boolean isFixed = false;
	private TableColumn uicolumn;
	
	private int order;
	
	public OverviewTableColumnWrapper(IOverviewTableColumn column, boolean isFixed) {
		this.column = column;
		this.isVisible = true;
		this.isFixed = isFixed;
		this.uicolumn = null;
	}
	
	public boolean isFixed() {
		return this.isFixed;
	}
	
	public boolean isVisible() {
		return this.isVisible;
	}
	
	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
		if (uicolumn != null) {
			if (isVisible) {
				uicolumn.pack();
			}else {
				uicolumn.setWidth(0);
			}
		}
	}
	
	public void setUiColumn(TableColumn uicolumn) {
		this.uicolumn = uicolumn;
	}
	
	public TableColumn getTableColumn() {
		return this.uicolumn;
	}
	
	public IOverviewTableColumn getColumn() {
		return this.column;
	}
	
	public void setOrder(int order) {
		this.order  = order;
	}
	public int getOrder() {
		return this.order;
	}
}
