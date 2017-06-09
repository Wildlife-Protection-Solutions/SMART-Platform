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
package org.wcs.smart.observation.query.ui;

import java.util.UUID;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * Cell editor that automatically opens a dialog box
 * where the user can edit an observation. 
 * 
 * @author Emily
 *
 */
public class ObservationDialogCellEditor extends CellEditor {

	/**
	 * The custom combo box control.
	 */
	protected EditObservationDialog observationDialog;

	protected Object inputValue;

	protected WaypointObservation updatedValue;

	/**
	 * Creates a new cell editor with a combo viewer and a default style
	 *
	 * @param parent
	 *            the parent control
	 */
	public ObservationDialogCellEditor(Composite parent) {
		super(parent);
	}


	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		super.activate(activationEvent);
		getControl().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (inputValue != null && !(inputValue instanceof UUID)){
					fireCancelEditor();
					return;
				}
				EditObservationDialog dialog = new EditObservationDialog(getControl().getShell(), (UUID)inputValue);
				int ret = dialog.open();
				if (ret == Window.CANCEL){
					fireCancelEditor();
				}else{
					updatedValue = dialog.getUpdatedObservation();
					applyEditorValueAndDeactivate();
				}
			}
		});
	}
	
	@Override
	protected Control createControl(Composite parent) {		
		Label label = new Label(parent, SWT.NONE);
		return label;
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method returns the zero-based index
	 * of the current selection.
	 *
	 * @return the zero-based index of the current selection wrapped as an
	 *         <code>Integer</code>
	 */
	@Override
	protected Object doGetValue() {
		return updatedValue;
	}

	@Override
	protected void doSetFocus() {	
	}


	private Label getLabel(){
		return (Label)getControl();
	}
	/**
	 * Set a new value
	 *
	 * @param value
	 *            the new value
	 */
	@Override
	protected void doSetValue(Object value) {
		if (getLabel() == null) return;
		getLabel().setText("..."); //$NON-NLS-1$
		inputValue = value;
	}

	/**
	 * Applies the currently selected value and deactiavates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		fireApplyEditorValue();
		deactivate();
	}

	@Override
	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}

	@Override
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		} else if (keyEvent.character == '\t') { // tab key
			applyEditorValueAndDeactivate();
		}
	}
}
