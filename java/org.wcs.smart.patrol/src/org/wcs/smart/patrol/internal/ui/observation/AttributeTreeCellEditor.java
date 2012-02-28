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
package org.wcs.smart.patrol.internal.ui.observation;

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Cell editor for tree attributes.
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTreeCellEditor extends DialogCellEditor{

	
	public AttributeTreeCellEditor(Composite parent){
		super(parent);
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		WaypointObservationAttribute wp = (WaypointObservationAttribute)super.getValue();
		
		
		final AttributeTreeDialog dialog = new AttributeTreeDialog(super.getControl().getShell(), wp.getAttribute());

		if (dialog.open() == Window.CANCEL) {
			setValue(null);
			return null;
		}
		wp.setAttributeTreeNode(dialog.getSelection());

		return wp;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.CellEditor#getLayoutData()
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = getDefaultLabel().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return data;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#updateContents(java.lang.Object)
	 */
	@Override
	 protected void updateContents(Object value) {
		super.updateContents(value);
		if (value == null){
			return;
		}
		
		String text = "";
		if (value instanceof WaypointObservationAttribute){
			if (((WaypointObservationAttribute)value).getAttributeTreeNode() != null){
				text = ((WaypointObservationAttribute)value).getAttributeTreeNode().getName(); 
			}
		}
		getDefaultLabel().setText( text);  
    }
	
}
