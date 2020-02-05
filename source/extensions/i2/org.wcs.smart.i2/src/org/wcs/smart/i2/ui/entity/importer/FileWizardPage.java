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
package org.wcs.smart.i2.ui.entity.importer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * File wizard page for importer
 * 
 * @author Emily
 *
 */
public class FileWizardPage extends WizardPage{

	public static final String FILE_PAGE = "org.wcs.smart.i2.ui.entity.importer.file"; //$NON-NLS-1$
	
	private Text txtFile;
	private TableComboViewer cmbEntityType;
	private TableComboViewer cmbProfile;
	private Button chSkipFirstLine;
	private DelimiterCombo delimCombo;
	private ComboViewer cmbCharset;
	private ComboViewer cmbDateFormat;
	private ComboViewer cmbProjection;
	
	protected FileWizardPage() {
		super(FILE_PAGE);
	}

	public Path getFile(){
		return Paths.get(txtFile.getText());
	}
	
	public String getDateFormatStr(){
		return cmbDateFormat.getCombo().getText();
	}
	
	public char getDelimiter(){
		try {
			return delimCombo.getDelimiter();
		} catch (Exception e) {
		}
		return ',';
	}
	public IntelEntityType getEntityType(){
		return (IntelEntityType) cmbEntityType.getStructuredSelection().getFirstElement();
	}
	
	public IntelProfile getProfile() {
		return (IntelProfile)cmbProfile.getStructuredSelection().getFirstElement();
	}
	public boolean getSkipFirstLine(){
		return chSkipFirstLine.getSelection();
	}
	
	public Projection getProjection(){
		return (Projection) ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
	}
	
	public Charset getCharacterSet(){
		return (Charset) ((IStructuredSelection)cmbCharset.getSelection()).getFirstElement();
	}
	
	
    @Override
	public boolean isPageComplete() {
    	if (!super.isPageComplete()) return false;
    	if (txtFile == null ) return false;
    	
    	//validate file
    	if (!txtFile.getText().isEmpty()){
        	if (!Files.exists(Paths.get(txtFile.getText()))){
        		setErrorMessage(Messages.FileWizardPage_FileNotFound);
        		return false;
        	}
    	}
    	
    	if (!cmbEntityType.getSelection().isEmpty() && 
    			!(((StructuredSelection)cmbEntityType.getSelection()).getFirstElement() instanceof IntelEntityType)){
    		setErrorMessage(Messages.FileWizardPage_InvalidEntityType);
    		return false;
    	}
    	if (!cmbProfile.getSelection().isEmpty() && 
    			!(((StructuredSelection)cmbProfile.getSelection()).getFirstElement() instanceof IntelProfile)){
    		setErrorMessage(Messages.FileWizardPage_profileRequired);
    		return false;
    	}
    	try{
    		delimCombo.getDelimiter();
    	}catch (Exception ex){
    		setErrorMessage(Messages.FileWizardPage_InvalidDelimiter + ex.getMessage());
    		return false;
    	}
        
    	Object x= ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
    	if ( x == null || !(x instanceof Projection)){
    		setErrorMessage(Messages.FileWizardPage_InvalidProjection);
    		return false;
    	}
    	
    	try{
    		DateTimeFormatter.ofPattern(cmbDateFormat.getCombo().getText());
    	}catch (Exception ex){
    		setErrorMessage(Messages.FileWizardPage_InvalidDateFormat);
    		return false;
    	}
    	
    	setErrorMessage(null);
    	if (cmbEntityType.getSelection().isEmpty()){
    		return false;
     	}
    	if (txtFile.getText().isEmpty()){
    		 return false;
    	 }
    	return true;
    }
    
