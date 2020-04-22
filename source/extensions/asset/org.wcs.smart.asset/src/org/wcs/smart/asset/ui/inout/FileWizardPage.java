/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.ui.inout.AssetDataImportWizard.Type;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Page for collecting csv file details for importing asset data.
 * 
 * @author Emily
 *
 */
public class FileWizardPage extends WizardPage {

	public static final String PREFERENCE_DIR_KEY = FileWizardPage.class.getCanonicalName() + ".dir";  //$NON-NLS-1$

	private Text txtFile;
	private DelimiterCombo cmbDelimeter;
	private Button btnSkipFirst;
	private Label llDateFormat;
	private ComboViewer cmbDateFormat;
	private ComboViewer cmbProjection;
	private boolean isValid = true;
	private Composite fileComp;
	
	protected FileWizardPage() {
		super("FILE_PAGE"); //$NON-NLS-1$
	}

	public Path getFile() {
		if (txtFile.getText().trim().isEmpty()) return null;
		return Paths.get(txtFile.getText());
	}
	
	public char getDelimiter() {
		try {
			return cmbDelimeter.getDelimiter();
		} catch (Exception e) {
			AssetPlugIn.log(e.getMessage(), e);
		}
		return ',';
	}
	
	public boolean skipFirst() {
		return btnSkipFirst.getSelection();
	}
	
	public String getDateTimeFormat() {
		if (cmbDateFormat.getControl().isDisposed()) return "d-M-y"; //$NON-NLS-1$
		return cmbDateFormat.getCombo().getText();
	}
	
	public Projection getProjection(){
		return (Projection) ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
	}
	
	public boolean canFlipToNextPage() {
		return isValid;
	}
	
	public void pageShown() {
		boolean hasDate = true;
		if ( ((AssetDataImportWizard) getWizard()).getType() == Type.STATION_CSV  ) {
			//lets see if it's valid
			hasDate = false;
			try(Session session = HibernateManager.openSession()){
				List<AssetStationAttribute> stnAttributes = QueryFactory.buildQuery(session, AssetStationAttribute.class, new Object[] {"attribute.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				for (AssetStationAttribute a : stnAttributes) {
					if (a.getAttribute().getType() == AttributeType.DATE) {
						hasDate = true;
						break;
					}
				}
			}
			setTitle(Messages.StationMappingPage_Title);
		}else if ( ((AssetDataImportWizard) getWizard()).getType() == Type.LOCATION_CSV ) {
			hasDate = false;
			try(Session session = HibernateManager.openSession()){
				List<AssetStationLocationAttribute> stnAttributes = QueryFactory.buildQuery(session, AssetStationLocationAttribute.class, new Object[] {"attribute.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				for (AssetStationLocationAttribute a : stnAttributes) {
					if (a.getAttribute().getType() == AttributeType.DATE) {
						hasDate = true;
						break;
					}
				}
			}
			setTitle(Messages.LocationMappingPage_Title);
		}else if ( ((AssetDataImportWizard) getWizard()).getType() == Type.ASSET_CSV ) {
			hasDate = false;
			try(Session session = HibernateManager.openSession()){
				List<AssetTypeAttribute> stnAttributes = QueryFactory.buildQuery(session, AssetTypeAttribute.class, new Object[] {"id.attribute.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				for (AssetTypeAttribute a : stnAttributes) {
					if (a.getAttribute().getType() == AttributeType.DATE) {
						hasDate = true;
						break;
					}
				}
			}
			setTitle(Messages.AssetMappingPage_Title);
		}
		if (!hasDate) {
			if (!cmbDateFormat.getControl().isDisposed()) {
				cmbDateFormat.getControl().dispose();
				llDateFormat.dispose();
			}
			txtFile.getParent().getParent().layout(true);
			
		}else {
			if (cmbDateFormat.getControl().isDisposed()) {
				createDateFormatControl();
				llDateFormat.moveBelow(btnSkipFirst);
				cmbDateFormat.getCombo().moveBelow(llDateFormat);
			}
			txtFile.getParent().getParent().layout(true);		
		}
	}
	
	private void createDateFormatControl() {
		llDateFormat = new Label(fileComp, SWT.NONE);
		llDateFormat.setText(Messages.FileWizardPage_DateFormatLabel);
		llDateFormat.setToolTipText(Messages.FileWizardPage_DateFormatTooltip);
		llDateFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		cmbDateFormat = new ComboViewer(fileComp, SWT.DROP_DOWN);
		cmbDateFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbDateFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbDateFormat.setLabelProvider(new LabelProvider());
		String[] dateFormats = new String[]{
				"yyyy-MM-dd", //$NON-NLS-1$
				DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.MEDIUM, null, IsoChronology.INSTANCE, Locale.getDefault()),
				DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, Locale.getDefault()),
				"d/M/y", //$NON-NLS-1$
				"d-M-y", //$NON-NLS-1$
				"M/d/y", //$NON-NLS-1$
				"M-d-y", //$NON-NLS-1$
				"y/M/d", //$NON-NLS-1$
				"y-M-d" //$NON-NLS-1$
		};
		cmbDateFormat.setInput(dateFormats);
		cmbDateFormat.setSelection(new StructuredSelection(dateFormats[0]));
		cmbDateFormat.getCombo().addListener(SWT.Modify, e-> {validate(); getWizard().getContainer().updateButtons();});
	}
	@Override
	public IWizardPage getNextPage() {
		AssetDataImportWizard w = (AssetDataImportWizard) getWizard();
		
		if (w.typePage.getType() == AssetDataImportWizard.Type.ASSET_CSV) {
			w.assetPage.updateMapping(getFile(),getDelimiter());
			return w.assetPage;
		}
		if (w.typePage.getType() == AssetDataImportWizard.Type.STATION_CSV) {
			w.stationPage.updateMapping(getFile(),getDelimiter());
			return w.stationPage;
		}
		if (w.typePage.getType() == AssetDataImportWizard.Type.LOCATION_CSV) {
			w.locationPage.updateMapping(getFile(),getDelimiter());
			return w.locationPage;
		}
		return null;
			
		
	}
	
	private void validate() {
		setErrorMessage(null);
		isValid = true;
		if (txtFile.getText().trim().isEmpty()) {
			setErrorMessage(Messages.FileWizardPage_FileRequired);
			isValid = false;
		}else {
			Path p = Paths.get(txtFile.getText());
			if (!Files.exists(p)) {
				setErrorMessage(Messages.FileWizardPage_FileNotFound);
				isValid = false;
			}
			AssetPlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_DIR_KEY, p.toString());
		}
		
		if (!cmbDateFormat.getControl().isDisposed()) {
	    	try{
	    		DateTimeFormatter.ofPattern(cmbDateFormat.getCombo().getText());
	    	}catch (Exception ex){
	    		setErrorMessage(Messages.FileWizardPage_DateTimeRequired);
	    		isValid = false;
	    	}
		}
    	 
    	Object x= cmbProjection.getStructuredSelection().getFirstElement();
    	if ( x == null || !(x instanceof Projection)){
    		setErrorMessage(Messages.FileWizardPage_ProjectionRequired);
    		isValid = false;
    	}
    	
	}
	
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		fileComp = new Composite(main, SWT.NONE);
		fileComp.setLayout(new GridLayout(3, false));
		fileComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(fileComp, SWT.NONE);
		l.setText(Messages.FileWizardPage_FileLabel);
		
