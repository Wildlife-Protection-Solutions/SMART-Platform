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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Class that contains common logic for attributes that support configuration.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public abstract class CmAttributeConfInfoComposite extends CmAttributeInfoComposite {

	private ComboViewer configViewer;
	private ConfigurableModelEditDialog dialog;
	private List<CmAttributeConfig> deletedConfigs = null;
	private List<CmAttributeConfig> addedConfigs = null;
	protected ToolItem tiDeleteConfig;
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public CmAttributeConfInfoComposite(Composite parent, ConfigurableModelEditDialog dialog, List<CmAttributeConfig> deletedConfigs, List<CmAttributeConfig> addedConfigs) {
		super(parent, dialog.getModel(), dialog.getSession());
		this.dialog = dialog;
		this.deletedConfigs = deletedConfigs;
		this.addedConfigs = addedConfigs;
	}
	
	public ConfigurableModelEditDialog getDialog() {
		return dialog;
	}
	
	private List<CmAttributeConfig> getConfigs(CmAttribute cmAttr) {
		ConfigurableModel cm = cmAttr.getNode().getModel();
		List<CmAttributeConfig> cfgList = new ArrayList<>(DataentryHibernateManager.getCmAttributeConfigs(dialog.getSession(), cm, cmAttr.getAttribute()));
		cfgList.removeAll(deletedConfigs);
		for (CmAttributeConfig c : addedConfigs) {
			if (c.getAttribute().equals(cmAttr.getAttribute()) && !cfgList.contains(c)) cfgList.add(c);
		}
		CmAttributeConfig defaultCfg = dialog.getModel().getDefaultConfigs().get(cmAttr.getAttribute());
		if (defaultCfg != null && !cfgList.contains(defaultCfg)) {
			cfgList.add(defaultCfg);
		}
		cfgList.sort(new CmAttributeConfigComparator());

		return cfgList;
	}

	protected void createConfigSelectionControl(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeConfInfoComposite_Configuration_Label);
		label.setToolTipText(Messages.CmAttributeConfInfoComposite_Configuration_Tooltip);
		
		Composite temp = new Composite(container, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		configViewer = new ComboViewer(temp, SWT.READ_ONLY);
		configViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		configViewer.setContentProvider(ArrayContentProvider.getInstance());
		configViewer.setLabelProvider(new CmAttributeConfigLabelProvider());
		configViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) configViewer.getSelection();
				Object obj = selection.getFirstElement();
				if (obj instanceof CmAttributeConfig) {
					CmAttributeConfig c = (CmAttributeConfig) obj;
					if (!c.equals(getSourceObject().getConfig())) {
						getSourceObject().setConfig(c);
						
						handleConfigViewerSelectionChanged();
						
						fireModelChanged();
					}
				}
			}
		});
		
		ToolBar tb  =  new ToolBar(temp, SWT.HORIZONTAL | SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		ToolItem newConfig = new ToolItem(tb, SWT.PUSH);
		newConfig.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		newConfig.setToolTipText(Messages.CmAttributeConfInfoComposite_createConfigTooltip);
		newConfig.addListener(SWT.Selection, e->addConfiguration());

		ToolItem editConfig = new ToolItem(tb, SWT.PUSH);
		editConfig.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editConfig.setToolTipText(Messages.CmAttributeConfInfoComposite_editConfigTooltip);
		editConfig.addListener(SWT.Selection, e->editConfiguration());
		
		tiDeleteConfig = new ToolItem(tb, SWT.PUSH);
		tiDeleteConfig.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDeleteConfig.setToolTipText(Messages.CmAttributeConfInfoComposite_deleteConfigTooltip);
		tiDeleteConfig.addListener(SWT.Selection, e->deleteConfiguration());
		
		ToolItem revertConfig = new ToolItem(tb, SWT.PUSH);
		revertConfig.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		revertConfig.setToolTipText(Messages.CmAttributeConfInfoComposite_revertConfigTooltip);
		revertConfig.addListener(SWT.Selection, e->revertConfiguration());

		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				List<CmAttributeConfig> cfgList = getConfigs(getSourceObject());
				if (configViewer.getLabelProvider() instanceof NamedItemLabelProvider) {
					((NamedItemLabelProvider)configViewer.getLabelProvider()).setLanguage(language);;
				}
				configViewer.setInput(cfgList);
				if (!cfgList.isEmpty()) {
					//default config will be at the top based on sorting logic
					//Note that config must always be set
					configViewer.setSelection(new StructuredSelection(getSourceObject().getConfig() != null ? getSourceObject().getConfig() : cfgList.get(0)));
				}
			}
		});
	}

	protected void applyNewConig(CmAttributeConfig cfg) {
		if (cfg.getUuid() == null && !addedConfigs.contains(cfg)) {
			dialog.getSession().persist(cfg);
			addedConfigs.add(cfg);
		}
		
		List<CmAttributeConfig> cfgList = getConfigs(getSourceObject());
		//cfgList.sort(new CmAttributeConfigComparator());
		configViewer.setInput(cfgList);
		configViewer.setSelection(new StructuredSelection(cfg));
		handleConfigViewerSelectionChanged();
	}
	
	protected void refreshConfigViewer() {
		configViewer.refresh(true);
	}

	protected void removeConfig(CmAttributeConfig config) {
		if (config == null || config.isDefault()) {
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.CmAttributeConfInfoComposite_DeleteConfirmation_Title, Messages.CmAttributeConfInfoComposite_DeleteConfirmation_Message)){
			return;
		}
		for (CmNode cmNode : getModel().getNodes()) {
			resetToDefaultConfig(cmNode, config);
		}
		if (config.getUuid() != null) {
			dialog.getSession().remove(config);
		}
		if (!deletedConfigs.contains(config)) deletedConfigs.add(config);
		addedConfigs.remove(config);
		
		List<CmAttributeConfig> cfgList = getConfigs(getSourceObject());
		configViewer.setInput(cfgList);
		configViewer.setSelection(new StructuredSelection(getModel().getDefaultConfigs().get(config.getAttribute())));
		
		handleConfigViewerSelectionChanged();
		fireModelChanged();
	}

	private void resetToDefaultConfig(CmNode cmNode, CmAttributeConfig config) {
		for (CmAttribute cmAttr : cmNode.getCmAttributes()) {
			if (config.equals(cmAttr.getConfig())) {
				cmAttr.setConfig(getModel().getDefaultConfigs().get(config.getAttribute()));
			}
		}
		for (CmNode childNode : cmNode.getChildren()) {
			resetToDefaultConfig(childNode, config);
		}
		
	}	
	
	protected abstract void handleConfigViewerSelectionChanged();
	
	/**
	 * Add a new configuration
	 */
	protected abstract void addConfiguration();
	/**
	 * Delete selected configuration
	 */
	protected abstract void deleteConfiguration();
	/**
	 * Edit current configuration
	 */
	protected abstract void editConfiguration();
	/**
	 * Reset current configuration
	 */
	protected abstract void revertConfiguration();

	/**
	 * 	Comparator for {@link CmAttributeConfig}
	 * 
	 * @author elitvin
	 * @since 6.0.0
	 */
	private class CmAttributeConfigComparator implements Comparator<CmAttributeConfig> {
		@Override
		public int compare(CmAttributeConfig c1, CmAttributeConfig c2) {
			if (c1 == null) return c2 == null ? 0 : -1;
			if (c2 == null) return 1;
			if (c1.isDefault() && !c2.isDefault()) return -1;
			if (!c1.isDefault() && c2.isDefault()) return 1;
			return Collator.getInstance().compare(c1.findName(SmartDB.getCurrentLanguage()), c2.findName(SmartDB.getCurrentLanguage()));
		}
	}
	
}
