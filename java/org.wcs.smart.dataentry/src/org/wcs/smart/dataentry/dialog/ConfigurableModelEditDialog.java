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
package org.wcs.smart.dataentry.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryPlugIn;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for editing Configurable Models.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ConfigurableModelEditDialog extends AbstractPropertyJHeaderDialog {
	
	private static final int DIALOG_WIDTH = 700;
	private static final int DIALOG_HEIGHT = 725;

	private ConfigurableModel model;
	
	public ConfigurableModelEditDialog(ConfigurableModel model) {
		super(Display.getDefault().getActiveShell(), Messages.ConfigurableModelEditDialog_Title);
		this.model = model;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	protected Composite createContent(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout(1, false);
		mainLayout.marginBottom=0;
		mainLayout.marginHeight = 0;
		mainLayout.marginLeft = 0;
		mainLayout.marginRight = 0;
		mainLayout.marginTop = 0;
		mainLayout.marginWidth = 0;
		main.setLayout(mainLayout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setChangesMade(model.getUuid() == null);
		
		List<IConfigurableModelEditorTabContent> extraTabs = getExtraTabs();
		
		ConfigurableModelEditorDefaultTab defaultTab = new ConfigurableModelEditorDefaultTab(this);
		if (!extraTabs.isEmpty()) {
			//we have some extra tabs and need to create tab panel
			final TabFolder tabFolder = new TabFolder (main, SWT.BORDER);
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			extraTabs.add(0, defaultTab);
			for (IConfigurableModelEditorTabContent tabContent : extraTabs) {
				TabItem tabItem = new TabItem (tabFolder, SWT.NONE);
				tabItem.setText(tabContent.getTabName());

				Composite tabContainer = new Composite(tabFolder, SWT.NONE);
				tabContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				tabContainer.setLayout(new GridLayout(1, false));
				
				tabItem.setControl(tabContainer);
				
				tabContent.createTabContent(tabContainer);
			}
		} else {
			//no extra tabs create old fashioned content without tabs
			defaultTab.createTabContent(main);
		}
		
		setTitle(Messages.ConfigurableModelEditDialog_Title);
		setMessage(Messages.ConfigurableModelEditDialog_Message);
		
		return main;

	}
	
	private List<IConfigurableModelEditorTabContent> getExtraTabs() {
		List<IConfigurableModelEditorTabContent> extraTabs = new ArrayList<IConfigurableModelEditorTabContent>();
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] tabElement = Platform.getExtensionRegistry().getConfigurationElementsFor(DataentryPlugIn.CM_EXTRADATA_EXTENSION_ID);
			try {
				for (IConfigurationElement e : tabElement) {
					IConfigurableModelEditorTabContent content = (IConfigurableModelEditorTabContent) e.createExecutableExtension("content"); //$NON-NLS-1$
					content.setDialog(this); //inject dialog dependency
					extraTabs.add(content);
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.ConfigurableModelEditDialog_LoadExtensionError, ex);
			}
		}
		return extraTabs;
	}
	
	@Override
	public void setButtonLayoutData(Button button) {
		//this is just a visibility change
		super.setButtonLayoutData(button);
	}

	public void notifyChangesMade() {
		setChangesMade(true);
	}
	
	public ConfigurableModel getModel() {
		return model;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(this.changesMade); //this will enable "Save" button when new model is just created
	}
	
	@Override
	protected boolean performSave() {

		try{
			//commit transaction
			session.saveOrUpdate(model);
			session.getTransaction().commit();
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.ConfigurableModelEditDialog_SaveError  + ex.getMessage(), ex);
		}
		
		//start a new transaction
		session.getTransaction().begin();
		setChangesMade(false);
		return true;
	}
	
}