		txtFile = new Text(fileComp, SWT.BORDER);
		txtFile.setText(""); //$NON-NLS-1$
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String initDir = AssetPlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
		if (initDir != null) txtFile.setText(initDir);
		txtFile.addListener(SWT.Modify, e->{validate();getContainer().updateButtons();});

		
		Button btnBrowse = new Button(fileComp, SWT.PUSH);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addListener(SWT.Selection,e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.FileWizardPage_CsvFile, Messages.FileWizardPage_AllFiles});
			
			if (!txtFile.getText().trim().isEmpty()) fd.setFilterPath(txtFile.getText());
			String file = fd.open();
			if (file != null) txtFile.setText(file);
		});
		
		l = new Label(fileComp, SWT.NONE);
		l.setText(Messages.FileWizardPage_DelimiterLabel);
		
		cmbDelimeter = new DelimiterCombo(fileComp, SWT.NONE);
		cmbDelimeter.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbDelimeter.getControl().addListener(SWT.Selection, e->{validate();getContainer().updateButtons();});
		
		l = new Label(fileComp, SWT.NONE);
		l.setText(Messages.FileWizardPage_SkipLabel);
		l.setToolTipText(Messages.FileWizardPage_SkipTooltip);
		
		btnSkipFirst = new Button(fileComp, SWT.CHECK);
		btnSkipFirst.setSelection(true);
		btnSkipFirst.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		createDateFormatControl();
		
		l = new Label(fileComp, SWT.NONE);
		l.setText(Messages.FileWizardPage_ProjectionLabel);
		l.setToolTipText(Messages.FileWizardPage_ProjectionTooltip);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbProjection = new ComboViewer(fileComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Projection){
					return ((Projection) element).getName();
				}
				return super.getText(element);
			}
		});
		cmbProjection.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbProjection.getCombo().addListener(SWT.Modify, e-> {validate(); getWizard().getContainer().updateButtons();});
		
		setMessage(Messages.FileWizardPage_Message);
		
		setControl(main);
		validate();
		
		loadProjections.schedule();
		
	}

	
	private Job loadProjections = new Job(Messages.FileWizardPage_loadingProjectionJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<Projection> projections = new ArrayList<>();

			try(Session s = HibernateManager.openSession()){
				projections.addAll(HibernateManager.getCaProjectionList(s));
				projections.forEach(p->{
					p.getName();
				});
			}
			projections.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().syncExec(() -> {
				if (!cmbProjection.getControl().isDisposed()){
					cmbProjection.setInput(projections);
					cmbProjection.setSelection(new StructuredSelection(projections.get(0)));
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
}
