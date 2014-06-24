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
package org.wcs.smart.ct2smart.ui.support;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class DmTreeCellEditor extends CellEditor {

	private Composite main;
	private DmTreeDropDownViewer treeEditor;
	private CategoryType currentSelection = null;

	/**
	 * @param parent
	 */
	public DmTreeCellEditor(Composite parent, DmTreeDropDownViewer treeViewer) {
		super(parent);
		treeEditor = treeViewer;
	}

	@Override
	protected Control createControl(Composite parent) {
		main = new Composite(parent, SWT.BORDER);
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 5;
		gl.verticalSpacing = 5;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		main.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_WHITE ));
		
		final Label lblitem = new Label(main, SWT.NONE);
		lblitem.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_WHITE ));
		FontData fd = (lblitem.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		Font smallerFont2 = new Font(Display.getCurrent(), fd);
		lblitem.setFont(smallerFont2);
		lblitem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button btnEdit = new Button(main, SWT.DOWN | SWT.ARROW);
		fd = (btnEdit.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 2);
		Font smallerFont = new Font(Display.getCurrent(), fd);
		btnEdit.setFont(smallerFont);
		btnEdit.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
		
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				treeEditor.positionAndShow(main, new ISelectionListener() {

					@Override
					public void selectionChanged(IWorkbenchPart part, ISelection selection) {
						if (selection != null && !selection.isEmpty()){
							currentSelection = (CategoryType) ((IStructuredSelection) selection).getFirstElement();
						}
						if (!lblitem.isDisposed()){
							if (currentSelection != null) {
								lblitem.setText(treeEditor.getLabelProvider().getText(currentSelection));
							}else{
								lblitem.setText(""); //$NON-NLS-1$
							}
						}
						
//						AttributeTreeDropItem.this.queryChanged();
					}});
				
				
			}
		});
		
		if (currentSelection != null) {
			lblitem.setText(treeEditor.getLabelProvider().getText(currentSelection));
		} else {
			lblitem.setText(""); //$NON-NLS-1$
		}
		return main;
	}

	@Override
	protected Object doGetValue() {
		return currentSelection;
	}

	@Override
	protected void doSetFocus() {
		main.setFocus();
	}

	@Override
	protected void doSetValue(Object arg0) {
		if (arg0 instanceof CategoryType) {
			currentSelection = (CategoryType) arg0;
		} else {
			currentSelection = null;
		}
	}

}
