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
package org.wcs.smart.common.celleditor;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.wcs.smart.util.SmartUtils;

/**
 * Cell editor for editing date fields in a table.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateCellEditor extends CellEditor{

	protected DateTime dt;
	
	/**
	 * @param parent  parent composite
	 */
	public DateCellEditor(Composite parent){
		super(parent);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.CellEditor#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) {
		dt = new DateTime(parent, SWT.DATE | SWT.MEDIUM | SWT.BORDER);
		dt.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == Character.LINE_SEPARATOR){
					handleDefaultSelection();
				}
			}
		});
		return dt;
	}
	
    /**
     * Handles a default selection event from the text control by applying the editor
     * value and deactivating this cell editor.
     * 
     */
    protected void handleDefaultSelection() {
        // same with enter-key handling code in keyReleaseOccured(e);
        fireApplyEditorValue();
        deactivate();
    }

	/**
	 * Returns a Date object of the time selected.
	 * @see org.eclipse.jface.viewers.CellEditor#doGetValue()
	 */
	@Override
	protected Object doGetValue() {
		return SmartUtils.getDate(dt);
	}

	/**
	 * @see org.eclipse.jface.viewers.CellEditor#doSetFocus()
	 */
	@Override
	protected void doSetFocus() {
		dt.setFocus();
	}

	/**
	 * Expects a Date or Calendar object.
	 * @see org.eclipse.jface.viewers.CellEditor#doSetValue(java.lang.Object)
	 */
	@Override
	protected void doSetValue(Object value) {
		Calendar cal = null;
		if (value instanceof Date){
			cal = Calendar.getInstance();
			cal.setTime((Date)value);
		}else if (value instanceof Calendar){
			cal = (Calendar)value;
		}else{
			return;
		}
		dt.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	}

	/** Re-size so entri cell is displayed
	 * @see org.eclipse.jface.viewers.CellEditor#getLayoutData()
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = dt.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return data;
	}
}
