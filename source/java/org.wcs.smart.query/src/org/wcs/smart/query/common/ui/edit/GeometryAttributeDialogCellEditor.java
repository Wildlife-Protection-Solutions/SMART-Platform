/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.query.common.ui.edit;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ui.DrawOnMapDialog;

/**
 * Cell editor that automatically opens a dialog box
 * where the user can edit a geometry. 
 * 
 * @author Emily
 * @since 8.0.0
 *
 */
public class GeometryAttributeDialogCellEditor extends CellEditor {

	protected Object inputValue;

	protected Geometry updatedValue;

	private Attribute.AttributeType type;
	
	/**
	 * Creates a new cell editor 
	 *
	 * @param parent
	 *            the parent control
	 */
	public GeometryAttributeDialogCellEditor(Composite parent, Attribute.AttributeType type) {
		super(parent);
		this.type = type;
		if (!type.isGeometry()) throw new IllegalStateException();
	}


	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		super.activate(activationEvent);
		getControl().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (inputValue != null && !(inputValue instanceof Geometry)){
					fireCancelEditor();
					return;
				}
				DrawOnMapDialog.Type mdtype = type == AttributeType.LINE ? DrawOnMapDialog.Type.LINESTRING : DrawOnMapDialog.Type.POLYGON;
				
				DrawOnMapDialog dialog = new DrawOnMapDialog(getControl().getShell(), mdtype, (Geometry)inputValue);
				int ret = dialog.open();
				if (ret == Window.CANCEL){
					fireCancelEditor();
				}else{
					updatedValue = dialog.getGeometry();
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
	
	@Override
	public LayoutData getLayoutData() {
		LayoutData layoutData = super.getLayoutData();
		layoutData.verticalAlignment = SWT.CENTER;
		layoutData.minimumHeight = getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT,true).y;
		return layoutData;
	}

	/**
	 * will return a Geometry
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
	 * @param value should be a geometry
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
