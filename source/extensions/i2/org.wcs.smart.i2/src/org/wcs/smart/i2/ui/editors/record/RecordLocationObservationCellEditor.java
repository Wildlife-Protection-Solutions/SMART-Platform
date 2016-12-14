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
package org.wcs.smart.i2.ui.editors.record;

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.ui.ObservationDialog;

/**
 * Record observation cell editor
 * 
 * @author Emily
 *
 */
public class RecordLocationObservationCellEditor extends DialogCellEditor {

	private IntelLocation location;	//waypoint being modified

	
	/**
	 * Creates a new observation cell editor
	 * @param parent
	 */
	public RecordLocationObservationCellEditor(Composite parent) {
		super(parent);	
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doGetValue()
	 */
	@Override
	protected Object doGetValue() {
		return location;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doSetFocus()
	 */
	@Override
	protected void doSetFocus() {
		super.doSetFocus();
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doSetValue(java.lang.Object)
	 */
	@Override
	protected void doSetValue(Object value) {
		if (value instanceof IntelLocation) {
			this.location = (IntelLocation) value;
		}
		super.doSetValue(value);
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		ObservationDialog dialog = new ObservationDialog(cellEditorWindow.getShell(), location);
		dialog.open();
		
		return location;
	}
	
	protected void updateContents(Object value) {
		if (location == null) return;
		if (getDefaultLabel() != null){
			getDefaultLabel().setText( "Edit Observations...");
		}
	}

}
