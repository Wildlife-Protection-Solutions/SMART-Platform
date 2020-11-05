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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.CmAttributeConfigUtil;
import org.wcs.smart.dataentry.CmCustomListsUtil;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.EditListDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Info composite for {@link CmAttribute} of list type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ListAttributeInfoComposite extends CmAttributeConfInfoComposite {

	private final static String NO_OPTION = ""; //$NON-NLS-1$
	private Label lblMulti;
	private Button btnMulti;
	
	private ComboViewer defaultViewer;
	
	private TableViewer listViewer;
	private Language currentLanguage;

	private boolean initializingControl = false;
	private Object lastSelection = null;
	
	private ConfigurableModelEditDialog dialog;
	
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public ListAttributeInfoComposite(Composite parent, ConfigurableModelEditDialog dialog, List<CmAttributeConfig> deletedConfigs, List<CmAttributeConfig> addedConfigs) {
		super(parent, dialog, deletedConfigs, addedConfigs);
		this.dialog = dialog;
	}

	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createMultiselectControl(container);
		createDefaultControl(container);
		
		createConfigSelectionControl(container);

		Composite part = new Composite(container, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		createListControl(part);
		
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				currentLanguage = language;
				updateMultiselectControl();
				updateDefaultControl();
				if (getSourceObject() != lastSelection){
					updateListControl();
				}
				tiDeleteConfig.setEnabled(!getSourceObject().getConfig().isDefault());
				lastSelection = getSourceObject();
				ListAttributeInfoComposite.this.layout(true, true);
			}
		});
	}

	private void createMultiselectControl(Composite parent) {
		lblMulti = new Label(parent, SWT.NONE);
		lblMulti.setText(Messages.CmAttributeInfoComposite_Option_Multiselect1);
		lblMulti.setToolTipText(Messages.ListAttributeInfoComposite_multiSelectTooltip1);
		btnMulti = new Button(parent, SWT.CHECK);
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
		String disabledText = getMulultiSelectDisableText(cmAttr);
		boolean isEnabled = disabledText.isEmpty();
		CmAttributeOption option = cmAttr.getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
		lblMulti.setVisible(option != null);
		btnMulti.setVisible(option != null);
		btnMulti.setEnabled(isEnabled);
		btnMulti.setText(disabledText);
		if (option != null ) {
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
					}
				}
				if (!initializingControl){
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

	@Override
	protected void handleConfigViewerSelectionChanged() {
		tiDeleteConfig.setEnabled(!getSourceObject().getConfig().isDefault());
		listViewer.setInput(getSourceObject().getCurrentList());
		listViewer.refresh();
	}

	private void createListControl(Composite parent) {
		listViewer = new TableViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		listViewer.setLabelProvider(new CmListItemLabelProvider());
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	@Override
	protected void addConfiguration() {
		CmAttributeConfig cfg = CmAttributeConfig.createConfig(getModel(), getSourceObject().getAttribute(), false);
		CmAttributeConfigUtil.assignCustomName(cfg, getSourceObject());
		cfg.setList(CmCustomListsUtil.buildCustomList(cfg, getSourceObject().getAttribute()));
		getSourceObject().setConfig(cfg);
		
		EditListDialog dlg = new EditListDialog(getShell(), getSourceObject(), dialog.getSession());
		dlg.open();
		
		applyNewConig(cfg);
		
		fireModelChanged();
	}
	
	@Override
	protected void editConfiguration() {
		EditListDialog dlg = new EditListDialog(getShell(), getSourceObject(), dialog.getSession());
		dlg.open();

		refreshConfigViewer();
		updateListControl();
		listViewer.refresh();
		fireModelChanged();
	}
	
	@Override
	protected void deleteConfiguration() {
		removeConfig(getSourceObject().getConfig());
	}

	@Override
	protected void revertConfiguration() {
		CmAttribute cmAttr = getSourceObject();
		if (MessageDialog.openConfirm(getShell(), Messages.ListAttributeInfoComposite_ConfirmRevert_Title, Messages.ListAttributeInfoComposite_ConfirmRevert_Message)) {
			CmAttributeConfig cfg = cmAttr.getConfig();
			List<CmAttributeListItem> newList = cfg.isDefault() ? CmDefaultListsUtil.buildDefaultList(cfg, cmAttr.getAttribute()) : CmCustomListsUtil.buildCustomList(cfg, cmAttr.getAttribute());
			cfg.getList().clear();
			cfg.getList().addAll(newList);
			
			updateListControl();
			listViewer.refresh();
			fireModelChanged();
		}
	}
	
	@Override
	public void setSourceObject(CmAttribute attribute, Language language) {
		super.setSourceObject(attribute, language);
		this.listViewer.refresh();
	}
	
	private void updateListControl() {
		((CmListItemLabelProvider)listViewer.getLabelProvider()).setLanguage(currentLanguage);
		listViewer.setInput(getSourceObject().getCurrentList());
	}	
}
