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
package org.wcs.smart.cybertracker.survey.export;

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
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.mbtile.MapPackageContribution;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * Dialog for exporting Cybertracker patrol package.  See PatrolPackageExporter.java for
 * more information.
 * 
 * @author Emily
 */
public class SurveyCTPackageDialog extends TitleAreaDialog {

	private static final String LAST_FILE_KEY = "SurveyCTPackageDialog.file"; //$NON-NLS-1$
	private static final String LAST_DESIGN_KEY = "SurveyCTPackageDialog.cm"; //$NON-NLS-1$
	private static final String LAST_PROFILE_KEY ="SurveyCTPackageDialog.profile"; //$NON-NLS-1$
	
	private ComboViewer designViewer;
	private ComboViewer profileViewer;
	private Text txtOutputFile;
	
	private CyberTrackerPropertiesProfile selectedProfile = null;
	
	private SurveyDesign selectedDesign = null;
	
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;
	
	private List<IPackageContribution> contributions = null;
	
    public SurveyCTPackageDialog(Shell parentShell) {
		super(parentShell);
		this.contributions = PackageContributionManager.INSTANCE.getContributionItems();
		this.contributions.add(0,  new MapPackageContribution());
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
    	CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(LAST_DESIGN_KEY, UuidUtils.uuidToString(selectedDesign.getUuid()));
    	CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(LAST_PROFILE_KEY, UuidUtils.uuidToString(selectedProfile.getUuid()));
    	

		Path exportFile = Paths.get(selectedFile);
		if (Files.exists(exportFile)) {
			if (!MessageDialog.openQuestion(getShell(), Messages.SurveyCTPackageDialog_DialogTitle, MessageFormat.format(Messages.SurveyCTPackageDialog_OverwriteQuestion, exportFile.toString()))) {
				return;
			}
			boolean ok = true;
			try{
				ok = Files.deleteIfExists(exportFile);
			}catch (Exception ex) {
				ok = false;
			}
			if (!ok) {
				MessageDialog.openError(getShell(), Messages.SurveyCTPackageDialog_DialogTitle, MessageFormat.format(Messages.SurveyCTPackageDialog_OverwriteError, exportFile.toString()));
				return;
			}
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor progress = SubMonitor.convert(monitor, Messages.SurveyCTPackageDialog_ExportTaskName, 2);
					
					try {
						//process contributions
						List<IPackageContribution.PackageContribution> updates = new ArrayList<>();
						SubMonitor work = progress.split(1);
						if (contributions != null) {
							for (IPackageContribution cc : contributions) {
								IPackageContribution.PackageContribution update = cc.packageFiles(work);
								if (update != null) updates.add(update);
							}
						}
						
						SurveyPackageExporter.INSTANCE.exportPackage(selectedDesign, selectedProfile, exportFile, updates, progress.split(1));
						Display.getDefault().syncExec(()->{
							MessageDialog.openInformation(getShell(), Messages.SurveyCTPackageDialog_ExportDoneTitle, MessageFormat.format(Messages.SurveyCTPackageDialog_ExportDoneMessage,exportFile.toString()));	
						});
						
					}catch(OperationCanceledException e) {
						Display.getDefault().syncExec(()->{
							MessageDialog.openError(getShell(), Messages.SurveyCTPackageDialog_CancelledTitle, Messages.SurveyCTPackageDialog_CancelledMsg);	
						});
						
					} catch (Exception e) {
						CyberTrackerPlugIn.displayError(Messages.SurveyCTPackageDialog_ErrorTitle, Messages.SurveyCTPackageDialog_ExportErrorMsg + e.getMessage(), e);
					}		
				}
			});
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.SurveyCTPackageDialog_ErrorTitle, Messages.SurveyCTPackageDialog_ExportErrorMsg + e.getMessage(), e);
			return;
		}	
		super.okPressed();	
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
			setErrorMessage(Messages.SurveyCTPackageDialog_FileRequired);
			return;
		}
		
		if (this.selectedDesign == null) {
			setErrorMessage(Messages.SurveyCTPackageDialog_DesignRequired);
			return;
		}
		
		if (this.selectedProfile == null) {
			setErrorMessage(Messages.SurveyCTPackageDialog_ProfileRequired);
			return;
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
		lblwarnimage.setText(Messages.SurveyCTPackageDialog_warning);
		lblwarnimage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblwarnimage.getLayoutData()).widthHint = 200;
		
		Group g = new Group(main, SWT.NONE);
		g.setText(Messages.SurveyCTPackageDialog_PropertiesGroup);
		g.setLayout(new GridLayout(3, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label outputFile = new Label(g, SWT.NONE);
		outputFile.setText(Messages.SurveyCTPackageDialog_FileLabel);
		
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
			fd.setFilterNames(new String[] {Messages.SurveyCTPackageDialog_ZipFileLbl, Messages.SurveyCTPackageDialog_AllFileLbl});
			fd.setText(Messages.SurveyCTPackageDialog_FileDialogTitle);
			String file = fd.open();
			if (file == null) return;
			txtOutputFile.setText(file);
		});
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText(Messages.SurveyCTPackageDialog_DesignLabel);
		
		designViewer = new ComboViewer(g, SWT.READ_ONLY);
		designViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)designViewer.getControl().getLayoutData()).widthHint = 100;
		designViewer.setContentProvider(ArrayContentProvider.getInstance());
		designViewer.setLabelProvider(new SurveyDesignLabelProvider());
		
		designViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		designViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		designViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedDesign = null;
				Object profile = ((IStructuredSelection)designViewer.getSelection()).getFirstElement();
				if (profile instanceof SurveyDesign) {
					selectedDesign = (SurveyDesign)profile;
					cmDefaultProfile = getAssciatedProfile(selectedDesign);
					profileViewer.setSelection(new StructuredSelection(cmDefaultProfile));
				}
				validate();
			}
		});

		
		Label lblProfile = new Label(g, SWT.NONE);
		lblProfile.setText(Messages.SurveyCTPackageDialog_ProfileLabel);

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
			for (IPackageContribution cc : contributions) {
				Composite part = cc.createUi(main, "survey"); //$NON-NLS-1$
				if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			}
		}
		
		setTitle(Messages.SurveyCTPackageDialog_ShellTitle);
		setMessage(Messages.SurveyCTPackageDialog_ShellMsg);
		super.getShell().setText(Messages.SurveyCTPackageDialog_ShellTitle);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		loadData();
		
		return composite;
	}


	private CyberTrackerPropertiesProfile getAssciatedProfile(Object src) {
		try (Session session = HibernateManager.openSession()){
			if (src instanceof SurveyDesign) {
				SurveyDesign cm = session.get(SurveyDesign.class,((SurveyDesign) src).getUuid());
				if (cm.getConfigurableModel() != null) {
					ConfigurableModelCtPropertiesProfile cmctp = CyberTrackerHibernateManager.getAssociatedCmProfile(session, cm.getConfigurableModel());
					CyberTrackerPropertiesProfile  profile = cmctp.getProfile();
					profile.equals(profile);
					return profile;
				}
			}
			CyberTrackerPropertiesProfile profile = CyberTrackerHibernateManager.getDefaultProfile(session);
			profile.equals(profile);
			return profile;
			
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.SurveyCTPackageDialog_ProfileLoadError, ex);
			return null;
		}
	}
	

	private void loadData() {
		
		String text = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(LAST_FILE_KEY);
		if (text != null) txtOutputFile.setText(text);
		
    	final String lastDesignUuid = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(LAST_DESIGN_KEY);
    	final String lastProfileUuid = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(LAST_PROFILE_KEY);
    	
		Job j = new Job("loading data") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<SurveyDesign> designList = new ArrayList<>();
				List<CyberTrackerPropertiesProfile>  profiles = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					
					designList.addAll(QueryFactory.buildQuery(session, SurveyDesign.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
							new Object[] {"state", State.ACTIVE}).list()); //$NON-NLS-1$
					
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
				}
				
				Display.getDefault().syncExec(()->{
					profileViewer.setInput(profiles);
					designViewer.setInput(designList);
					
					if (lastDesignUuid != null && !lastDesignUuid.isEmpty()) {
						SurveyDesign ds = new SurveyDesign();
						ds.setUuid(UuidUtils.stringToUuid(lastDesignUuid));
						designViewer.setSelection(new StructuredSelection(ds));
					}else {
						if (!designList.isEmpty()) designViewer.setSelection(new StructuredSelection(designList.get(0)));	
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
}
