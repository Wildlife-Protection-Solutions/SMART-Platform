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
package org.wcs.smart.query.ui.querylist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * This is a custom FocusCellHighlighter
 * that allows multiple selection.
 * 
 * Solution taken from:
 * http://www.eclipse.org/forums/index.php?t=msg&goto=334579&
 * 
 * @author egouge
 * @since 1.0.0
 */
public class MultiFocusCellOwnerDrawHighlighter extends FocusCellHighlighter {
	/**
	 * Create a new instance which can be passed to a
	 * {@link TreeViewerFocusCellManager}
	 * 
	 * @param viewer
	 *            the viewer
	 */
	public MultiFocusCellOwnerDrawHighlighter(ColumnViewer viewer) {
		super(viewer);
		hookListener(viewer);
	}

	private void markFocusedCell(Event event, ViewerCell cell) {
		Color background = (cell.getControl().isFocusControl()) ? getSelectedCellBackgroundColor(cell)
				: getSelectedCellBackgroundColorNoFocus(cell);
		Color foreground = (cell.getControl().isFocusControl()) ? getSelectedCellForegroundColor(cell)
				: getSelectedCellForegroundColorNoFocus(cell);

		if (foreground != null || background != null || onlyTextHighlighting(cell)) {
			GC gc = event.gc;

			if (background == null) {
				background = cell.getItem().getDisplay().getSystemColor(
						SWT.COLOR_LIST_SELECTION);
			}

			if (foreground == null) {
				foreground = cell.getItem().getDisplay().getSystemColor(
						SWT.COLOR_LIST_SELECTION_TEXT);
			}

			gc.setBackground(background);
			gc.setForeground(foreground);
			
			if (onlyTextHighlighting(cell)) {
				Rectangle area = event.getBounds();
				Rectangle rect = cell.getTextBounds();
				if( rect != null ) {
					area.x = rect.x;
				}
				gc.fillRectangle(area);
			} else {
				gc.fillRectangle(event.getBounds());
			}
			
			event.detail &= ~SWT.SELECTED;
		}
	}

	private void removeSelectionInformation(Event event, ViewerCell cell) {
		GC gc = event.gc;
		gc.setBackground(cell.getViewerRow().getBackground(
				cell.getColumnIndex()));
		gc.setForeground(cell.getViewerRow().getForeground(
				cell.getColumnIndex()));
		gc.fillRectangle(cell.getBounds());
		event.detail &= ~SWT.SELECTED;
	}

	private void hookListener(final ColumnViewer viewer) {

		Listener listener = new Listener() {

			public void handleEvent(Event event) {
				
				if ((event.detail & SWT.SELECTED) > 0 &&						
						((StructuredSelection)viewer.getSelection()).size() == 1) {
//					ViewerCell focusCell = getFocusCell();
					//ViewerRow row = viewer.getViewerRowFromItem(event.item);
					ViewerCell focusCell = viewer.getCell(new Point(event.x, event.y));
					if (focusCell == null){
						return;
					}
					ViewerRow row = focusCell.getViewerRow();

					Assert.isNotNull(row,"Internal structure invalid. Item without associated row is not possible."); //$NON-NLS-1$

					ViewerCell cell = row.getCell(event.index);

					if (focusCell == null || !cell.equals(focusCell)) {
						removeSelectionInformation(event, cell);
					} else {
						markFocusedCell(event, cell);
					}
					
				}
			}

		};
		viewer.getControl().addListener(SWT.EraseItem, listener);
	}

	/**
	 * The color to use when rendering the background of the selected cell when
	 * the control has the input focus
	 * 
	 * @param cell
	 *            the cell which is colored
	 * @return the color or <code>null</code> to use the default
	 */
	protected Color getSelectedCellBackgroundColor(ViewerCell cell) {
		return null;
	}

	/**
	 * The color to use when rendering the foreground (=text) of the selected
	 * cell when the control has the input focus
	 * 
	 * @param cell
	 *            the cell which is colored
	 * @return the color or <code>null</code> to use the default
	 */
	protected Color getSelectedCellForegroundColor(ViewerCell cell) {
		return null;
	}

	/**
	 * The color to use when rendering the foreground (=text) of the selected
	 * cell when the control has <b>no</b> input focus
	 * 
	 * @param cell
	 *            the cell which is colored
	 * @return the color or <code>null</code> to use the same used when
	 *         control has focus
	 * @since 3.4
	 */
	protected Color getSelectedCellForegroundColorNoFocus(ViewerCell cell) {
		return null;
	}

	/**
	 * The color to use when rendering the background of the selected cell when
	 * the control has <b>no</b> input focus
	 * 
	 * @param cell
	 *            the cell which is colored
	 * @return the color or <code>null</code> to use the same used when
	 *         control has focus
	 * @since 3.4
	 */
	protected Color getSelectedCellBackgroundColorNoFocus(ViewerCell cell) {
		return null;
	}

	/**
	 * Controls whether the whole cell or only the text-area is highlighted
	 * 
	 * @param cell
	 *            the cell which is highlighted
	 * @return <code>true</code> if only the text area should be highlighted
	 * @since 3.4
	 */
	protected boolean onlyTextHighlighting(ViewerCell cell) {
		return false;
	}

	protected void focusCellChanged(ViewerCell newCell, ViewerCell oldCell) {
		super.focusCellChanged(newCell, oldCell);
		
		// Redraw new area
		if (newCell != null) {
			Rectangle rect = newCell.getBounds();
			int x = newCell.getColumnIndex() == 0 ? 0 : rect.x;
			int width = newCell.getColumnIndex() == 0 ? rect.x + rect.width
					: rect.width;
			// 1 is a fix for Linux-GTK
			newCell.getControl().redraw(x, rect.y - 1, width, rect.height + 1,
					true);
		}

		if (oldCell != null) {
			Rectangle rect = oldCell.getBounds();
			int x = oldCell.getColumnIndex() == 0 ? 0 : rect.x;
			int width = oldCell.getColumnIndex() == 0 ? rect.x + rect.width
					: rect.width;
			// 1 is a fix for Linux-GTK
			oldCell.getControl().redraw(x, rect.y - 1, width, rect.height + 1,
					true);
		}
	}
}
