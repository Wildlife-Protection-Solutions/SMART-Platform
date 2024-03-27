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
package org.wcs.smart.ui.internal.ca.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class IconCellHighlighter extends FocusCellHighlighter {
	

	/**
	 * @param viewer
	 *            the viewer
	 */
	public IconCellHighlighter(ColumnViewer viewer) {
		super(viewer);
		hookListener(viewer);
	}

	private void markFocusedCell(Event event, ViewerCell cell) {
		if (cell.getColumnIndex() < 2) {
			//specific to the icon table; the first two columns don't have icons
			event.detail &= ~SWT.SELECTED;
			return;
		}
		GC gc = event.gc;

		Rectangle rect = event.getBounds();
		gc.drawFocus(rect.x, rect.y, rect.width, rect.height);

		gc.setForeground(cell.getControl().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawRectangle(rect.x, rect.y, rect.width-2, rect.height-2);
//		event.detail &= ~SWT.SELECTED;
	}

	private void removeSelectionInformation(Event event, ViewerCell cell) {

	}

	private void hookListener(final ColumnViewer viewer) {

		Listener listener = event -> {
			if ((event.detail & SWT.SELECTED) > 0) {
				ViewerCell focusCell = getFocusCell();
				if (focusCell == null) {
					event.detail &= ~SWT.SELECTED;
				}else {
					ViewerRow row = focusCell.getViewerRow();
	
					Assert.isNotNull(row, "Internal structure invalid. Item without associated row is not possible."); //$NON-NLS-1$
	
					ViewerCell cell = row.getCell(event.index);
	
					if (focusCell == null || !cell.equals(focusCell)) {
						removeSelectionInformation(event, cell);
						event.detail &= ~SWT.SELECTED;
	
					} else {
						markFocusedCell(event, cell);
					}
				}
			}
		};
		viewer.getControl().addListener(SWT.EraseItem, listener);
	}

	/**
	 * @param cell
	 *            the cell which is colored
	 * @return the color
	 */
	protected Color getSelectedCellBackgroundColor(ViewerCell cell) {
		return null;
	}

	/**
	 * @param cell
	 *            the cell which is colored
	 * @return the color
	 */
	protected Color getSelectedCellForegroundColor(ViewerCell cell) {
		return null;
	}

	@Override
	protected void focusCellChanged(ViewerCell cell, ViewerCell oldCell) {
		super.focusCellChanged(cell, oldCell);
		
		// Redraw new area
		if (cell != null) {
			Rectangle rect = cell.getBounds();
			int x = cell.getColumnIndex() == 0 ? 0 : rect.x;
			int width = cell.getColumnIndex() == 0 ? rect.x + rect.width : rect.width;
			cell.getControl().redraw(x, rect.y, width, rect.height, true);
		}

		if (oldCell != null) {
			Rectangle rect = oldCell.getBounds();
			int x = oldCell.getColumnIndex() == 0 ? 0 : rect.x;
			int width = oldCell.getColumnIndex() == 0 ? rect.x + rect.width : rect.width;
			oldCell.getControl().redraw(x, rect.y, width, rect.height, true);
		}
	}
	
	
}