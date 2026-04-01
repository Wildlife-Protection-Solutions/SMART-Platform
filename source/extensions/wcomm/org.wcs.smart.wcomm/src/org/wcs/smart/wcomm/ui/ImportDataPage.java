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
package org.wcs.smart.wcomm.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.wcomm.DataLoader;
import org.wcs.smart.wcomm.Messages;
import org.wcs.smart.wcomm.WCommPlugIn;

/**
 * Editor page for importing wcomm data.
 * 
 * @author Emily
 *
 */
public class ImportDataPage extends EditorPart {

	
	private static final String PREF_EM = "org.wcs.smart.wcomm.EM"; //$NON-NLS-1$
	private static final String PREF_WO = "org.wcs.smart.wcomm.WO"; //$NON-NLS-1$
	private static final String PREF_OC = "org.wcs.smart.wcomm.OC"; //$NON-NLS-1$
	private static final String PREF_HWC = "org.wcs.smart.wcomm.HWC"; //$NON-NLS-1$
	private static final String PREF_INC = "org.wcs.smart.wcomm.INC"; //$NON-NLS-1$
	private static final String PREF_DATE = "org.wcs.smart.wcomm.DATE"; //$NON-NLS-1$
	
	private FormToolkit toolkit;
	
	private enum UtmZone{
		UTM37N,
		UTM37S;
	}
	
	private Text txtResults;
	private Text txtWoFile, txtEmFile, txtOcFile, txtHwcFile, txtIncidentFile;
	private Text txtDateFormat;

	private ComboViewer cmbPType;
	private ComboViewer cmbUTMZone;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	private void setPreferences() {
		WCommPlugIn.getDefault().getPreferenceStore().setValue(PREF_EM, txtEmFile.getText());
		WCommPlugIn.getDefault().getPreferenceStore().setValue(PREF_WO, txtWoFile.getText());
		WCommPlugIn.getDefault().getPreferenceStore().setValue(PREF_OC, txtOcFile.getText());
		WCommPlugIn.getDefault().getPreferenceStore().setValue(PREF_HWC, txtHwcFile.getText());
		WCommPlugIn.getDefault().getPreferenceStore().setValue(PREF_INC, txtIncidentFile.getText());
		WCommPlugIn.getDefault().getPreferenceStore().setValue(PREF_DATE, txtDateFormat.getText());
	}
	
	private void initPreferences() {
		String t = WCommPlugIn.getDefault().getPreferenceStore().getString(PREF_EM);
		if (t != null) txtEmFile.setText(t);
		
		t = WCommPlugIn.getDefault().getPreferenceStore().getString(PREF_WO);
		if (t != null) txtWoFile.setText(t);
		
		t = WCommPlugIn.getDefault().getPreferenceStore().getString(PREF_OC);
		if (t != null) txtOcFile.setText(t);
		
		t = WCommPlugIn.getDefault().getPreferenceStore().getString(PREF_HWC);
		if (t != null) txtHwcFile.setText(t);
		
		t = WCommPlugIn.getDefault().getPreferenceStore().getString(PREF_INC);
		if (t != null) txtIncidentFile.setText(t);
		
		t = WCommPlugIn.getDefault().getPreferenceStore().getString(PREF_DATE);
		if (t != null && !t.isBlank()) txtDateFormat.setText(t);
		
		Listener setL = e->setPreferences();
		
		txtEmFile.addListener(SWT.Modify, setL);
		txtWoFile.addListener(SWT.Modify, setL);
		txtOcFile.addListener(SWT.Modify, setL);
		txtHwcFile.addListener(SWT.Modify, setL);
		txtIncidentFile.addListener(SWT.Modify, setL);
		txtDateFormat.addListener(SWT.Modify, setL);
	}
	
