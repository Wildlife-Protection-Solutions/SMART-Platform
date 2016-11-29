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
package org.wcs.smart.i2.ui.editors;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.i2.ui.SmartShellDialog;

/**
 * For the entity editor, adds listeners required to a column view to display
 * a shell tooltip style dialog box.
 * 
 * @author Emily
 *
 * @param <T>
 * @param <D>
 */
public abstract class AbstractEntityEditorShellListener<T,D extends SmartShellDialog> implements Listener {

	private boolean doHover = false;

	private ColumnViewer viewer;
	private int column = -1;
	
	protected D shellDialog;
	
	/**
	 * tooltip only displays when hover over the given column 
	 * @param viewer
	 */
	public AbstractEntityEditorShellListener(ColumnViewer viewer){
		this.viewer = viewer;
		
		viewer.getControl().addListener(SWT.MouseDoubleClick, this);
		viewer.getControl().addListener(SWT.MouseDown, this);
		viewer.getControl().addListener(SWT.MouseUp, this);
		viewer.getControl().addListener(SWT.MouseMove, this);
		viewer.getControl().addListener(SWT.MouseHover, this);
	}
	
	/**
	 * tooltip displays when hover over any column 
	 * @param viewer
	 * @param column
	 */
	public AbstractEntityEditorShellListener(ColumnViewer viewer, int column){
		this(viewer);
		this.column = column;
	}
	

	/**
	 * Provides the shell dialog to show.  You can provide the existing shellDialog if
	 * you don't want to close and reopen a new one. Otherwise the existing dialog is closed
	 * and they provided one is opened.  Return null if nothing to display.
	 * 
	 * @param currentSelection
	 * @return
	 */
	protected abstract D getShellDialog(T currentSelection);
	
	@Override
	public void handleEvent(Event event) {
		switch (event.type) {
		case SWT.MouseDoubleClick:
		case SWT.MouseDown:
		case SWT.MouseUp:
			doHover = false;
			break;
		case SWT.MouseMove:
			doHover = true;
			break;
		case SWT.MouseHover:
			if (doHover) {
				doHover(event.x, event.y);
			}
			break;
		}

	}
	
	private void doHover(int x, int y) {

		ViewerCell cell = viewer.getCell(new Point(x, y));
		if (cell == null){
			closeShell();
			return;
			
		}
		if (column != -1){
			if (cell.getColumnIndex() != column) {
				closeShell();
				return;
			}
		}
		
		if (cell != null) {
			T element = (T) cell.getElement();
			D d = getShellDialog(element);
			if (d == null || d != shellDialog){
				closeShell();	
			}
			if (d != null && d != shellDialog){
				shellDialog = d;
				
				int height = shellDialog.getSize().y;
				Point p = viewer.getControl().toDisplay(x, y);
				shellDialog.open(new Point(p.x, p.y - height));
			}
		}
	}
	private void closeShell(){
		if (shellDialog != null && !shellDialog.isDisposed()){
			shellDialog.close();
		}
	}

}