	@Override
	public void createControl(Composite parent) {
		Composite upper = new Composite(parent, SWT.NONE);
		upper.setLayout(new GridLayout(3, false));
		
		Label l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_ETLabel);
		l.setToolTipText(Messages.FileWizardPage_ETtooltip);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		cmbEntityType = new TableComboViewer(upper, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		SmartUiUtils.configure(cmbEntityType);
		cmbEntityType.addSelectionChangedListener(event-> {
			Object x = cmbEntityType.getStructuredSelection().getFirstElement();
			if (x instanceof IntelEntityType) {
				IntelEntityType t = (IntelEntityType)x;
				cmbProfile.setInput(t.getProfiles());
				cmbProfile.setSelection(new StructuredSelection(t.getProfiles().iterator().next()));
			}else {
				cmbProfile.setInput(new String[] {Messages.FileWizardPage_SelectEntityTypeMsg});
			}
			getWizard().getContainer().updateButtons();
		});
		
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_ProfileLbl);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		cmbProfile = new TableComboViewer(upper, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbProfile.setContentProvider(ArrayContentProvider.getInstance());
		cmbProfile.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IntelProfile) return ((IntelProfile)element).getName();
				return super.getText(element);
			}
			public Image getImage(Object element) {
				if (element instanceof IntelProfile) return Resources.INSTANCE.getImage((IntelProfile)element);
				return null;
			}
		});
		
		cmbProfile.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		SmartUiUtils.configure(cmbProfile);
		cmbProfile.addSelectionChangedListener(event-> getWizard().getContainer().updateButtons());
		
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_FileLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtFile = new Text(upper, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addListener(SWT.Modify, e->getWizard().getContainer().updateButtons());
		
		Button btnFileSelector = new Button(upper, SWT.PUSH);
		btnFileSelector.setText("..."); //$NON-NLS-1$
		btnFileSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[] {Messages.FileWizardPage_CSVFileName, Messages.FileWizardPage_AllFileName});
				
				String f = fd.open();
				if (f != null){
					txtFile.setText(f);
				}
				getWizard().getContainer().updateButtons();
			}
		});
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_DelimiterLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		delimCombo = new DelimiterCombo(upper, SWT.DEFAULT);
		delimCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		Label lblCharset = new Label(upper, SWT.NONE);
		lblCharset.setText(Messages.FileWizardPage1_CharSetLbl);
		lblCharset.setToolTipText(Messages.FileWizardPage1_CharSetTooltip);
		lblCharset.setBackground(upper.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbCharset = new ComboViewer(upper, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbCharset.setContentProvider(ArrayContentProvider.getInstance());
		cmbCharset.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Charset)element).displayName();
			}
		});
		cmbCharset.setInput( Charset.availableCharsets().values() );
		cmbCharset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		Charset defaultcs = StandardCharsets.UTF_8;
		try {
			String cc = SmartPlugIn.getDefault().getDialogSettings().get(SmartPlugIn.DEFAULT_ENCODING_KEY);
			if (cc != null && !cc.isBlank()) defaultcs = Charset.forName(cc);
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		cmbCharset.setSelection(new StructuredSelection(defaultcs));
		cmbCharset.addSelectionChangedListener(e->{
			SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_ENCODING_KEY, ((Charset)e.getStructuredSelection().getFirstElement()).name());
		});
		cmbCharset.getControl().setBackground(upper.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_SkipLineLabel);
		l.setToolTipText(Messages.FileWizardPage_SkipLinkTooltip);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		chSkipFirstLine = new Button(upper, SWT.CHECK);
		chSkipFirstLine.setSelection(true);
		chSkipFirstLine.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_DateFormat);
		l.setToolTipText(Messages.FileWizardPage_dateformattooltip);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		
		cmbDateFormat = new ComboViewer(upper, SWT.DROP_DOWN);
		cmbDateFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbDateFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbDateFormat.setLabelProvider(new LabelProvider());
		String[] dateFormats = new String[]{
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
		cmbDateFormat.getCombo().addListener(SWT.Modify, e-> getWizard().getContainer().updateButtons());
		
		
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_ProjectionLabel);
		l.setToolTipText(Messages.FileWizardPage_projectiontooltip);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbProjection = new ComboViewer(upper, SWT.DROP_DOWN | SWT.READ_ONLY);
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
		cmbProjection.getCombo().addListener(SWT.Modify, e-> getWizard().getContainer().updateButtons());
		
		loadEntityTypes.schedule();
		setControl(upper);
		
		setTitle(Messages.FileWizardPage_FileLabel2);
		setMessage(Messages.FileWizardPage_FileTooltip);
	}
	
	private Job loadEntityTypes = new Job(Messages.FileWizardPage_LoadTypeJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelEntityType> types = new ArrayList<>();
			final List<Projection> projections = new ArrayList<>();

			try(Session s = HibernateManager.openSession()){
				
				types.addAll(QueryFactory.buildQuery(s, IntelEntityType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list()); //$NON-NLS-1$
				types.forEach(t -> {
					t.getName();
					t.getProfiles().forEach(ip->ip.getProfile().getName());
				});
				
				projections.addAll(HibernateManager.getCaProjectionList(s));
				projections.forEach(p->{
					p.getName();
				});
			}
			types.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			projections.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().syncExec(() -> {
				if (!cmbEntityType.getControl().isDisposed()) cmbEntityType.setInput(types);
				if (!cmbProjection.getControl().isDisposed()){
					cmbProjection.setInput(projections);
					cmbProjection.setSelection(new StructuredSelection(projections.get(0)));
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
}
