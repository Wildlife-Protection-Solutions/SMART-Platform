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
package org.wcs.smart.i2.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.editors.RelationshipSearchJob;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog to select a relationship type based on source and target entity types
 * provided.
 * 
 * @author Emily
 *
 */
public class RelationshipSelectorDialog extends TitleAreaDialog{

	private IntelEntityType srcType, targetType;
	
	private TableViewer cmbTypes;
	private IntelRelationshipType selectedType = null;
	
	public RelationshipSelectorDialog(Shell parentShell, IntelEntityType srcType, IntelEntityType targetType) {
		super(parentShell);
		
		this.srcType = srcType;
		this.targetType = targetType;
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
//		return new Point(p.x,(int)(p.y*1.4));
		return p;
	}
	
	public IntelRelationshipType getRelationshipType(){
		return this.selectedType;
	}
	
	@Override
	protected void cancelPressed() {
		selectedType = null;
		super.cancelPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
		
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		cmbTypes = new TableViewer(parent, SWT.READ_ONLY | SWT.BORDER);
		cmbTypes.setLabelProvider(new RelationshipTypeLabelProvider());
		cmbTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbTypes.addSelectionChangedListener((event)-> {
			boolean ok = false;
			selectedType = null;
			if (cmbTypes.getSelection() instanceof IStructuredSelection){
				if ( !((IStructuredSelection)cmbTypes.getSelection()).isEmpty()){
					ok = ((IStructuredSelection)cmbTypes.getSelection()).getFirstElement() instanceof IntelRelationshipType;
					if (ok) selectedType = (IntelRelationshipType) ((IStructuredSelection)cmbTypes.getSelection()).getFirstElement();
				}
			}
			getButton(IDialogConstants.OK_ID).setEnabled(ok);
		});
		
		(new RelationshipSearchJob(srcType, targetType) {
			@Override
			protected void afterLoad() {
				Display.getDefault().syncExec(()->{
					
					Object selection = null;
					if (!rtypes.isEmpty()){
						cmbTypes.setInput(rtypes);
						selection = rtypes.get(0);
					}else{
						String noTypes = "No valid relationship types found.";
						cmbTypes.setInput(new String[]{noTypes});
						selection = noTypes;
					}
					cmbTypes.setSelection(new StructuredSelection(selection));
				});
			}
		}).schedule();
		
		
		setMessage("Select the relationship type to create");
		setTitle("New Relationship");
		getShell().setText("New Relationship");
		
		return parent;
	}
}
