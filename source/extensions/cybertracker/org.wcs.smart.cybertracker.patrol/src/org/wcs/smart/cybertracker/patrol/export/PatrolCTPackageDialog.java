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
package org.wcs.smart.cybertracker.patrol.export;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.PatrolCtPackage;
import org.wcs.smart.cybertracker.patrol.ui.PatrolCtPackageManager;
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * Dialog for exporting Cybertracker patrol package.  See PatrolPackageExporter.java for
 * more information.
 * 
 * @author Emily
 */
public class PatrolCTPackageDialog extends TitleAreaDialog {

	private static final String LAST_FILE_KEY = "PatrolCTPackageDialog.file"; //$NON-NLS-1$
	private static final String LAST_CM_KEY = "PatrolCTPackageDialog.cm"; //$NON-NLS-1$
	private static final String DM_KEY = "DataModel"; //$NON-NLS-1$
	private static final String LAST_PROFILE_KEY ="PatrolCTPackageDialog.profile"; //$NON-NLS-1$

	private ComboViewer modelViewer;
	private ComboViewer profileViewer;
	private Text txtOutputFile;
	
	private CyberTrackerPropertiesProfile selectedProfile = null;
	
	private ConfigurableModel selectedModel = null;
	private DataModelWrapper selectedDataModel = null;
	
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;
	
	private List<IPackageUiContribution> contributions = null;
	
    public PatrolCTPackageDialog(Shell parentShell) {
		super(parentShell);
		this.contributions = new ArrayList<>();
		for (IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
			if (c.getUiController() != null) this.contributions.add(c.getUiController());
		}
	}

    @Override
    public Point getInitialSize() {
    	Point pnt = super.getInitialSize();
    	if (pnt.x > 500) pnt.x = 500;
    	return pnt;
    }
    
