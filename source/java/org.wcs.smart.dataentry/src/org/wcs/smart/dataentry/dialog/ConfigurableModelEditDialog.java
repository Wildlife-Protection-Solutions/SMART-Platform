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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryPlugIn;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing Configurable Models.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ConfigurableModelEditDialog extends TitleAreaDialog {
	
	private static final int DIALOG_WIDTH = 750;
	private static final int DIALOG_HEIGHT = 725;

	private ConfigurableModel model;
	
	private List<IConfigurableModelEditorTabContent> tabs;
	private boolean changesMade = false;
	
	private List<IConfigurableModelChangeListener> cmListeners = new ArrayList<IConfigurableModelChangeListener>();
	
	public ConfigurableModelEditDialog(ConfigurableModel model) {
		super(Display.getDefault().getActiveShell());
		this.model = model;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	public Control createDialogArea(Composite parent){
		getShell().setText( Messages.ConfigurableModelEditDialog_Title);
		Composite composite = (Composite) super.createDialogArea(parent);

		//Create an outer composite for spacing
		ScrolledComposite scrolled = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL );
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// always show the focus control
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite c = createContent(scrolled);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		scrolled.setContent(c);
		scrolled.setMinSize(scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CLOSE_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CLOSE_ID).setFocus();
		
		super.setReturnCode(IDialogConstants.CLOSE_ID);
		
		getButton(IDialogConstants.OK_ID).setEnabled(this.changesMade); //this will enable "Save" button when new model is just created
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			performSave();
			super.setReturnCode(IDialogConstants.OK_ID);
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			close();
		}
	}
	
	/**
	 * Updates the buttons of the dialog to reflect current
	 * state.
	 * 
	 * @param ischanged if changes have occurred and dialog needs to be saved
	 */
	protected void setChangesMade(boolean ischanged){
		this.changesMade = ischanged;
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(ischanged);
		}
	}
	
	/**
	 * If there are unsaved changes, the user is prompted to
	 * save changes then the dialog is closed.
	 */
	@Override
	public boolean close(){
		//ensure all edits are finished
		getButtonBar().setFocus();

		if (changesMade){
			if (!validateSave()){
				return false;
			}
		}

		return super.close();  
	}
	
	/**
	 * Validates if the current changes should be saved
	 * 
	 * @return <code>true</code> if users wishes to save and save was successful, <code>true</code> if user does not want to save, <code>false</code> if
	 * cancel pressed or error occured while saving.
	 */
	protected boolean validateSave(){
		if (getErrorMessage() != null){
			if (!MessageDialog.openQuestion(getShell(), Messages.ConfigurableModelEditDialog_CloseTitle, Messages.ConfigurableModelEditDialog_CloseMessage)){
				return false;
			}
		}else{
			MessageDialog md = new MessageDialog(getShell(), Messages.ConfigurableModelEditDialog_SaveTitle, null, Messages.ConfigurableModelEditDialog_SaveMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (!performSave()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}
		}
		return true;
	}
	@Override
	protected boolean isResizable() {
		return true;
	}
	
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
		
		tabs = getExtraTabs();
		
		ConfigurableModelEditorDefaultTab defaultTab = new ConfigurableModelEditorDefaultTab(this);
		tabs.add(0, defaultTab);
		
		if (tabs.size() > 1) {
			//we have some extra tabs and need to create tab panel
			final TabFolder tabFolder = new TabFolder (main, SWT.NONE);
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			for (IConfigurableModelEditorTabContent tabContent : tabs) {
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
		Collections.sort(extraTabs, new Comparator<IConfigurableModelEditorTabContent>() {
			@Override
			public int compare(IConfigurableModelEditorTabContent o1, IConfigurableModelEditorTabContent o2) {
				return Integer.compare(o1.getTabIndex(), o2.getTabIndex());
			}
		});
		return extraTabs;
	}
	
	@Override
	public void setButtonLayoutData(Button button) {
		//this is just a visibility change
		super.setButtonLayoutData(button);
	}

	public void notifyChangesMade() {
		fireChangesMade();
		setChangesMade(true);
	}
	
	public ConfigurableModel getModel() {
		return model;
	}
	
	protected void fireChangesMade() {
		for (IConfigurableModelChangeListener listener : cmListeners) {
			listener.notifyChangesMade();
		}
	}
	
	public void addModelChangedListener(IConfigurableModelChangeListener listener) {
		cmListeners.add(listener);
	}

	public void removeModelChangedListener(IConfigurableModelChangeListener listener) {
		cmListeners.remove(listener);
	}
	
	protected boolean performSave() {
		final boolean[] ret = new boolean[]{false};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ConfigurableModelEditDialog_SaveCmProgress, IProgressMonitor.UNKNOWN);
					Session s = HibernateManager.openSession(new AssociatedImageInterceptor());
					try{
						s.beginTransaction();
						//commit transaction
						for (IConfigurableModelEditorTabContent tab : tabs) {
							tab.performSave(s);
							s.flush();
						}
						s.getTransaction().commit();

						for (IConfigurableModelEditorTabContent tab : tabs) {
							tab.postSave();
						}
						
						ret[0] = true;
					}catch (Exception ex){
						s.getTransaction().rollback();
						SmartPlugIn.displayLog(Messages.ConfigurableModelEditDialog_SaveError  + ex.getMessage(), ex);
						ret[0] = false;
					}finally{
						s.close();
						monitor.done();
					}
					
				}
			});
		}catch(Exception ex){
			SmartPlugIn.displayLog(Messages.ConfigurableModelEditDialog_SaveError  + ex.getMessage(), ex);
			return false;
		}
		if (ret[0]) setChangesMade(false);
		return ret[0];		
	}
	
}
