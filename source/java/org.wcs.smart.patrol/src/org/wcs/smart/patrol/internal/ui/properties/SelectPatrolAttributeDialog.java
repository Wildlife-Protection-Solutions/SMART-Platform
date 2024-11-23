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
package org.wcs.smart.patrol.internal.ui.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Patrol attribute dialog for adding/removing custom patrol attributes.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class SelectPatrolAttributeDialog extends SmartStyledTitleDialog {

	private CheckboxTableViewer lstAttributes;
	private List<PatrolAttribute> attributes;
	private List<PatrolAttribute> selection;
	
	public SelectPatrolAttributeDialog(Shell parentShell, List<PatrolAttribute> attributes) {
		super(parentShell);
		this.attributes = attributes;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
	}
	
	@Override
	public void cancelPressed() {
		this.selection = Collections.emptyList();
		super.cancelPressed();
	}
	
	@Override
	public void okPressed() {
		this.selection = new ArrayList<>();
		for (Object x : lstAttributes.getCheckedElements()) {
			this.selection.add((PatrolAttribute) x);
		}
		super.okPressed();
	}
	
	public List<PatrolAttribute> getSelection(){
		return this.selection;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstAttributes = CheckboxTableViewer.newCheckList(wrapper,
				SWT.BORDER | SWT.MULTI);
		lstAttributes.getTable().setHeaderVisible(false);
		lstAttributes.getControl().addKeyListener(new CheckboxSelectorKeyAdapter(lstAttributes));
				
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 300;
		gd.widthHint = 300;
		lstAttributes.getControl().setLayoutData(gd);
		
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(new NamedIconItemLabelProvider());
		lstAttributes.setInput(attributes);
		
		Button btnAdd = new Button(main, SWT.NONE);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setText(Messages.SelectPatrolAttributeDialog_CreateNew);
		btnAdd.addListener(SWT.Selection, e->newAttribute());
		
		TableColumn tc = new TableColumn(lstAttributes.getTable(), SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		layout.setColumnData(tc, new ColumnWeightData(100));
		wrapper.setLayout(layout);
		
		setTitle(LabelConstants.CUSTOM_METADATA_NAME);
		getShell().setText(LabelConstants.CUSTOM_METADATA_NAME);
		setMessage(Messages.SelectPatrolAttributeDialog_message);
		
		return main;
	}
	
	private void newAttribute(){
		PatrolAttribute ma = new PatrolAttribute();
		ma.setType(AttributeType.LIST);
		ma.setConservationArea(SmartDB.getCurrentConservationArea());
		ma.setIsActive(true);
		ma.setPatrolTypes(new ArrayList<>());
		ma.setAttributeList(new ArrayList<>());
		ma.setAttributeTree(new ArrayList<>());
		ma.setNames(new HashSet<>());
		EditPatrolAttributeDialog dialog = new EditPatrolAttributeDialog(getShell(), ma, attributes);
		dialog.open();
		if (ma.getUuid() == null) return;
		selection = Collections.singletonList(ma);
		super.okPressed();
		
	}

	@Override
	public boolean isResizable(){
		return true;
	}
}