	private CoordinateReferenceSystem getCrs() throws Exception{
		if (cmbUTMZone.getStructuredSelection().getFirstElement() == UtmZone.UTM37N) {
			return CRS.decode("EPSG:32637"); //$NON-NLS-1$
		}
		if (cmbUTMZone.getStructuredSelection().getFirstElement() == UtmZone.UTM37S) {
			return CRS.decode("EPSG:32737"); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form f = toolkit.createForm(parent);
		f.setText(Messages.ImportDataPage_FormName);
		
		f.getBody().setLayout(new GridLayout());
		((GridLayout)f.getBody().getLayout()).marginWidth = 0;
		((GridLayout)f.getBody().getLayout()).marginHeight = 0;
		
		Composite main = toolkit.createComposite(f.getBody(), SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite filec = toolkit.createComposite(main, SWT.NONE);
		filec.setLayout(new GridLayout(3, false));
		filec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(filec, Messages.ImportDataPage_WOSection, SWT.NONE);
		txtWoFile = toolkit.createText(filec, "", SWT.BORDER); //$NON-NLS-1$
		txtWoFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button btn = toolkit.createButton(filec, "...", SWT.PUSH); //$NON-NLS-1$
		btn.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.ImportDataPage_csv, Messages.ImportDataPage_allfiles});
			fd.setText(Messages.ImportDataPage_selectfiletext);
			String filename = fd.open();
			if (filename == null) return;
			txtWoFile.setText(filename);
		});
		
		toolkit.createLabel(filec, Messages.ImportDataPage_EMSection, SWT.NONE);
		txtEmFile = toolkit.createText(filec, "", SWT.BORDER); //$NON-NLS-1$
		txtEmFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btn = toolkit.createButton(filec, "...", SWT.PUSH); //$NON-NLS-1$
		btn.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.ImportDataPage_csv, Messages.ImportDataPage_allfiles});
			fd.setText(Messages.ImportDataPage_selectfiletext);
			String filename = fd.open();
			if (filename == null) return;
			txtEmFile.setText(filename);
		});
		
		toolkit.createLabel(filec, Messages.ImportDataPage_OCSection, SWT.NONE);
		txtOcFile = toolkit.createText(filec, "", SWT.BORDER); //$NON-NLS-1$
		txtOcFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btn = toolkit.createButton(filec, "...", SWT.PUSH); //$NON-NLS-1$
		btn.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.ImportDataPage_csv, Messages.ImportDataPage_allfiles});
			fd.setText(Messages.ImportDataPage_selectfiletext);
			String filename = fd.open();
			if (filename == null) return;
			txtOcFile.setText(filename);
		});
		
		toolkit.createLabel(filec, Messages.ImportDataPage_HWCSection, SWT.NONE);
		txtHwcFile = toolkit.createText(filec, "", SWT.BORDER); //$NON-NLS-1$
		txtHwcFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btn = toolkit.createButton(filec, "...", SWT.PUSH); //$NON-NLS-1$
		btn.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.ImportDataPage_csv, Messages.ImportDataPage_allfiles});
			fd.setText(Messages.ImportDataPage_selectfiletext);
			String filename = fd.open();
			if (filename == null) return;
			txtHwcFile.setText(filename);
		});
		
		toolkit.createLabel(filec, Messages.ImportDataPage_IncidentsSection, SWT.NONE);
		txtIncidentFile = toolkit.createText(filec, "", SWT.BORDER); //$NON-NLS-1$
		txtIncidentFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btn = toolkit.createButton(filec, "...", SWT.PUSH); //$NON-NLS-1$
		btn.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.ImportDataPage_csv, Messages.ImportDataPage_allfiles});
			fd.setText(Messages.ImportDataPage_selectfiletext);
			String filename = fd.open();
			if (filename == null) return;
			txtIncidentFile.setText(filename);
		});
		
		toolkit.createLabel(filec, Messages.ImportDataPage_TransportTypeOp);
		cmbPType = new ComboViewer(filec, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPType.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((PatrolTransportType)element).getName();
			}
		});
		cmbPType.setContentProvider(ArrayContentProvider.getInstance());
		cmbPType.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		
		
		toolkit.createLabel(filec, Messages.ImportDataPage_UTMZoneOp);
		cmbUTMZone = new ComboViewer(filec, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbUTMZone.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == UtmZone.UTM37N) return "UTM Zone 37N"; //$NON-NLS-1$
				if (element == UtmZone.UTM37S) return "UTM Zone 37S"; //$NON-NLS-1$
				return super.getText(element);
			}
		});
		cmbUTMZone.setContentProvider(ArrayContentProvider.getInstance());
		cmbUTMZone.setInput(UtmZone.values());
		cmbUTMZone.setSelection(new StructuredSelection(UtmZone.UTM37N));
		cmbUTMZone.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		
		Label l = toolkit.createLabel(filec, "Date Format:", SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite dtinfo = toolkit.createComposite(filec);
		dtinfo.setLayout(new GridLayout());
		((GridLayout)dtinfo.getLayout()).marginWidth = 0;
		((GridLayout)dtinfo.getLayout()).marginHeight = 0;
		txtDateFormat = toolkit.createText(dtinfo, "dd/MM/yyyy HH:mm", SWT.BORDER); //$NON-NLS-1$
		txtDateFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		toolkit.createLabel(dtinfo, "The time part is ignored, to specify time use a 'Time' column (ex: dd/MM/yyyy HH:mm)");
		
		try(Session session = HibernateManager.openSession()){
			List<PatrolTransportType> tts = QueryFactory.buildQuery(session, PatrolTransportType.class, new Object[] {"conservationArea",  SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			tts.forEach(r->r.getName());
			cmbPType.setInput(tts);
			
			if (!tts.isEmpty()) cmbPType.setSelection(new StructuredSelection(tts.get(0)));
		}
		
		Button btnGo = toolkit.createButton(filec, Messages.ImportDataPage_LoadDataButton, SWT.PUSH);
		btnGo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		btnGo.addListener(SWT.Selection, e->{
			try {
				doLoad();
			}catch (Exception ex) {
				WCommPlugIn.displayLog(ex.getMessage(), ex);
			}
		});
				
		Composite resultsc = toolkit.createComposite(main, SWT.NONE);
		resultsc.setLayout(new GridLayout());
		resultsc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(resultsc, Messages.ImportDataPage_ResultsSection);
		txtResults = toolkit.createText(resultsc, "", SWT.MULTI | SWT.V_SCROLL); //$NON-NLS-1$
		txtResults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		initPreferences();
	}
	
	private void doLoad() throws Exception {
		
		Path wo = null;
		Path em = null;
		Path oc = null;
		Path hwc = null;
		Path incident = null;
		String dateFormat = null;
		
		if (!txtWoFile.getText().isEmpty()) wo = Paths.get(txtWoFile.getText());
		if (!txtEmFile.getText().isEmpty()) em = Paths.get(txtEmFile.getText());
		if (!txtOcFile.getText().isEmpty()) oc = Paths.get(txtOcFile.getText());
		if (!txtHwcFile.getText().isEmpty()) hwc = Paths.get(txtHwcFile.getText());
		if (!txtIncidentFile.getText().isEmpty()) incident = Paths.get(txtIncidentFile.getText());
		if (!txtDateFormat.getText().isEmpty()) dateFormat = txtDateFormat.getText();

		if (wo != null && !Files.exists(wo)) throw new Exception(Messages.ImportDataPage_invalidwofile);
		if (em != null && !Files.exists(em)) throw new Exception(Messages.ImportDataPage_invalidemfile);
		if (oc != null && !Files.exists(oc)) throw new Exception(Messages.ImportDataPage_invalidocfile);
		if (hwc != null && !Files.exists(hwc)) throw new Exception(Messages.ImportDataPage_invalidhwcfile);
		if (incident != null && !Files.exists(incident)) throw new Exception(Messages.ImportDataPage_invalidincidentsfile);
		
		try {
			txtResults.setText(Messages.ImportDataPage_loadingtext);
			
			PatrolTransportType tt = (PatrolTransportType) cmbPType.getStructuredSelection().getFirstElement();
			DataLoader dl = new DataLoader(wo, em, oc, hwc, incident, tt, getCrs(), dateFormat);
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
			pmd.run(true,  false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						dl.loadData(monitor);
						if (dl.getPatrols() != null)
							for (Patrol p : dl.getPatrols()) PatrolEventManager.getInstance().patrolAdded(p);
					} catch (Exception e) {
						WCommPlugIn.displayLog(e.getMessage(), e);
					}
				}
			});
			
			StringBuilder sb = new StringBuilder();
			if (dl.getPatrols() == null) {
				sb.append(Messages.ImportDataPage_NoData);
			}else {
				sb.append(Messages.ImportDataPage_PatrolsSection);
				sb.append("\n"); //$NON-NLS-1$
				for (Patrol s : dl.getPatrols()) {
					sb.append(s.getId());
					sb.append("\n"); //$NON-NLS-1$
				}
			}
			sb.append("\n"); //$NON-NLS-1$
			sb.append(Messages.ImportDataPage_WarningsSection);
			sb.append("\n"); //$NON-NLS-1$
			for (String s : dl.getWarnings()) {
				sb.append(s);
				sb.append("\n"); //$NON-NLS-1$
			}
			txtResults.setText(sb.toString());
		} catch (Exception e) {
			WCommPlugIn.displayLog(e.getMessage(), e);
		}
	}
	@Override
	public void dispose(){
		super.dispose();
		
		toolkit.dispose();
	}
	

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void setFocus() {
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
}