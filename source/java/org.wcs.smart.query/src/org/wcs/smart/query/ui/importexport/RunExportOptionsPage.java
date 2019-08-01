/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.query.ui.importexport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Wizard page for collecting export options
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class RunExportOptionsPage extends WizardPage {

	public static final String NAME = "FORMATPAGE"; //$NON-NLS-1$
	
	private static final String DIR_PREF_KEY = "org.wcs.smart.query.export.location"; //$NON-NLS-1$
	
	private Label lblProjection, lblDelimiter, lblLocation;
	private Text txtDir;
	private ComboViewer cmbProjection;
	private DelimiterCombo cmbDelimiter;
	
	protected RunExportOptionsPage() {
		super(NAME);
	}

	public RunExportWizard getWizardInternal() {
		return (RunExportWizard)getWizard();
	}
	
	public Projection getProjection() {
		if (cmbProjection == null) return null;
		return (Projection) cmbProjection.getStructuredSelection().getFirstElement();
	}
	
	public Character getDelimiter() {
		if (cmbDelimiter == null) return null;
		try {
			return Character.valueOf(cmbDelimiter.getDelimiter());
		}catch (Exception ex) {
			QueryPlugIn.log(ex.getMessage(),ex);
			return Character.valueOf(ICsvQueryExporter.DEFAULT_DELIMITER);
		}
	}
	
	public void updateControls() {
		lblProjection.setVisible(getWizardInternal().needsProjection());
		cmbProjection.getControl().setVisible(getWizardInternal().needsProjection());
		lblDelimiter.setVisible(getWizardInternal().needsDelimitier());
		cmbDelimiter.getControl().setVisible(getWizardInternal().needsDelimitier());
	}
	
	public String getOutputDirectory() {
		if (!txtDir.getText().strip().isEmpty()) QueryPlugIn.getDefault().getPreferenceStore().putValue(DIR_PREF_KEY, txtDir.getText());
		return txtDir.getText();
	}
	
	private void validate() {
		if (txtDir.getText().strip().isEmpty()) {
			setPageComplete(false);
			setErrorMessage(Messages.RunExportOptionsPage_LocationRequired);
		}else {
			setPageComplete(true);
			setErrorMessage(null);
		}
	}
	
	@Override
	public void createControl(Composite parent) {

		Composite core = new Composite(parent, SWT.NONE);
		core.setLayout(new GridLayout());
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		core = new Composite(core, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		lblLocation = new Label(core, SWT.NONE);
		lblLocation.setText(Messages.RunExportOptionsPage_LocaltionLabel);
		
		Composite t = new Composite(core, SWT.NONE);
		t.setLayout(new GridLayout(2, false));
		((GridLayout)t.getLayout()).marginWidth = 0;
		((GridLayout)t.getLayout()).marginHeight = 0;
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		txtDir = new Text(t, SWT.BORDER);
		txtDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtDir.addListener(SWT.Modify,  e->validate());
		
		String init = QueryPlugIn.getDefault().getPreferenceStore().getString(DIR_PREF_KEY);
		txtDir.setText(init);
		validate();
		
		Button btn = new Button(t, SWT.PUSH);
		btn.setText("..."); //$NON-NLS-1$
		btn.addListener(SWT.Selection, e->{
			DirectoryDialog dd = new DirectoryDialog(getShell());
			dd.setMessage(Messages.RunExportOptionsPage_DirectoryMsg);
			dd.setText(Messages.RunExportOptionsPage_DirectoryTitle);
			dd.setFilterPath(txtDir.getText());
			String i = dd.open();
			if (i != null) txtDir.setText(i);
		});
		
		
		lblProjection = new Label(core, SWT.NONE);
		lblProjection.setText(Messages.ExportQueryLocationPage_ProjectionLbl);
		lblProjection.setToolTipText(Messages.ExportQueryLocationPage_ProjectionTooltip);
			
		cmbProjection = new ComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
		loadProjections.schedule();
		
		lblDelimiter = new Label(core, SWT.NONE);
		lblDelimiter.setText(Messages.RunExportOptionsPage_DelimiterLbl);
		cmbDelimiter = new DelimiterCombo(core, SWT.DROP_DOWN | SWT.READ_ONLY);
		
		setControl(core.getParent());
		
		setTitle(Messages.RunExportOptionsPage_PageTitle);
		setMessage(Messages.RunExportOptionsPage_PageMessage);
	}

	
	private Job loadProjections = new Job("loading projections") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Projection> prjs = new ArrayList<>();
			Projection defaultPrj = null;
			try(Session s = HibernateManager.openSession()){
				prjs.addAll( QueryFactory.buildQuery(s,Projection.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).getResultList()); //$NON-NLS-1$
				defaultPrj = HibernateManager.getCurrentViewProjection(s);
				
				for (Iterator<Projection> iterator = prjs.iterator(); iterator.hasNext();) {
					Projection p = iterator.next();
					try {
						p.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(p.getDefinition()));
					}catch (Exception ex) {
						QueryPlugIn.log(ex.getMessage(), ex);
						iterator.remove();
					}	
				}
			}
			
			final Projection fdefaultPrj = defaultPrj;
			Display.getDefault().syncExec(()->{
				cmbProjection.setInput(prjs);
				cmbProjection.setSelection(new StructuredSelection(fdefaultPrj));
			});
			return Status.OK_STATUS;
		}
		
	};

}
