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
package org.wcs.smart.export.dialog;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.export.config.ICsvImportDialogConfig;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.ConservationAreaLabelProvider;

/**
 * Dialog for importing from csv file or another conservation area
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CsvCaImportDialog extends AbstractCsvDialog {

	private static final int CONTENT_MARGIN = 15;

	private ICsvImportDialogConfig config;

	private Button btnFromCsv;
	
	private Button btnFromCa;
	private ComboViewer caViewer;

	private boolean isImportFromCa = false;
	private ConservationArea caToExportFrom;
	
	/**
	 * @param parentShell
	 * @param config
	 */
	public CsvCaImportDialog(Shell parentShell, ICsvImportDialogConfig config) {
		super(parentShell, config);
		this.config = config;
	}

	@Override
	protected boolean performAction(File file, char delimiter, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		boolean result = false;
		if (isImportFromCa) {
			File tmpFile = File.createTempFile("tempImport", ".csv"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				result = config.getExporter().exportCsvFile(tmpFile,  DelimiterCombo.Delimiter.COMMA.value, caToExportFrom, false, monitor, session);
				if (result) {
					result = config.getImporter().importCsvFile(tmpFile, DelimiterCombo.Delimiter.COMMA.value, false, monitor, session);
				}
				
			} finally {
				tmpFile.deleteOnExit();
			}
		}else{
			result = config.getImporter().importCsvFile(file, delimiter, headers, monitor, session);
		}
		return result;
	}

	@Override
	protected List<String> getWarnings(){
		return config.getImporter().getWarnings();
	}
	
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		Composite inner = new Composite(c, SWT.NONE);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		inner.setLayout(new GridLayout());
		
		List<ConservationArea> areas = loadCaList();
		if (areas.isEmpty()) {
			//no need to display ca dropdown, so display parent control with all its functionality
			super.createFileComposite(inner, true);
			return c;
		}
		
		btnFromCsv = new Button(inner, SWT.RADIO);
		btnFromCsv.setSelection(true);
		btnFromCsv.setText(Messages.CsvCaImportDialog_FromCsv_Label);
		btnFromCsv.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateControlsState();
			}
		});

		
		csvComposite = super.createFileComposite(inner, false);
		((GridLayout)csvComposite.getLayout()).marginLeft = CONTENT_MARGIN;
		csvComposite.addFileModifyListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateControlsState();
			}
		});

		createImportFromCa(inner, areas);
		updateControlsState();
		
		initDialogLabels();
		return parent;
	}

	private List<ConservationArea> loadCaList() {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		LoadCaRunnable loadCaRunnable = new LoadCaRunnable();
		try {
			dialog.run(true, true, loadCaRunnable);
		} catch (InvocationTargetException e) {
			SmartPlugIn.displayLog(Messages.CsvCaImportDialog_LoadCa_Fail_Error, e);
		} catch (InterruptedException e) {
			SmartPlugIn.displayLog(Messages.CsvCaImportDialog_LoadCa_Interrupted_Error, e);
		}
		return loadCaRunnable.getAreas();
	}

	protected CsvFileComposite getCsvComposite() {
		return csvComposite;
	}

	private void updateControlsState() {
		csvComposite.setEnabled(btnFromCsv.getSelection());
		isImportFromCa = btnFromCa.getSelection();
		caViewer.getControl().setEnabled(isImportFromCa);
		if (btnFromCsv.getSelection()) {
			Button button = getButton(IDialogConstants.OK_ID);
			if (button != null) {
				button.setEnabled(csvComposite.getFileText().length() > 0);
			}
		} else if (btnFromCa.getSelection()) {
			Button button = getButton(IDialogConstants.OK_ID);
			if (button != null) {
				button.setEnabled(!caViewer.getSelection().isEmpty());
			}
		}
	}
	
	private void createImportFromCa(Composite comp, List<ConservationArea> areas) {
		btnFromCa = new Button(comp, SWT.RADIO);
		btnFromCa.setText(Messages.CsvCaImportDialog_FromCa_Label);
		btnFromCa.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateControlsState();
			}
		});

		Composite imp = new Composite(comp, SWT.NONE);
		imp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, true);
		layout.marginLeft = CONTENT_MARGIN;
		imp.setLayout(layout);

		caViewer = new ComboViewer(imp, SWT.DROP_DOWN | SWT.READ_ONLY);
		caViewer.setContentProvider(ArrayContentProvider.getInstance());
		caViewer.setLabelProvider(new ConservationAreaLabelProvider());
		caViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				caToExportFrom = (ConservationArea) ((IStructuredSelection) caViewer.getSelection()).getFirstElement();
				updateControlsState();
			}
		});
		caViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)caViewer.getControl().getLayoutData()).widthHint = 150;
		caViewer.setInput(areas.toArray());
		caViewer.setSelection(new StructuredSelection(areas.get(0)));
	}

	/**
	 * Inner class responsible for wrapping load conservation area activities
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LoadCaRunnable implements IRunnableWithProgress {
		List<ConservationArea> areas = new ArrayList<ConservationArea>();

		private LoadCaRunnable() {}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			Session session = HibernateManager.openSession();
			try {
				synchronized (this) {
					areas = HibernateManager.getConservationAreas(session);
					areas.remove(SmartDB.getCurrentConservationArea());
				}
			} finally {
				session.close();
			}
		}

		public List<ConservationArea> getAreas() {
			return areas;
		}
		
	}
	
}
