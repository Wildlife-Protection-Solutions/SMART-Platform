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
package org.wcs.smart.asset.ui.views.map;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for configuring asset over map columns that count incidents based
 * on category/attribute filters.
 * 
 * @author Emily
 *
 */
public class CategoryColumnDialog extends TitleAreaDialog {

	private enum Type{
		CATEGORY(Messages.CategoryColumnDialog_CategoryColumnType),
		COMBINED(Messages.CategoryColumnDialog_CombinedColumnType);
		
		String name;
		Type(String name){
			this.name = name;
		}
	}
	private Text txtName;
	
	private CategoryColumnComposite categoryOption;
	private CombinedColumnComposite combinedOption;
	
	private IOverviewTableColumn toUpdate;
	private IOverviewTableColumn newColumn;
	
	private Composite stackPanel;
	private List<IOverviewTableColumn> allColumns;
	/**
	 * Creates a new dialog for creating a new columns
	 * @param parentShell
	 */
	public CategoryColumnDialog(Shell parentShell, List<IOverviewTableColumn> allColumns) {
		super(parentShell);
		this.allColumns = allColumns;
	}

	/**
	 * Creates a new dialog for editing an existing column
	 * @param parentShell
	 * @param toUpdate
	 */
	public CategoryColumnDialog(Shell parentShell, IOverviewTableColumn toUpdate, List<IOverviewTableColumn> allColumns) {
		super(parentShell);
		this.toUpdate = toUpdate;
		this.allColumns = allColumns;
	}
	
	public String getName() {
		return txtName.getText();
	}
	
	public List<IOverviewTableColumn> getAllColumns(){
		return this.allColumns;
	}
	/**
	 * 
	 * @return the new column created; this will return null if we are updating a column
	 */
	public IOverviewTableColumn getNewColumn() {
		return newColumn;
		
	}
	
	public void enableOk(boolean enabled) {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) ok.setEnabled(enabled);
	}
	
	public boolean validate() {
		if (txtName.getText().trim().isEmpty()) {
			setErrorMessage(Messages.CategoryColumnDialog_NameRequired);
			enableOk(false);
			return false;
		}
		
		Control currentPart = getCurrentPanel();
		if (currentPart == null) {
			enableOk(false);
			return false;
		}
		if (currentPart == categoryOption) {
			if (!categoryOption.validate()) {
				enableOk(false);
				return false;
			}
				
		}
		if (currentPart == combinedOption) {
			if (!combinedOption.validate()) {
				enableOk(false);
				return false;
			}
		}
		
		enableOk(true);
		setErrorMessage(null);
		return true;
	}
	
	private Control getCurrentPanel() {
		if (stackPanel == null) return null;
		return ((StackLayout)stackPanel.getLayout()).topControl;
	}
	@Override
	public void cancelPressed() {
		Control currentPart = getCurrentPanel();
		if (currentPart == categoryOption) categoryOption.cancelPressed();
		if (currentPart == combinedOption) combinedOption.cancelPressed();
		super.cancelPressed();
	}
	
	@Override
	public void okPressed() {
		if (!validate()) return;
		
		Control currentPart = getCurrentPanel();
		if (currentPart == categoryOption) {
			categoryOption.okPressed();
			newColumn = categoryOption.getNewColumn();
		}
		if (currentPart == combinedOption) {
			combinedOption.okPressed();
			newColumn = combinedOption.getNewColumn();
		}

		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
	
		Label l = new Label(parent, SWT.NONE);
		l.setText(Messages.CategoryColumnDialog_NameLabel);
		
		txtName = new Text(parent, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (toUpdate != null) txtName.setText(toUpdate.getName());
		txtName.addListener(SWT.Modify, e->validate());
		
		l = new Label(parent, SWT.NONE);
		l.setText(Messages.CategoryColumnDialog_TypeLabel);
		
		ComboViewer cmbViewer = null;
		if (toUpdate == null) {
			cmbViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					return ((Type)element).name;
				}
			});
			cmbViewer.setInput(Type.values());
		}else {
			l = new Label(parent, SWT.NONE);
			if (toUpdate instanceof CategoryOverviewColumn) {
				l.setText(Type.CATEGORY.name);
			}else if (toUpdate instanceof CombinedOverviewColumn) {
				l.setText(Type.COMBINED.name);
			}
		}
		
		l = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		stackPanel = new Composite(parent, SWT.NONE);
		stackPanel.setLayout(new StackLayout());
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		categoryOption = new CategoryColumnComposite(stackPanel, this, toUpdate instanceof CategoryOverviewColumn ? toUpdate : null);
		combinedOption = new CombinedColumnComposite(stackPanel, this, toUpdate instanceof CombinedOverviewColumn ? toUpdate : null);
		
		((StackLayout)stackPanel.getLayout()).topControl = categoryOption;
		
		if (cmbViewer != null) {
			cmbViewer.setSelection(new StructuredSelection(Type.CATEGORY));
			final ComboViewer fViewer =cmbViewer;
			cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (fViewer.getStructuredSelection().getFirstElement() == Type.CATEGORY) {
						((StackLayout)stackPanel.getLayout()).topControl = categoryOption;
						stackPanel.layout();
					}else {
						if (fViewer.getStructuredSelection().getFirstElement() == Type.COMBINED) {
							((StackLayout)stackPanel.getLayout()).topControl = combinedOption;
							stackPanel.layout();
						}
					}
					
				}
			});
		}else {
			if (toUpdate instanceof CategoryOverviewColumn) {
				((StackLayout)stackPanel.getLayout()).topControl = categoryOption;
			}else if (toUpdate instanceof CombinedOverviewColumn) {
				((StackLayout)stackPanel.getLayout()).topControl = combinedOption;
			}
			stackPanel.layout();
			validate();
		}
				
		setTitle(Messages.CategoryColumnDialog_Title);
		getShell().setText(Messages.CategoryColumnDialog_Title2);
		setMessage(Messages.CategoryColumnDialog_Message);
		return parent;
	}
	

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
		
	@Override
	public boolean isResizable(){
		return true;
	}
	
}
