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
package org.wcs.smart.i2.ui.record.importer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.time.chrono.IsoChronology;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.internal.Messages;
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
	private Button chSkipFirstLine;
	private DelimiterCombo delimCombo;
	private ComboViewer cmbProjection;
	private ComboViewer cmbCharset;
	private ComboViewer cmbDateFormat;
	
	protected FileWizardPage() {
		super(FILE_PAGE);
	}

	public Path getFile(){
		return Paths.get(txtFile.getText());
	}
	
	public char getDelimiter(){
		try {
			return delimCombo.getDelimiter();
		} catch (Exception e) {
		}
		return ',';
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
	
	public String getDateFormatStr(){
		return cmbDateFormat.getCombo().getText();
	}
	
    @Override
	public boolean isPageComplete() {
    	if (!super.isPageComplete()) return false;
    	if (txtFile == null ) return false;
    	
    	//validate file
    	if (!txtFile.getText().isEmpty()){
        	if (!Files.exists(Paths.get(txtFile.getText()))){
        		setErrorMessage(Messages.FileWizardPage1_InvalidFile);
        		return false;
        	}
    	}
    	
    	
    	try{
    		delimCombo.getDelimiter();
    	}catch (Exception ex){
    		setErrorMessage(Messages.FileWizardPage1_InvalidDelimiter + ex.getMessage());
    		return false;
    	}
        
    	Object x= ((IStructuredSelection)cmbProjection.getSelection()).getFirstElement();
    	if ( x == null || !(x instanceof Projection)){
    		setErrorMessage(Messages.FileWizardPage1_InvalidProjection);
    		return false;
    	}
    	
    	setErrorMessage(null);
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
		l.setText(Messages.FileWizardPage1_FileLabel);
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
				fd.setFilterNames(new String[] {Messages.FileWizardPage1_CSVFileType, Messages.FileWizardPage1_allFiles});
				
				String f = fd.open();
				if (f != null){
					txtFile.setText(f);
				}
				getWizard().getContainer().updateButtons();
			}
		});
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage1_DelimiterLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		delimCombo = new DelimiterCombo(upper, SWT.DEFAULT);
		delimCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		Label lblCharset = new Label(upper, SWT.NONE);
		lblCharset.setText(Messages.FileWizardPage_CharSetLbl);
		lblCharset.setToolTipText(Messages.FileWizardPage_CharSetTooltip);
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
		l.setText(Messages.FileWizardPage1_SkipLabel);
		l.setToolTipText(Messages.FileWizardPage1_Skiptooltip);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		chSkipFirstLine = new Button(upper, SWT.CHECK);
		chSkipFirstLine.setSelection(true);
		chSkipFirstLine.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		l = new Label(upper, SWT.NONE);
		l.setText(Messages.FileWizardPage_DateFormatLabel);
		l.setToolTipText(Messages.FileWizardPage1_DateFormatTooltip);
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
		l.setText(Messages.FileWizardPage1_ProjectionLabel);
		l.setToolTipText(Messages.FileWizardPage1_ProjTooltip);
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
		
		setControl(upper);
		loadProjections.schedule();
		setTitle(Messages.FileWizardPage1_Title);
		setMessage(Messages.FileWizardPage1_24);
	}
	
	private Job loadProjections = new Job(Messages.FileWizardPage_LoadTypesJob){
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
