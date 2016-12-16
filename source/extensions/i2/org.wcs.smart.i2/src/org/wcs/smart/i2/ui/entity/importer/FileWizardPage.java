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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
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
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * File wizard page for importer
 * 
 * @author Emily
 *
 */
public class FileWizardPage extends WizardPage{

	public static final String FILE_PAGE = "org.wcs.smart.i2.ui.entity.importer.file";
	
	private Text txtFile;
	private TableComboViewer cmbEntityType;
	private Button chSkipFirstLine;
	private DelimiterCombo delimCombo;
	
	private ComboViewer cmbDateFormat;
	
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
		return (IntelEntityType) ((StructuredSelection)cmbEntityType.getSelection()).getFirstElement() ;
	}
	
	public boolean getSkipFirstLine(){
		return chSkipFirstLine.getSelection();
	}
	
    @Override
	public boolean isPageComplete() {
    	if (!super.isPageComplete()) return false;
    	if (txtFile == null ) return false;
    	
    	//validate file
    	if (!txtFile.getText().isEmpty()){
        	if (!Files.exists(Paths.get(txtFile.getText()))){
        		setErrorMessage("File does not exist");
        		return false;
        	}
    	}
    	
    	if (!cmbEntityType.getSelection().isEmpty() && 
    			!(((StructuredSelection)cmbEntityType.getSelection()).getFirstElement() instanceof IntelEntityType)){
    		setErrorMessage("Invalid entity type");
    		return false;
    	}
    	
    	try{
    		delimCombo.getDelimiter();
    	}catch (Exception ex){
    		setErrorMessage("Invalid delimiter: " + ex.getMessage());
    		return false;
    	}
        
    	
    	try{
    		DateTimeFormatter.ofPattern(cmbDateFormat.getCombo().getText());
    	}catch (Exception ex){
    		setErrorMessage("Invalid date format");
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
		l.setText("Entity Type:");
		l.setToolTipText("the entity type to import");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		cmbEntityType = new TableComboViewer(upper, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbEntityType.addSelectionChangedListener(event-> getWizard().getContainer().updateButtons());
		
		l = new Label(upper, SWT.NONE);
		l.setText("File:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtFile = new Text(upper, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addListener(SWT.Modify, e->getWizard().getContainer().updateButtons());
		
		Button btnFileSelector = new Button(upper, SWT.PUSH);
		btnFileSelector.setText("...");
		btnFileSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.csv", "*.*"});
				fd.setFilterNames(new String[] {"Comma Separated Values (*.csv)", "All Files (*.*)"});
				
				String f = fd.open();
				if (f != null){
					txtFile.setText(f);
				}
				getWizard().getContainer().updateButtons();
			}
		});
		l = new Label(upper, SWT.NONE);
		l.setText("Field Delimiter:");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		delimCombo = new DelimiterCombo(upper, SWT.DEFAULT);
		delimCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		l = new Label(upper, SWT.NONE);
		l.setText("Skip First Line:");
		l.setToolTipText("skip the first line of the csv file");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		chSkipFirstLine = new Button(upper, SWT.CHECK);
		chSkipFirstLine.setSelection(true);
		chSkipFirstLine.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		l = new Label(upper, SWT.NONE);
		l.setText("Date Format:");
		l.setToolTipText("select a format or enter a custom format for date attribute; you can ignore if you have no date attributes to import");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		
		cmbDateFormat = new ComboViewer(upper, SWT.DROP_DOWN);
		cmbDateFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbDateFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbDateFormat.setLabelProvider(new LabelProvider());
		String[] dateFormats = new String[]{
				DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.MEDIUM, null, IsoChronology.INSTANCE, Locale.getDefault()),
				DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, Locale.getDefault()),
				"d/M/y",
				"d-M-y",
				"M/d/y",
				"M-d-y",
				"y/M/d",
				"y-M-d"
		};
		cmbDateFormat.setInput(dateFormats);
		cmbDateFormat.setSelection(new StructuredSelection(dateFormats[0]));
		cmbDateFormat.getCombo().addListener(SWT.Modify, e-> getWizard().getContainer().updateButtons());
		
		loadEntityTypes.schedule();
		setControl(upper);
		
		setTitle("File");
		setMessage("Select the import file and type of entity to import");
	}
	
	private Job loadEntityTypes = new Job("load entity types"){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelEntityType> types = new ArrayList<>();
			Session s = HibernateManager.openSession();
			try{
				types.addAll(s.createCriteria(IntelEntityType.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list());
				types.forEach(t -> t.getName());
			}finally{
				s.close();
			}
			types.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().syncExec(() -> {
				if (!cmbEntityType.getControl().isDisposed()) cmbEntityType.setInput(types);
			});
			return Status.OK_STATUS;
		}
		
	};
}