    public void okPressed() {
    	String selectedFile = txtOutputFile.getText();
    	
    	CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(LAST_FILE_KEY, selectedFile);
    	if (selectedModel != null) {
    		CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(LAST_CM_KEY), UuidUtils.uuidToString(selectedModel.getUuid()));
    	}else {
    		CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(LAST_CM_KEY), DM_KEY);
    	}
    	CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(LAST_PROFILE_KEY), UuidUtils.uuidToString(selectedProfile.getUuid()));
    	

		Path exportFile = Paths.get(selectedFile);
		if (Files.exists(exportFile)) {
			if (!MessageDialog.openQuestion(getShell(), Messages.PatrolCTPackageDialog_DialogTitle, MessageFormat.format(Messages.PatrolCTPackageDialog_FileExistsMsg, exportFile.toString()))) {
				return;
			}
			boolean ok = true;
			try{
				ok = Files.deleteIfExists(exportFile);
			}catch (Exception ex) {
				ok = false;
			}
			if (!ok) {
				MessageDialog.openError(getShell(), Messages.PatrolCTPackageDialog_DialogTitle, MessageFormat.format(Messages.PatrolCTPackageDialog_WriteError, exportFile.toString()));
				return;
			}
		}

		PatrolCtPackage ctpackage = (PatrolCtPackage) (new PatrolCtPackageManager()).createPackage();
		ctpackage.setConfigurableModel(selectedModel);
		ctpackage.setCtProfile(selectedProfile);
		for (IPackageUiContribution c : contributions) {
			c.updatePackage(ctpackage);
		}
		
		final boolean[] iscancel = new boolean[] {false};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						SubMonitor progress = SubMonitor.convert(monitor, Messages.PatrolCTPackageDialog_TaskName, selectedDataModel == null ? 2 : 3);
						
						//process contributions
						List<IPackageContribution.PackageContribution> updates = new ArrayList<>();
						SubMonitor work = progress.split(1);
						if (contributions != null) {
							for (IPackageContribution cc : PackageContributionManager.INSTANCE.getContributionItems()) {
								IPackageContribution.PackageContribution update = cc.packageFiles(ctpackage, work);
								if (update != null) updates.add(update);
							}
						}
						progress.checkCanceled();
						
						ConfigurableModel toExport = null;
						if (selectedDataModel != null) {
							//convert data model to configurable model
							monitor.subTask(Messages.PatrolCTPackageDialog_DmToCmTaskName);
							try(Session session = HibernateManager.openSession()){
								toExport = selectedDataModel.buildConfigurableModel(session, progress.split(1));
								toExport.setConservationArea(SmartDB.getCurrentConservationArea());
							}
						}else {
							toExport = selectedModel;
						}
						ctpackage.setConfigurableModel(toExport);
						
						progress.checkCanceled();
						PatrolPackageExporter.INSTANCE.exportPackage(ctpackage, updates,  exportFile, progress.split(1));
						
						Display.getDefault().syncExec(()->{
							MessageDialog.openInformation(getShell(), Messages.PatrolCTPackageDialog_CompleteTitle, MessageFormat.format(Messages.PatrolCTPackageDialog_CompleteMsg,exportFile.toString()));	
						});
						
					}catch(OperationCanceledException e) {
						iscancel[0] = true;
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(getShell(), Messages.PatrolCTPackageDialog_CancelledTitle, Messages.PatrolCTPackageDialog_CancelledMsg);	
						});
						
					} catch (Exception e) {
						CyberTrackerPlugIn.displayError(Messages.PatrolCTPackageDialog_ErrorTitle, Messages.PatrolCTPackageDialog_ErrorMsg + e.getMessage(), e);
					}		
				}
			});
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.PatrolCTPackageDialog_ErrorTitle, Messages.PatrolCTPackageDialog_ErrorMsg + e.getMessage(), e);
			return;
		}	
		if (!iscancel[0]) super.okPressed();
    }
    
    
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.EXPORT_BUTTON_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();		
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	private void validate() {
		if (getButton(IDialogConstants.OK_ID) == null) return;
		setErrorMessage(null);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		if (txtOutputFile.getText().isEmpty()) {
			setErrorMessage(Messages.PatrolCTPackageDialog_FileRequired);
			return;
		}
		
		if (this.selectedModel == null && selectedDataModel == null) {
			setErrorMessage(Messages.PatrolCTPackageDialog_CmRequired);
			return;
		}
		
		if (this.selectedProfile == null) {
			setErrorMessage(Messages.PatrolCTPackageDialog_ProfileRequired);
			return;
		}
		for (IPackageUiContribution c : contributions) {
			String error = c.isValid();
			if (error != null) {
				setErrorMessage(error);
				return;
			}
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(true);

	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite warn = new Composite(main, SWT.BORDER);
		warn.setLayout(new GridLayout(2, false));
		warn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblwarnimage = new Label(warn, SWT.NONE);
		lblwarnimage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		
		lblwarnimage = new Label(warn, SWT.WRAP);
		lblwarnimage.setText(Messages.PatrolCTPackageDialog_warning);
		lblwarnimage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblwarnimage.getLayoutData()).widthHint = 200;
		
		Group g = new Group(main, SWT.NONE);
		g.setLayout(new GridLayout(3, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setText(Messages.PatrolCTPackageDialog_PatrolConfigurationLabel);
		
		Label outputFile = new Label(g, SWT.NONE);
		outputFile.setText(Messages.PatrolCTPackageDialog_FileLbl);
		
		txtOutputFile = new Text(g, SWT.BORDER);
		txtOutputFile.setText(""); //$NON-NLS-1$
		txtOutputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtOutputFile.addListener(SWT.Modify, e->validate());
		
		Button btnBrowse = new Button(g, SWT.PUSH);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getShell());
			fd.setFileName(txtOutputFile.getText());
			fd.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.PatrolCTPackageDialog_ZipFileLbl, Messages.PatrolCTPackageDialog_AllFileLbl});
			fd.setText(Messages.PatrolCTPackageDialog_FileDialogTitle);
			String file = fd.open();
			if (file == null) return;
			txtOutputFile.setText(file);
		});
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText(Messages.PatrolCTPackageDialog_CmLbl);
		
		modelViewer = new ComboViewer(g, SWT.READ_ONLY);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)modelViewer.getControl().getLayoutData()).widthHint = 100;
		modelViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelViewer.setLabelProvider(new ConfigurableModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DataModelWrapper) {
					return Messages.PatrolCTPackageDialog_DmLbl;
				}
				return super.getText(element);
			}
		});
		modelViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		modelViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedModel = null;
				Object profile = ((IStructuredSelection)modelViewer.getSelection()).getFirstElement();
				if (profile instanceof ConfigurableModel) {
					selectedModel = (ConfigurableModel)profile;
					cmDefaultProfile = getAssciatedProfile(selectedModel);
					profileViewer.setSelection(new StructuredSelection(cmDefaultProfile));
					selectedDataModel = null;
				}else if (profile instanceof DataModelWrapper) {
					selectedDataModel = (DataModelWrapper)profile;
					selectedModel = null;					
				}
				validate();
			}
		});

		
		Label lblProfile = new Label(g, SWT.NONE);
		lblProfile.setText(Messages.PatrolCTPackageDialog_CtProfileLbl);

		profileViewer = new ComboViewer(g, SWT.READ_ONLY);
		profileViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		profileViewer.setContentProvider(ArrayContentProvider.getInstance());
		profileViewer.setLabelProvider(new CtProfileLabelProvider());
		profileViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		profileViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
			selectedProfile = null;
				Object profile = ((IStructuredSelection)profileViewer.getSelection()).getFirstElement();
				if (profile instanceof CyberTrackerPropertiesProfile) {
					selectedProfile = (CyberTrackerPropertiesProfile)profile;
				}
				validate();
			}
		});
		
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				Composite part = cc.createUi(main, null, e->validate());
				if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			}
		}
		
		setTitle(Messages.PatrolCTPackageDialog_ShellTitle);
		setMessage(Messages.PatrolCTPackageDialog_ShellMsg);
		super.getShell().setText(Messages.PatrolCTPackageDialog_ShellTitle);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		loadData();
		
		return composite;
	}


	private CyberTrackerPropertiesProfile getAssciatedProfile(Object src) {
		try (Session session = HibernateManager.openSession()){
			if (src instanceof ConfigurableModel) {
				ConfigurableModel cm = (ConfigurableModel) src;
				ConfigurableModelCtPropertiesProfile cmctp = CyberTrackerHibernateManager.getAssociatedCmProfile(session, cm);
				CyberTrackerPropertiesProfile  profile = cmctp.getProfile();
				profile.equals(profile);
				return profile;
			} else {
				CyberTrackerPropertiesProfile profile = CyberTrackerHibernateManager.getDefaultProfile(session);
				profile.equals(profile);
				return profile;
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.PatrolCTPackageDialog_ProfileLoadError, ex);
			return null;
		}
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}

	private void loadData() {		
		String text = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(LAST_FILE_KEY);
		if (text != null) txtOutputFile.setText(text);

    	final String lastCmUuid = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(getPreferenceKey(LAST_CM_KEY));
    	final String lastProfileUuid = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(getPreferenceKey(LAST_PROFILE_KEY));
    	
		Job j = new Job("loading data") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> modelList = new ArrayList<>();
				List<CyberTrackerPropertiesProfile>  profiles = new ArrayList<>();
				DataModelWrapper dm = new DataModelWrapper();
				try(Session session = HibernateManager.openSession()){
					modelList.addAll(DataentryHibernateManager.getConfigurableModels(session));
					modelList.add(dm);
					
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
				}
				
				Display.getDefault().syncExec(()->{
					profileViewer.setInput(profiles);
					modelViewer.setInput(modelList);
					
					if (lastCmUuid != null && !lastCmUuid.isEmpty() && !lastCmUuid.equalsIgnoreCase(DM_KEY)) {
						ConfigurableModel cm = new ConfigurableModel();
						cm.setUuid(UuidUtils.stringToUuid(lastCmUuid));
						modelViewer.setSelection(new StructuredSelection(cm));
					}else if (lastCmUuid == DM_KEY) {
						modelViewer.setSelection(new StructuredSelection(dm));
					}else {
						if (!modelList.isEmpty()) modelViewer.setSelection(new StructuredSelection(modelList.get(0)));	
					}
					if (lastProfileUuid != null && !lastProfileUuid.isEmpty()) {
						CyberTrackerPropertiesProfile temp = new CyberTrackerPropertiesProfile();
						temp.setUuid(UuidUtils.stringToUuid(lastProfileUuid));
						profileViewer.setSelection(new StructuredSelection(temp));
					}else {
						if (!profiles.isEmpty()) profileViewer.setSelection(new StructuredSelection(profiles.get(0)));
					}
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}
	
	/*
	 * append the conservation area uuid to preference key so each conservation
	 * area will have it's own preferences
	 */
	private String getPreferenceKey(String key) {
		return key + "." + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$
	}
}
