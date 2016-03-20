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

import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ChangeTracker;
import org.wcs.smart.dataentry.dialog.EditListDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Info composite for {@link CmAttribute} of list type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ListAttributeInfoComposite extends CmAttributeInfoComposite {

	private final static String NO_OPTION = ""; //$NON-NLS-1$
	private Label lblMulti;
	private Button btnMulti;
	
	private ComboViewer defaultViewer;
	private Button btnIsCustomConfig;
	
	private TableViewer listViewer;
	private Language currentLanguage;

	private boolean initializingControl = false;
	private Object lastSelection = null;
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public ListAttributeInfoComposite(Composite parent, ConfigurableModel model, ChangeTracker tracker) {
		super(parent, model, tracker);
	}

	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createMultiselectControl(container);
		createDefaultControl(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.ListAttributeInfoComposite_Values);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));
		createIsCustomConfigControl(container);
		createListControl(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				currentLanguage = language;
				updateMultiselectControl();
				updateDefaultControl();
				if (getSourceObject() != lastSelection){
					updateListControl();
				}
				btnIsCustomConfig.setSelection(getSourceObject().isUseCustomConfig());
				lastSelection = getSourceObject();
				ListAttributeInfoComposite.this.layout(true, true);
			}
		});
	}

	private void createMultiselectControl(Composite parent) {
		lblMulti = new Label(parent, SWT.NONE);
		lblMulti.setText(Messages.CmAttributeInfoComposite_Option_Multiselect);
		lblMulti.setToolTipText(Messages.ListAttributeInfoComposite_multiSelectTooltip);
		btnMulti = new Button(parent, SWT.CHECK);
		btnMulti.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT).setBooleanValue(btnMulti.getSelection());
				tracker.saveOrUpdate(getSourceObject());
				fireModelChanged();
			}
		});
	}

	private void updateMultiselectControl() {
		CmAttribute cmAttr = getSourceObject();
		String disabledText = getMulultiSelectDisableText(cmAttr);
		boolean isEnabled = disabledText.isEmpty();
		CmAttributeOption option = cmAttr.getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
		lblMulti.setVisible(option != null);
		btnMulti.setVisible(option != null);
		btnMulti.setEnabled(isEnabled);
		btnMulti.setText(disabledText);
		if (option != null && isEnabled) {
			btnMulti.setSelection(option.getBooleanValue());
		} else {
			btnMulti.setSelection(false);
		}
	}

	private String getMulultiSelectDisableText(CmAttribute cmAttr) {
		if (cmAttr.getNode().isCollectMultipleObservations()) {
			return Messages.CmAttributeInfoComposite_NotAllowedInMultiObservationMode;
		}
		CmAttribute cmMultiAttr = getMulultiSelectedAttr(cmAttr.getNode());
		if (cmMultiAttr != null && !cmAttr.equals(cmMultiAttr)) {
			return MessageFormat.format(Messages.ListAttributeInfoComposite_MultiselectHint, cmMultiAttr.findNameNull(currentLanguage));
		}
		return ""; //$NON-NLS-1$
	}
	
	private CmAttribute getMulultiSelectedAttr(CmNode cmNode) {
		for (CmAttribute cmAttr : cmNode.getCmAttributes()) {
			CmAttributeOption option = cmAttr.getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
			if (option != null && Boolean.TRUE.equals(option.getBooleanValue())) {
				return cmAttr;
			}
		}
		return null;
	}
	
	private void createDefaultControl(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_DefaultValue);
		label.setToolTipText(Messages.ListAttributeInfoComposite_defaultTooltip);
		
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
					if (option == null){
						option = CmAttributeOptionFactory.createDefaultValueOption(getSourceObject());
						getSourceObject().getCmAttributeOptions().put(option.getOptionId(), option);
					}
					option.setUuidValue(i.getUuid());
					
				}else{
					CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
					if (option != null){
						getSourceObject().getCmAttributeOptions().remove(option.getOptionId());
						option.setCmAttribute(null);
					}
				}
				if (!initializingControl){
					tracker.saveOrUpdate(getSourceObject());
					fireModelChanged();
				}
			}
		});
	}
	
	private void updateDefaultControl() {
		initializingControl = true;
		try {
			((NamedItemLabelProvider) defaultViewer.getLabelProvider()).setLanguage(currentLanguage);
			List<Object> input = new ArrayList<Object>();
			input.add(NO_OPTION);
			input.addAll(getSourceObject().getAttribute().getActiveListItems());
			defaultViewer.setInput(input);
			CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
			if (option != null && option.getUuidValue() != null) {
				for (Object item : input) {
					if (item instanceof AttributeListItem
							&& ((AttributeListItem) item).getUuid().equals(option.getUuidValue())) {
						defaultViewer.setSelection(new StructuredSelection(item));
					}
				}
			} else {
				defaultViewer.setSelection(new StructuredSelection(NO_OPTION));
			}
		} finally {
			initializingControl = false;
		}
	}	

	private void createIsCustomConfigControl(Composite parent) {
		btnIsCustomConfig = new Button(parent, SWT.CHECK);
		btnIsCustomConfig.setText(Messages.ListAttributeInfoComposite_UseCustomConfiguration);
		btnIsCustomConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnIsCustomConfig.setToolTipText(Messages.ListAttributeInfoComposite_UseCustomConfigurationTooltip);
		
		btnIsCustomConfig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!btnIsCustomConfig.getSelection() && !MessageDialog.openQuestion(getShell(), Messages.ListAttributeInfoComposite_UseDefaultWarning_Title, Messages.ListAttributeInfoComposite_UseDefaultWarning_Message)) {
					btnIsCustomConfig.setSelection(true);
					return;
				}
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_CUSTOM_CONFIG);
				if (option == null) {
					option = CmAttributeOptionFactory.createCustomCofigOption(getSourceObject());
					getSourceObject().getCmAttributeOptions().put(option.getOptionId(), option);
					tracker.saveOrUpdate(getSourceObject());
				}
				option.setBooleanValue(btnIsCustomConfig.getSelection());
				
				//we need to remove any configuration created 
				clearCustomListConfiguration(getSourceObject());
				if (btnIsCustomConfig.getSelection()){
					//we need to create custom configuration
					getSourceObject().getList().addAll(CmDefaultListsUtil.buildCustomList(getModel(), getSourceObject()));
				}
				
				listViewer.setInput(getSourceObject().getCurrentList());
				listViewer.refresh();
				tracker.saveOrUpdate(getSourceObject());
				fireModelChanged();
			}
		});
	}

	private void clearCustomListConfiguration(CmAttribute a) {
		for (CmAttributeListItem toDelete : a.getList()){
			toDelete.setAttribute(null);
			tracker.deleteObject(toDelete);
		}
		getSourceObject().getList().clear();
	}
	
	private void createListControl(Composite parent) {
		listViewer = new TableViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		listViewer.setLabelProvider(new CmListItemLabelProvider(getModel()));
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		new Label(parent, SWT.NONE);
		
		Button btnEdit = new Button(parent, SWT.PUSH);
		btnEdit.setText(Messages.ListAttributeInfoComposite_Button_Edit);
		btnEdit.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getSourceObject().isUseCustomConfig() || MessageDialog.openConfirm(getShell(), Messages.ListAttributeInfoComposite_WarnDialogTitle, Messages.ListAttributeInfoComposite_WarnDialogMessage)){
					EditListDialog dialog = new EditListDialog(getShell(), getSourceObject(), getModel(), tracker);
					dialog.open();
							
					updateListControl();
					listViewer.refresh();
					tracker.saveOrUpdate(getSourceObject());
					fireModelChanged();
				}
			}
		});
	}

	private void updateListControl() {
		((CmListItemLabelProvider)listViewer.getLabelProvider()).setLanguage(currentLanguage);
		listViewer.setInput(getSourceObject().getCurrentList());
	}	
}
