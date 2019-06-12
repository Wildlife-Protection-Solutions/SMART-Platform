/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.ui;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

/**
 * Key adapter for CheckboxTableViewers that changes the
 * checkbox state with the spacebar is selected 
 * 
 * EVENTS: This listeners fires a single check state event
 * change for the first element whose check state is modified.  DOES NOT
 * fire events for all items changed. 
 * 
 * @author Emily
 * @since 7.0
 *
 */
public class CheckboxSelectorKeyAdapter extends KeyAdapter {
	
	private CheckboxTableViewer tblViewer;
	
	public CheckboxSelectorKeyAdapter(CheckboxTableViewer tblViewer) {
		this.tblViewer = tblViewer;
	}
	
	//spacebar check
	@Override
	public void keyPressed(KeyEvent e) {		
		if (e.keyCode != SWT.SPACE) return;
		if (tblViewer.getSelection().isEmpty()) return;
		
		Object item = tblViewer.getStructuredSelection().getFirstElement();
		if (item == null) return;
		boolean value = !tblViewer.getChecked( item );
		
		Event evt = null;
		for (TableItem ti : tblViewer.getTable().getSelection()) {
			if (ti.getChecked() != value) {
				ti.setChecked(value);
				if (evt == null) {
					 evt = new Event();
						evt.widget = ti;
						evt.item = ti;
						evt.detail = SWT.CHECK;
				}
			}
		}
		
		if (evt != null) tblViewer.handleSelect(new SelectionEvent(evt));
		e.doit = false;
	}
}
