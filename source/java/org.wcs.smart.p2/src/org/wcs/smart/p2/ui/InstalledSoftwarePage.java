/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/

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
package org.wcs.smart.p2.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.actions.PropertyDialogAction;
import org.eclipse.equinox.internal.p2.ui.actions.UninstallAction;
import org.eclipse.equinox.internal.p2.ui.dialogs.CopyUtils;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstalledIUGroup;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ICopyable;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.about.InstallationPage;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.statushandlers.StatusManager;
import org.wcs.smart.SmartApp;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.p2.internal.Messages;
import org.wcs.smart.ui.UserNamePasswordDialog;

/**
 * Modified version of the org.eclipse.equinox.p2.ui.sdk installed software pages.
 * 
 * @author Emily
 *
 */
public class InstalledSoftwarePage extends InstallationPage implements ICopyable {

	private static final int UPDATE_ID = IDialogConstants.CLIENT_ID;
	private static final int UNINSTALL_ID = IDialogConstants.CLIENT_ID + 1;
	private static final int PROPERTIES_ID = IDialogConstants.CLIENT_ID + 2;
	private static final String BUTTON_ACTION = "org.eclipse.equinox.p2.ui.buttonAction"; //$NON-NLS-1$

	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;
	String profileId;
	Button uninstallButton, propertiesButton;
	ProvisioningUI ui;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IProvHelpContextIds.INSTALLED_SOFTWARE);

		profileId = getProvisioningUI().getProfileId();
		if (profileId == null || ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()) == null) {//the second || is to make it work in the development env. 
			profileId = null;
			IStatus status = getProvisioningUI().getPolicy().getNoProfileChosenStatus();
			if (status != null)
				ProvUI.reportStatus(status, StatusManager.LOG);
			Text text = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setText(""); //$NON-NLS-1$
			setControl(text);
			return;
		}

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		int width = getDefaultWidth(composite);
		gd.widthHint = width;
		composite.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		// Table of installed IU's
		installedIUGroup = new InstalledIUGroup(getProvisioningUI(), composite, JFaceResources.getDialogFont(), profileId, getColumnConfig());
		// we hook selection listeners on the viewer in createPageButtons because we
		// rely on the actions we create there getting selection events before we use
		// them to update button enablement.

		CopyUtils.activateCopy(this, installedIUGroup.getStructuredViewer().getControl());

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		gd.widthHint = width;

		detailsArea = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		detailsArea.setBackground(detailsArea.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		detailsArea.setLayoutData(gd);

		setControl(composite);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.about.InstallationPage#createPageButtons(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageButtons(Composite parent) {
		if (profileId == null)
			return;

		
		if (!SmartDB.isMultipleAnalysis() &&
				SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ADMIN){
 			// Uninstall action
			Action uninstallAction = new UninstallAction(getProvisioningUI(), 
					installedIUGroup.getStructuredViewer(), profileId) {
				public void run() {
					if (!confirmUninstall()){
						return;
					}
					
					super.run();
					
					if (getReturnCode() == Window.OK)
						getPageContainer().closeModalContainers();
				}
			};
			uninstallButton = createButton(parent, UNINSTALL_ID, uninstallAction.getText());
			uninstallButton.setData(BUTTON_ACTION, uninstallAction);
		}
		
		// Properties action
		PropertyDialogAction action = new PropertyDialogAction(new SameShellProvider(getShell()), installedIUGroup.getStructuredViewer());
		propertiesButton = createButton(parent, PROPERTIES_ID, action.getText());
		propertiesButton.setData(BUTTON_ACTION, action);

		// We rely on the actions getting selection events before we do, because
		// we rely on the enablement state of the action.  So we don't hook
		// the selection listener on our table until after actions are created.
		installedIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetailsArea();
				updateEnablement();
			}

		});

		updateEnablement();
	}

	void updateDetailsArea() {
		java.util.List<IInstallableUnit> selected = installedIUGroup.getSelectedIUs();
		if (selected.size() == 1) {
			String description = selected.get(0).getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
			if (description != null) {
				detailsArea.setText(description);
				return;
			}
		}
		detailsArea.setText(""); //$NON-NLS-1$
	}

	void updateEnablement() {
		Button[] buttons = {uninstallButton, propertiesButton};
		
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] == null) continue;
			Action action = (Action) buttons[i].getData(BUTTON_ACTION);
			if (action == null || !action.isEnabled())
				buttons[i].setEnabled(false);
			else
				buttons[i].setEnabled(true);
		}
		if (uninstallButton != null){
			for (IInstallableUnit unit : installedIUGroup.getSelectedIUs()) {
				if (SmartApp.ID.equals(unit.getId())) {
					uninstallButton.setEnabled(false);
				}
			}
		}
	}

	private IUColumnConfig[] getColumnConfig() {
		return new IUColumnConfig[] {new IUColumnConfig("", IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH), new IUColumnConfig("", IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH), new IUColumnConfig("", IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH), new IUColumnConfig("", IUColumnConfig.COLUMN_PROVIDER, ILayoutConstants.DEFAULT_COLUMN_WIDTH)}; //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-1$
	}

	private int getDefaultWidth(Control control) {
		IUColumnConfig[] columns = getColumnConfig();
		int totalWidth = 0;
		for (int i = 0; i < columns.length; i++) {
			totalWidth += columns[i].getWidthInPixels(control);
		}
		return totalWidth + 20; // buffer for surrounding composites
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.ICopyable#copyToClipboard(org.eclipse.swt.widgets.Control)
	 */
	public void copyToClipboard(Control activeControl) {
		Object[] elements = installedIUGroup.getSelectedIUElements();
		if (elements.length == 0)
			return;
		String text = CopyUtils.getIndentedClipboardText(elements, new IUDetailsLabelProvider(null, getColumnConfig(), null));
		Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	protected boolean confirmUninstall(){
		if (!MessageDialog.openConfirm(getShell(), Messages.InstalledSoftwarePage_DialogTitle, Messages.InstalledSoftwarePage_ConfirmDelete)){
			return false;
		}
		
		//Require user to re-enter their username/password
		UserNamePasswordDialog dialog = new UserNamePasswordDialog(Display.getCurrent().getActiveShell(),
				Messages.InstalledSoftwarePage_DialogTitle,
				Messages.InstalledSoftwarePage_UserNamePassordConfirm,
				Messages.InstalledSoftwarePage_ButtonName);
		if (dialog.open() == Window.CANCEL){
			return false;
		}
		
		if (!(dialog.getUserName().equalsIgnoreCase(SmartDB.getCurrentEmployee().getSmartUserId())
			&& dialog.getPassword().equals(SmartDB.getCurrentEmployee().getSmartPassword())	)){
			
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
					Messages.InstalledSoftwarePage_ErrordialogTitle, 
					Messages.InstalledSoftwarePage_InvalidUserName);
			return false;
		}
		return true;
	}
	
	
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case UNINSTALL_ID :
				if (uninstallButton == null){
					return;
				}
				((Action) uninstallButton.getData(BUTTON_ACTION)).run();
				break;
			case PROPERTIES_ID :
				((Action) propertiesButton.getData(BUTTON_ACTION)).run();
				break;
			default :
				super.buttonPressed(buttonId);
				break;
		}
	}

	ProvisioningUI getProvisioningUI() {
		// if a UI has not been set then assume that the current default UI is the right thing
		if (ui == null)
			return ui = ProvisioningUI.getDefaultUI();
		return ui;
	}

	/**
	 * Set the provisioning UI to use with this page
	 * 
	 * @param value the provisioning ui to use
	 * @since 2.1
	 */
	public void setProvisioningUI(ProvisioningUI value) {
		ui = value;
	}

}
