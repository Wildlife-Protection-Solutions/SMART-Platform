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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmNode;
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
	private Map<Attribute, List<CmAttributeConfig>> configsMap = new HashMap<>();

	private ConfigurableModelEditDialog dialog;
	
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public CmAttributeConfInfoComposite(Composite parent, ConfigurableModelEditDialog dialog) {
		super(parent, dialog.getModel());
		this.dialog = dialog;
	}
	
	public ConfigurableModelEditDialog getDialog() {
		return dialog;
	}
	
	private List<CmAttributeConfig> getConfigs(CmAttribute cmAttr) {
		List<CmAttributeConfig> cfgList = configsMap.get(cmAttr.getAttribute());
		if (cfgList == null) {
			cfgList = new ArrayList<>(DataentryHibernateManager.getCmAttributeConfigs(dialog.getSession(), cmAttr));
			cfgList.addAll(getUnsavedConfigs(cmAttr.getAttribute()));
			CmAttributeConfig defaultCfg = dialog.getModel().getDefaultConfigs().get(cmAttr.getAttribute());
			if (defaultCfg != null && !cfgList.contains(defaultCfg)) {
				cfgList.add(defaultCfg);
			}
			cfgList.sort(new CmAttributeConfigComparator());
			configsMap.put(cmAttr.getAttribute(), cfgList);
		}
		return cfgList;
	}

	private Set<CmAttributeConfig> getUnsavedConfigs(Attribute attribute) {
		Set<CmAttributeConfig> result = new HashSet<>();
		for (CmNode cmNode : getModel().getNodes()) {
			result.addAll(getUnsavedConfigs(cmNode, attribute));
		}
		return result;
	}
	
	private Set<CmAttributeConfig> getUnsavedConfigs(CmNode cmNode, Attribute attribute) {
		Set<CmAttributeConfig> result = new HashSet<>();
		for (CmAttribute cmAttr : cmNode.getCmAttributes()) {
			if (attribute.equals(cmAttr.getAttribute())) {
				CmAttributeConfig cfg = cmAttr.getConfig();
				if (cfg != null && cfg.getUuid() == null) {
					result.add(cfg);
				}
			}
		}
		for (CmNode childNode : cmNode.getChildren()) {
			result.addAll(getUnsavedConfigs(childNode, attribute));
		}
		return result;
	}
	
	protected void createConfigSelectionControl(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeConfInfoComposite_Configuration_Label);
		label.setToolTipText(Messages.CmAttributeConfInfoComposite_Configuration_Tooltip);
		
		configViewer = new ComboViewer(container, SWT.READ_ONLY);
		configViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
		List<CmAttributeConfig> cfgList = getConfigs(getSourceObject());
		cfgList.add(cfg);
		cfgList.sort(new CmAttributeConfigComparator());
		configViewer.setInput(cfgList);
		configViewer.setSelection(new StructuredSelection(cfg));
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
		
		List<CmAttributeConfig> cfgList = getConfigs(getSourceObject());
		cfgList.remove(config);
		configViewer.setInput(cfgList);
		configViewer.setSelection(new StructuredSelection(getModel().getDefaultConfigs().get(config.getAttribute())));
		dialog.getSession().delete(config);
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
