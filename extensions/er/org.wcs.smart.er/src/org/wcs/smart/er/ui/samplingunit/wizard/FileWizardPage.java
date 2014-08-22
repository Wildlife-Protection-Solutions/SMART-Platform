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
package org.wcs.smart.er.ui.samplingunit.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.ui.samplingunit.load.CsvSamplingUnitImporter;
import org.wcs.smart.er.ui.samplingunit.load.ISamplingUnitImporter;
import org.wcs.smart.er.ui.samplingunit.load.ShpSamplingUnitImporter;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.export.dialog.DelimiterCombo.Delimiter;

/**
 * Sampling unit wizard page; file wizard
 * @author Emily
 *
 */
public class FileWizardPage extends WizardPage {

	private Text txtFile;
	
	private DelimiterCombo delimiter;
	private Label dell;
	
	public FileWizardPage(){
		super("FILE_PAGE");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite c = new Composite(outer, SWT.NONE);
		c.setLayout(new GridLayout(3, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		//file
		Label l = new Label(c, SWT.NONE);
		l.setText("File:");
		
		txtFile = new Text(c, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				enableDelimiter();	
			}
		});
		
		Button btnFile = new Button(c, SWT.PUSH);
		btnFile.setText("...");
		btnFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell());
				
				fd.setFilterExtensions(new String[]{"*.csv;*.shp", "*.csv", "*.shp", "*.*"});
				fd.setFilterNames(new String[]{"Supported Formats (*.csv, *.shp)", "Comma Seperated Values (*.csv)", "Shapefile (*.shp)", "All Files (*.*)"});
				String file = fd.open();
				if(file != null){
					txtFile.setText(file);
					enableDelimiter();
				}
			}
		});
		
		//delimiter
		dell = new Label(c, SWT.NONE);
		dell.setText("Delimiter:");
		
		delimiter = new DelimiterCombo(c, SWT.BORDER);
		delimiter.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		enableDelimiter();
		
		setControl(outer);
		setTitle("Import File");
		setMessage("Select the file to import from.");
	}
	
	private void enableDelimiter(){
		String str = txtFile.getText();
		boolean enabled = !str.endsWith(".shp");
		
		dell.setEnabled(enabled);
		delimiter.getControl().setEnabled(enabled);
	}

	private String[] fieldsNames = new String[0];
	private ISamplingUnitImporter importer;

	public Character getDelimiter(){
		return new Character( ((Delimiter) ((IStructuredSelection)delimiter.getSelection()).getFirstElement()).value); 
	}
	public ISamplingUnitImporter getImporter(){
		return this.importer;
	}
	
	public File getFile(){
		return new File(txtFile.getText());
	}
	
	public String[] getFieldNames(){
		fieldsNames = null;
		final File f = new File(txtFile.getText());
		final boolean isCsv = delimiter.getControl().getEnabled();
		if (!f.exists()){
			MessageDialog.openInformation(getShell(), "Error", 
					MessageFormat.format("File {0} not found.", new Object[]{f.getAbsolutePath()}));
			return null;
		}
		
		final SamplingUnitType type = ((ImportWizard)getWizard()).getSamplingUnitType();
		try {
			getWizard().getContainer().run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Reading file", 2);
					
					HashMap<String, Object> params = new HashMap<String, Object>();
					if (isCsv){
						importer = new CsvSamplingUnitImporter();
						DelimiterCombo.Delimiter delimiterfield = (Delimiter) ((IStructuredSelection)delimiter.getSelection()).getFirstElement();
						params.put(CsvSamplingUnitImporter.DELIMETER_KEY, new Character(delimiterfield.value));
					}else{
						importer = new ShpSamplingUnitImporter();
					}
					monitor.worked(1);
					try {
						fieldsNames = importer.getFieldNames(f, params);
					} catch (Exception e) {
						MessageDialog.openInformation(getShell(), "Error", 
								MessageFormat.format("Error reading file {0}." + "\n\n" + e.getMessage(), new Object[]{f.getAbsolutePath()}));
					}
					monitor.worked(1);
					monitor.done();
				}
			});
		} catch (Exception e) {
			//TODO
			e.printStackTrace();
		}
		return fieldsNames;
	}
}
