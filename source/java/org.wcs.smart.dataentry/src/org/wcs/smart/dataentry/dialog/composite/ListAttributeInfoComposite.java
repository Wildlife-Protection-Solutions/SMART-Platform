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
package org.wcs.smart.dataentry.dialog.composite;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.dialog.RenameListDialog;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Info composite for {@link CmAttribute} of list type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ListAttributeInfoComposite extends CmAttributeInfoComposite {

	private Label lblMulti;
	private Button btnMulti;
	
	private ComboViewer defaultViewer;
	
	private TableViewer listViewer;
	
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public ListAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
	}

	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createMultiselectControl(container);
		createDefaultControl(container);
		createListControl(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject) {
				updateMultiselectControl();
				updateDefaultControl();
				updateListControl();
			}
		});
	}

	private void createMultiselectControl(Composite parent) {
		lblMulti = new Label(parent, SWT.NONE);
		lblMulti.setText(Messages.CmAttributeInfoComposite_Option_Multiselect);
		btnMulti = new Button(parent, SWT.CHECK);
		btnMulti.setText(Messages.CmAttributeInfoComposite_Option_Multi_Checkbox);
		btnMulti.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT).setBooleanValue(btnMulti.getSelection());
				fireModelChanged();
			}
		});
	}

	private void updateMultiselectControl() {
		CmAttribute cmAttr = getSourceObject();
		boolean isEnabled = cmAttr.getOrder() == 0; //must be the first element for this option to be enabled
		CmAttributeOption option = cmAttr.getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
		btnMulti.setVisible(option != null);
		lblMulti.setVisible(option != null);
		btnMulti.setEnabled(isEnabled);
		if (option != null && isEnabled) {
			btnMulti.setSelection(option.getBooleanValue());
		} else {
			btnMulti.setSelection(false);
		}
	}
	
	private void createDefaultControl(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_DefaultValue);
		
		defaultViewer = new ComboViewer(parent, SWT.READ_ONLY);
		defaultViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		defaultViewer.setContentProvider(ArrayContentProvider.getInstance());
		defaultViewer.setLabelProvider(new NamedItemLabelProvider());
		defaultViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) defaultViewer.getSelection();
				Object obj = selection.getFirstElement();
				if (obj instanceof UuidItem) {
					UuidItem i = (UuidItem) obj;
					CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
					option.setUuidValue(i.getUuid());
					fireModelChanged();
				}
			}
		});
	}
	
	private void updateDefaultControl() {
		List<AttributeListItem> input = getSourceObject().getAttribute().getActiveListItems();
		defaultViewer.setInput(input);
		CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
		if (option.getUuidValue() != null) {
			for (AttributeListItem item : input) {
				if (Arrays.equals(item.getUuid(), option.getUuidValue())) {
					defaultViewer.setSelection(new StructuredSelection(item));
				}
			}
		}
	}	
	
	private void createListControl(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.ListAttributeInfoComposite_Values);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		listViewer = new TableViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		listViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		listViewer.setLabelProvider(new CmListItemLabelProvider(getSession()));
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	//	listViewer.getControl().setEnabled(false);

		new Label(parent, SWT.NONE);
		
		Button btnEdit = new Button(parent, SWT.PUSH);
		btnEdit.setText(Messages.ListAttributeInfoComposite_Button_Edit);
		setButtonLayoutData(btnEdit);
		((GridData)btnEdit.getLayoutData()).horizontalAlignment = SWT.RIGHT;
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog.openConfirm(getShell(), Messages.ListAttributeInfoComposite_WarnDialogTitle, Messages.ListAttributeInfoComposite_WarnDialogMessage)){
					return;
				}
				
				RenameListDialog dialog = new RenameListDialog(getShell(), getSourceObject().getAttribute(), getSourceObject().getNode().getModel(),getSession());
				dialog.open();
						
				updateListControl();
				listViewer.refresh();
				fireModelChanged();
			}
		});
	}

	private void updateListControl() {
		listViewer.setInput(getSourceObject().getAttribute().getActiveListItems());
	}	
}
