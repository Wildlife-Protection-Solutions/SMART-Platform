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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.CmCustomTreesUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.dialog.CmAttributeTreeContentProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.EditTreeDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.TreeEditorField;

/**
 * Info composite for {@link CmAttribute} of tree type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TreeAttributeInfoComposite extends CmAttributeInfoComposite {

	private TreeEditorField defaultValueTreeField;
	private TreeViewer attributeTreeViewer;
	private Button btnIsCustomConfig;
	
	private Object lastSelection;

	private ConfigurableModelEditDialog dialog;

	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public TreeAttributeInfoComposite(Composite parent, ConfigurableModelEditDialog dialog) {
		super(parent, dialog.getModel());
		this.dialog = dialog;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createBooleanControl(container, CmAttributeOption.ID_FLATTEN_TREE, 
				Messages.CmAttributeInfoComposite_Option_FlattenTree, "", Messages.TreeAttributeInfoComposite_listOpTooltip); //$NON-NLS-1$
		createDefaultControl(container);
		
		Label lblValues = new Label(container, SWT.NONE);
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		lblValues.setText(Messages.TreeAttributeInfoComposite_valueLabel);
		
		createIsCustomConfigControl(container);
		createTreeControl(container);
		createEditTreeButton(container);
		createRevertToDmButton(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				//default value field
				defaultValueTreeField.setAttribute(getSourceObject().getAttribute());
				defaultValueTreeField.clear();
				defaultValueTreeField.setLanguage(language);
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
				if (option != null && option.getUuidValue() != null){
					Session s = HibernateManager.openSession();
					try{
						AttributeTreeNode defaultNode = (AttributeTreeNode) s.load(AttributeTreeNode.class, option.getUuidValue());
						defaultValueTreeField.setValue(defaultNode);
					}finally{
						s.close();
					}
				}
				
				CmTreeLabelProvider cmTreeLabelProvider = (CmTreeLabelProvider)attributeTreeViewer.getLabelProvider();
				cmTreeLabelProvider.setLanguage(language);
				if (getSourceObject() != lastSelection){
					//tree viewer
					preLoadTree(getSourceObject());
					attributeTreeViewer.setInput(getSourceObject());
					attributeTreeViewer.expandToLevel(2);
				}
				attributeTreeViewer.refresh(true);
				
				btnIsCustomConfig.setSelection(getSourceObject().isUseCustomConfig());
				
				lastSelection = getSourceObject();
			}
		});
	}

	private void createIsCustomConfigControl(Composite parent) {
		btnIsCustomConfig = new Button(parent, SWT.CHECK);
		btnIsCustomConfig.setText(Messages.TreeAttributeInfoComposite_UseCustomConfiguration);
		btnIsCustomConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnIsCustomConfig.setToolTipText(Messages.TreeAttributeInfoComposite_customTreeOp);
		
		btnIsCustomConfig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!btnIsCustomConfig.getSelection() && !MessageDialog.openQuestion(getShell(), Messages.TreeAttributeInfoComposite_UseDefaultWarning_Title, Messages.TreeAttributeInfoComposite_UseDefaultWarning_Message)) {
					btnIsCustomConfig.setSelection(true);
					return;
				}
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_CUSTOM_CONFIG);
				if (option == null) {
					option = CmAttributeOptionFactory.createCustomCofigOption(getSourceObject());
					getSourceObject().getCmAttributeOptions().put(option.getOptionId(), option);
				}
				option.setBooleanValue(btnIsCustomConfig.getSelection());
				
				if (!btnIsCustomConfig.getSelection()) {
					//we need to remove any configuration created 
					cleanupCustomTree(getSourceObject());
				}
				
				attributeTreeViewer.setInput(getSourceObject());
				attributeTreeViewer.expandToLevel(2);
				attributeTreeViewer.refresh();
				fireModelChanged();
			}
		});
	}

	private void preLoadTree(final CmAttribute cmAttribute) {
		final ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(getShell());
		try {
			pmdDialog.run(true, false, new TreePreLoadRunnable(cmAttribute));
		} catch (InvocationTargetException | InterruptedException e) {
			SmartPlugIn.displayLog(Messages.TreeAttributeInfoComposite_TreePreLoad_Error + e.getLocalizedMessage(), e);
		}
	}

	private void cleanupCustomTree(final CmAttribute cmAttribute) {
		final ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(getShell());
		try {
			pmdDialog.run(true, false, new TreeCustomConfigCleanupRunnable(cmAttribute));
		} catch (InvocationTargetException | InterruptedException e) {
			SmartPlugIn.displayLog(Messages.TreeAttributeInfoComposite_Cleanup_Error + e.getLocalizedMessage(), e);
		}
	}
	
	private void createDefaultControl(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_DefaultValue);
		label.setToolTipText(Messages.TreeAttributeInfoComposite_deafultValueTooltip);
		
		defaultValueTreeField = new TreeEditorField(){
			public void createComposite(Composite parent) {
				super.createComposite(parent);
				
				txtText.addFocusListener(new FocusListener() {
					@Override
					public void focusLost(FocusEvent e) {
						defaultTreeFieldChanged();
					}
					@Override
					public void focusGained(FocusEvent e) {
					}
				});
			}
		};
		
		defaultValueTreeField.createComposite(container);
		
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				defaultValueTreeField.dispose();
			}
		});
		
		defaultValueTreeField.addSelectionChangedListener(new Listener(){
			@Override
			public void handleEvent(Event event) {
				defaultTreeFieldChanged();
			}
		});
	}

	private void defaultTreeFieldChanged(){
		AttributeTreeNode node = defaultValueTreeField.getValue();
		CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
		if (node != null){
			if (option == null){
				option = CmAttributeOptionFactory.createDefaultValueOption(getSourceObject());
				getSourceObject().getCmAttributeOptions().put(option.getOptionId(), option);
			}
			option.setUuidValue(node.getUuid());
		}else{
			if (option != null){
				getSourceObject().getCmAttributeOptions().remove(option.getOptionId());
				option.setCmAttribute(null);
			}
			defaultValueTreeField.setValue(null);
		}
		fireModelChanged();
	}

	private void createTreeControl(Composite container) {
		attributeTreeViewer = new TreeViewer(container);
		attributeTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		attributeTreeViewer.setLabelProvider(new CmTreeLabelProvider(getModel()));
		attributeTreeViewer.setContentProvider(new CmAttributeTreeContentProvider(false, false));
	}
	
	private void createEditTreeButton(Composite container) {
		Button btnRename = new Button(container, SWT.PUSH);
		btnRename.setText(Messages.TreeAttributeInfoComposite_RenameButton);
		btnRename.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));

		btnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getSourceObject().isUseCustomConfig() || MessageDialog.openConfirm(getShell(), Messages.TreeAttributeInfoComposite_WarnTitle, Messages.TreeAttributeInfoComposite_WarnMessage1)) {
					EditTreeDialog dlg = new EditTreeDialog(getShell(), getSourceObject(), getModel(), dialog.getSession());
					dlg.open();
					
					attributeTreeViewer.refresh();
					fireModelChanged();
				}
			}
		});
	}

	private void createRevertToDmButton(Composite container) {
		Button btnRename = new Button(container, SWT.PUSH);
		btnRename.setText(Messages.TreeAttributeInfoComposite_Button_Revert);
		btnRename.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));

		btnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String msg = getSourceObject().isUseCustomConfig() ? Messages.TreeAttributeInfoComposite_ConfirmRevert_Custom_Message : Messages.TreeAttributeInfoComposite_ConfirmRevert_Default_Message;
				if (MessageDialog.openConfirm(getShell(), Messages.TreeAttributeInfoComposite_ConfirmRevert_Title, msg)) {
					CmAttribute cmAttr = getSourceObject();
					List<CmAttributeTreeNode> newList = getSourceObject().isUseCustomConfig() ? 
							CmCustomTreesUtil.buildCustomTree(getModel(), cmAttr, cmAttr.getAttribute()) : CmDefaultTreesUtil.buildDefaultTree(getModel(), cmAttr.getAttribute());
								
					List<CmAttributeTreeNode> currentTree = cmAttr.getCurrentTree();
					currentTree.clear();
					currentTree.addAll(newList);
					
					attributeTreeViewer.refresh();
					fireModelChanged();
				}
			}
		});
	}

	/**
	 * 	Load lazy tree data
	 * 
	 * @author elitvin
	 * @since 3.2.0
	 */
	protected class TreePreLoadRunnable implements IRunnableWithProgress {
		private final CmAttribute cmAttribute;

		protected TreePreLoadRunnable(CmAttribute cmAttribute) {
			this.cmAttribute = cmAttribute;
		}
		
		public CmAttribute getCmAttribute() {
			return cmAttribute;
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			//we need to load two levels
			monitor.setTaskName(Messages.TreeAttributeInfoComposite_TreePreLoad_FirstLevel);
			monitor.beginTask(Messages.TreeAttributeInfoComposite_TreePreLoad_SecondLevel, cmAttribute.getCurrentTree().size());
			for (CmAttributeTreeNode treeNode : cmAttribute.getCurrentTree()) {
				treeNode.getChildren().size();
				monitor.worked(1);
			}
			monitor.done();
		}
	}

	/**
	 * 	Erases current custom tree configuration and load current lazy tree data
	 *  
	 * @author elitvin
	 * @since 3.2.0
	 */
	protected class TreeCustomConfigCleanupRunnable extends TreePreLoadRunnable {
		

		protected TreeCustomConfigCleanupRunnable(CmAttribute cmAttribute ) {
			super(cmAttribute);
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.TreeAttributeInfoComposite_Cleanup_Task, getCmAttribute().getTree().size()+1);
			for (CmAttributeTreeNode toDelete : getCmAttribute().getTree()){
				toDelete.setAttribute(null);
				monitor.worked(1);
			}
			getCmAttribute().getTree().clear();
			monitor.worked(1);
			super.run(monitor);
		}
	}
}
