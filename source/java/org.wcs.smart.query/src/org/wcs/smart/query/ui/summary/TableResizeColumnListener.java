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
package org.wcs.smart.query.ui.summary;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A listener for resizing tables with no header.  This listeners
 * to mouse over events and if you are on a column border it changes the cursor
 * and allows you to move the size of the column.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class TableResizeColumnListener implements Listener {

	private int moveIndex = -1;
	private int lastX = 0;
	private boolean mouseDown = false;
	private Table table;
	private Cursor ewCursor = null;
	private Listener onMouseUp;
	private Listener onColumnResize;
	
	/**
	 * @param table the table to track
	 * @param onMouseUp called when a column is finished resizing;  
	 * the data field of the associated event will contain an array of int[]
	 * where the first element is the index of the column being resized and the
	 * second is the new column width 
	 * @param onColumnResize called when a column is being resized
	 */
	public TableResizeColumnListener(Table table, Listener onMouseUp, Listener onColumnResize) {
		this.table = table;
		ewCursor = table.getShell().getDisplay()
				.getSystemCursor(SWT.CURSOR_SIZEWE);
		
		this.onMouseUp = onMouseUp;
		this.onColumnResize = onColumnResize;
	}

	@Override
	public void handleEvent(Event event) {

		if (event.type == SWT.MouseDown) {
			// find column being re-sized
			int width = 0;
			for (int i = 0; i < table.getColumnCount(); i++) {
				width = width + table.getColumn(i).getWidth();
				if (width - 5 <= event.x && event.x <= width + 5) {
					moveIndex = i;
				}
			}
			lastX = event.x;
			mouseDown = true;
			table.setCursor(ewCursor);
			table.setCapture(true);

		} else if (event.type == SWT.MouseUp) {
			// reset
			mouseDown = false;
			moveIndex = -1;
			lastX = 0;
			table.setCapture(false);
			table.setCursor(null);

			// update table width

			if (onMouseUp != null){
				onMouseUp.handleEvent(event);
			}

			table.getParent().layout();

		} else if (event.type == SWT.MouseMove) {
			if (moveIndex < 0) {
				table.setCursor(null);
				int width = 0;
				for (int i = 0; i < table.getColumnCount(); i++) {
					width = width + table.getColumn(i).getWidth();
					if (width - 5 <= event.x && event.x <= width + 5) {
						// can move = true;
						table.setCursor(ewCursor);
					}
				}
			} else if (mouseDown && moveIndex >= 0) {
				// resize colum in this table and the main table
				int prev = lastX - event.x;
				lastX = event.x;
				TableColumn col = table.getColumn(moveIndex);
				int newWidth = col.getWidth() - prev;
				if (newWidth < 10) {
					newWidth = 10;
				}
				col.setWidth(newWidth);
				event.data = new int[]{moveIndex,newWidth};
				if (onColumnResize != null){
					onColumnResize.handleEvent(event);
				}
			}
		}

	}

}
