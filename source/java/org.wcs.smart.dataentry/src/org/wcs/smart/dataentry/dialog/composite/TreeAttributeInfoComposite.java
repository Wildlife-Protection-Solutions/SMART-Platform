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
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.CmAttributeConfigUtil;
import org.wcs.smart.dataentry.CmCustomTreesUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.dialog.CmAttributeTreeContentProvider;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.EditTreeDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;
import org.wcs.smart.ui.properties.TreeEditorField;

/**
 * Info composite for {@link CmAttribute} of tree type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TreeAttributeInfoComposite extends CmAttributeConfInfoComposite {

	private TreeEditorField<AttributeTreeNode> defaultValueTreeField;
	private TreeViewer attributeTreeViewer;
	
	private Object lastSelection;
	private ConfigurableModelEditDialog dialog;

	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public TreeAttributeInfoComposite(Composite parent, ConfigurableModelEditDialog dialog, List<CmAttributeConfig> deletedConfigs) {
		super(parent, dialog, deletedConfigs);
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
		
		createConfigSelectionControl(container);
		createTreeControl(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				//default value field
				defaultValueTreeField.setInput(getSourceObject().getAttribute());
				defaultValueTreeField.clear();
				((AttributeTreeLabelProvider)defaultValueTreeField.getDropDown().getTreeViewer().getLabelProvider()).setLanguage(language);
				defaultValueTreeField.getDropDown().getTreeViewer().refresh();
				
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
				if (option != null && option.getUuidValue() != null){
					AttributeTreeNode defaultNode = (AttributeTreeNode) dialog.getSession().load(AttributeTreeNode.class, option.getUuidValue());
					defaultValueTreeField.setSelectedValue(defaultNode);
				}
				
				CmTreeLabelProvider cmTreeLabelProvider = (CmTreeLabelProvider)attributeTreeViewer.getLabelProvider();
				cmTreeLabelProvider.setLanguage(language);
				if (getSourceObject() != lastSelection){
					//tree viewer
					preLoadTree(getSourceObject());
					attributeTreeViewer.setInput(getSourceObject());
					attributeTreeViewer.expandToLevel(2);
				}
				tiDeleteConfig.setEnabled(!getSourceObject().getConfig().isDefault());
				attributeTreeViewer.refresh(true);
				
				lastSelection = getSourceObject();
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

	private void createDefaultControl(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_DefaultValue);
		label.setToolTipText(Messages.TreeAttributeInfoComposite_deafultValueTooltip);
		
		defaultValueTreeField = new TreeEditorField<AttributeTreeNode>(){
			@Override
			public void createComposite(Composite parent, IContentProvider contentProvider, IBaseLabelProvider labelProvider) {
				super.createComposite(parent, contentProvider, labelProvider);
				
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
		
		defaultValueTreeField.createComposite(container, new AttributeTreeContentProvider(true, false), new AttributeTreeLabelProvider());
		
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
			}
			defaultValueTreeField.setSelectedValue(null);
		}
		fireModelChanged();
	}

	private void createTreeControl(Composite container) {
		attributeTreeViewer = new TreeViewer(container);
		attributeTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		attributeTreeViewer.setLabelProvider(new CmTreeLabelProvider());
		attributeTreeViewer.setContentProvider(new CmAttributeTreeContentProvider(false, false));
	}


	@Override
	protected void addConfiguration() {
		CmAttributeConfig cfg = CmAttributeConfig.createConfig(getModel(), getSourceObject().getAttribute(), false);
		CmAttributeConfigUtil.assignCustomName(cfg, getSourceObject());
		getSourceObject().setConfig(cfg);

		EditTreeDialog dlg = new EditTreeDialog(getShell(), getSourceObject(), dialog.getSession());
		dlg.open();
		
		applyNewConig(cfg);
		
		fireModelChanged();
	}


	@Override
	protected void handleConfigViewerSelectionChanged() {
		tiDeleteConfig.setEnabled(!getSourceObject().getConfig().isDefault());
		attributeTreeViewer.setInput(getSourceObject());
		attributeTreeViewer.expandToLevel(2);
		attributeTreeViewer.refresh();
	}
	
	@Override
	protected void editConfiguration() {
		if (MessageDialog.openConfirm(getShell(), Messages.TreeAttributeInfoComposite_WarnTitle, Messages.TreeAttributeInfoComposite_WarnConfigMessage)) {
			EditTreeDialog dlg = new EditTreeDialog(getShell(), getSourceObject(), dialog.getSession());
			dlg.open();
			
			refreshConfigViewer();
			attributeTreeViewer.refresh();
			fireModelChanged();
		}
	}
	
	@Override
	protected void deleteConfiguration() {
		removeConfig(getSourceObject().getConfig());
	}
	
	@Override
	public void revertConfiguration() {
		if (MessageDialog.openConfirm(getShell(), Messages.TreeAttributeInfoComposite_ConfirmRevert_Title, Messages.TreeAttributeInfoComposite_ConfirmRevert_Message)) {
			CmAttribute cmAttr = getSourceObject();
			CmAttributeConfig cfg = cmAttr.getConfig();
			List<CmAttributeTreeNode> newList = cfg.isDefault() ? CmDefaultTreesUtil.buildDefaultTree(cfg, cmAttr.getAttribute()) : CmCustomTreesUtil.buildCustomTree(cfg, cmAttr.getAttribute());
			cfg.getTree().clear();
			cfg.getTree().addAll(newList);
			
			attributeTreeViewer.refresh();
			fireModelChanged();
		}
	}
	

	@Override
	public void setSourceObject(CmAttribute attribute, Language language) {
		super.setSourceObject(attribute, language);
		this.attributeTreeViewer.refresh();
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

}
