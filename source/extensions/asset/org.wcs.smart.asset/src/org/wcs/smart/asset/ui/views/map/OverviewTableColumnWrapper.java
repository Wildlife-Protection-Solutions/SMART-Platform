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
package org.wcs.smart.asset.ui.views.map;

import org.eclipse.swt.widgets.TableColumn;

/**
 * Wrapper around an IOverviewTableColumn that tracks ui options 
 * 
 * @author Emily
 *
 */
public class OverviewTableColumnWrapper {

	private IOverviewTableColumn column;
	private boolean isVisible;
	private boolean isFixed = false;
	private TableColumn uicolumn;
	
	private int order;
	
	/**
	 * create new wrapper 
	 * @param column column 
	 * @param isFixed if the column is a fixed column or not
	 */
	public OverviewTableColumnWrapper(IOverviewTableColumn column, boolean isFixed) {
		this.column = column;
		this.isVisible = true;
		this.isFixed = isFixed;
		this.uicolumn = null;
	}
	
	/**
	 * 
	 * @return true if the column is fixed; false otherwise
	 */
	public boolean isFixed() {
		return this.isFixed;
	}
	
	/**
	 * 
	 * @return true if the column is visible; false otherwise
	 */
	public boolean isVisible() {
		return this.isVisible;
	}
	
	/**
	 * Sets the visible state of the column
	 * 
	 * @param isVisible
	 */
	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
		if (uicolumn != null) {
			if (isVisible) {
				uicolumn.pack();
				if (uicolumn.getWidth() < 100) uicolumn.setWidth(100);
			}else {
				uicolumn.setWidth(0);
			}
		}
	}
	
	/**
	 * Sets the ui table column 
	 * 
	 * @param uicolumn
	 */
	public void setUiColumn(TableColumn uicolumn) {
		this.uicolumn = uicolumn;
	}
	
	/**
	 * Gets the ui table column or null if not yet configured
	 * 
	 * @return
	 */
	public TableColumn getTableColumn() {
		return this.uicolumn;
	}
	
	/**
	 * 
	 * @return get the wrapped column
	 */
	public IOverviewTableColumn getColumn() {
		return this.column;
	}
	
	/**
	 * Sets the column order
	 * @param order
	 */
	public void setOrder(int order) {
		this.order  = order;
	}
	
	/**
	 * 
	 * @return the column order
	 */
	public int getOrder() {
		return this.order;
	}
}
