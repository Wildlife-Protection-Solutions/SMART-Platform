/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.inout;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ConservationAreaLabelProvider;

/**
 * Page for collecting csv file details for importing asset data.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ExportLocationWizardPage extends WizardPage {

	public static final String MODELEXPORT_DIR_KEY = ExportLocationWizardPage.class.getCanonicalName() + ".dir";  //$NON-NLS-1$
	
	private Button btnOpFile;
	private Button btnOpCa;
	private Text txtOutputFile;
	private ComboViewer cmbCa;
	
	protected ExportLocationWizardPage() {
		super("EXPORT_LOCATION_PAGE"); //$NON-NLS-1$
	}

	public void pageShown() {
		
	}
	
	
	@Override
	public IWizardPage getNextPage() {
		boolean hascsv = false;
		boolean hasmodel = false;
		for (AssetDataExportWizard.Type t : getWizardLocal().typePage.getTypes()) {
			if (t.isCsv()) hascsv = true;
			if (t == AssetDataExportWizard.Type.XML) hasmodel = true;
		}

		if ( hascsv && btnOpFile.getSelection() ) {
			return ((AssetDataExportWizard)getWizardLocal()).csvOpPage;
		}
		
		if (  hasmodel ) {
			return ((AssetDataExportWizard)getWizard()).xmlOpPage;
		}
		return null;
	}
	
	private AssetDataExportWizard getWizardLocal() {
		return (AssetDataExportWizard) getWizard();
	}

	/**
	 * 
	 * @return the directory to export files to or null if exporting to ca
	 */
	public String getDirectory() {
		if (btnOpCa.getSelection()) return null;
		return txtOutputFile.getText();
	}
	
	/**
	 * 
	 * @return the conservation area to export to or null if exporting to file
	 */
	public ConservationArea getConservationArea() {
		if (!btnOpCa.getSelection()) return null;
		return (ConservationArea) cmbCa.getStructuredSelection().getFirstElement();
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite fileSection = new Composite(main, SWT.NONE);
		fileSection.setLayout(new GridLayout(3, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnOpFile = new Button(fileSection, SWT.RADIO);
		btnOpFile.setText(Messages.ExportLocationWizardPage_Directory);

		txtOutputFile = new Text(fileSection, SWT.BORDER);
		txtOutputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String initDir = AssetPlugIn.getDefault().getPreferenceStore().getString(MODELEXPORT_DIR_KEY);
		if (initDir != null && initDir.length() != 0) {
			txtOutputFile.setText(initDir);
		} else {
			txtOutputFile.setText(System.getProperty("user.home")); //$NON-NLS-1$
		}
		Button btnBrowse = new Button(fileSection, SWT.NONE);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData) btnBrowse.getLayoutData()).heightHint = txtOutputFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

		btnBrowse.addListener(SWT.Selection, e -> {
			DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
			
			if (!txtOutputFile.getText().strip().isBlank()) dialog.setFilterPath(txtOutputFile.getText());
			dialog.setText(Messages.ExportLocationWizardPage_DirSelectorMessage);
			
			String file = dialog.open();
			if (file != null)
				txtOutputFile.setText(file);
		});

		btnOpCa = new Button(fileSection, SWT.RADIO);
		btnOpCa.setText(Messages.ExportLocationWizardPage_CaOption);

		cmbCa = new ComboViewer(fileSection, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.DROP_DOWN);
		cmbCa.setContentProvider(ArrayContentProvider.getInstance());
		cmbCa.setLabelProvider(new ConservationAreaLabelProvider());
		cmbCa.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Listener enabledListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				cmbCa.getControl().setEnabled(btnOpCa.getSelection());

				btnBrowse.setEnabled(btnOpFile.getSelection());
				txtOutputFile.setEnabled(btnOpFile.getSelection());
				
				getWizard().getContainer().updateButtons();
			}
		};
		btnOpCa.setSelection(false);
		btnOpFile.setSelection(true);
		btnOpCa.addListener(SWT.Selection, enabledListener);
		btnOpFile.addListener(SWT.Selection, enabledListener);
		enabledListener.handleEvent(null);
	
		initializeLists.schedule();
		
		setTitle(Messages.ExportLocationWizardPage_Title);
		setMessage(Messages.ExportLocationWizardPage_Message);
		setControl(main);
	}

	private Job initializeLists = new Job(Messages.ExportModelToXmlDialog_initJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<ConservationArea> cas;

			try (Session s = HibernateManager.openSession()) {
				cas = QueryFactory.buildQuery(s, ConservationArea.class).list();
				for (Iterator<ConservationArea> iterator = cas.iterator(); iterator.hasNext();) {
					ConservationArea conservationArea = iterator.next();
					if (conservationArea.getIsCcaa())
						iterator.remove();
				}
				cas.remove(SmartDB.getCurrentConservationArea());
				ConservationArea ccaa = null;
				for (ConservationArea c : cas) {
					if (c.getIsCcaa())
						ccaa = c;
				}
				cas.remove(ccaa);
				cas.forEach(a -> a.getName());
			}

			Display.getDefault().syncExec(() -> {
				cmbCa.setInput(cas);
				if (!cas.isEmpty()) cmbCa.setSelection(new StructuredSelection(cas.get(0)));
			});

			return Status.OK_STATUS;
		}

	};
}
