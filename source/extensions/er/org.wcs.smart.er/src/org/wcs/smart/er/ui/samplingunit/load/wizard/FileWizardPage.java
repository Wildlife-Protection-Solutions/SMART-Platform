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
package org.wcs.smart.er.ui.samplingunit.load.wizard;

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
import org.wcs.smart.er.internal.Messages;
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

	private String[] fieldsNames = new String[0];
	private ISamplingUnitImporter importer;
	private boolean supportsShp;
	
	/**
	 * 
	 * @param supportsShp if shapefiles can be selected, if false
	 * only deleimited files can be selected
	 */
	public FileWizardPage(boolean supportsShp){
		super("FILE_PAGE"); //$NON-NLS-1$
		this.supportsShp = supportsShp;
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
		l.setText(Messages.FileWizardPage_FileLabel);
		
		txtFile = new Text(c, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				enableDelimiter();	
			}
		});
		
		Button btnFile = new Button(c, SWT.PUSH);
		btnFile.setText("..."); //$NON-NLS-1$
		btnFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell());
				if (supportsShp){
					fd.setFilterExtensions(new String[]{"*.csv;*.shp", "*.csv", "*.shp", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					fd.setFilterNames(new String[]{Messages.FileWizardPage_SupportedFormatLabel, Messages.FileWizardPage_CsvFormatLabel, Messages.FileWizardPage_ShpFormatLabel, Messages.FileWizardPage_AllFilesLabel});
				}else{
					fd.setFilterExtensions(new String[]{"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ 
					fd.setFilterNames(new String[]{Messages.FileWizardPage_CsvFormatLabel, Messages.FileWizardPage_AllFilesLabel});
				}
				String file = fd.open();
				if(file != null){
					txtFile.setText(file);
					enableDelimiter();
				}
			}
		});
		
		//delimiter
		dell = new Label(c, SWT.NONE);
		dell.setText(Messages.FileWizardPage_DelimiterLabel);
		
		delimiter = new DelimiterCombo(c, SWT.BORDER);
		delimiter.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		dell.setEnabled(!supportsShp);
		delimiter.getControl().setEnabled(!supportsShp);
		
		setControl(outer);
		setTitle(Messages.FileWizardPage_Title);
		if (supportsShp){
			setMessage(Messages.FileWizardPage_Message);
		}else{
			setMessage(Messages.FileWizardPage_CsvOnlyMessage);
		}
	}
	
	private void enableDelimiter(){
		if (supportsShp){
			String str = txtFile.getText();
			boolean enabled = !str.endsWith(".shp"); //$NON-NLS-1$
		
			dell.setEnabled(enabled);
			delimiter.getControl().setEnabled(enabled);
		}
	}

	public Character getDelimiter(){
		return new Character( ((Delimiter) ((IStructuredSelection)delimiter.getSelection()).getFirstElement()).value); 
	}
	public ISamplingUnitImporter getImporter(){
		return this.importer;
	}
	
	public File getFile(){
		return new File(txtFile.getText());
	}
	
	public String[] getFieldNames(final HashMap<String, Object> options){
		fieldsNames = null;
		final File f = new File(txtFile.getText());
		final boolean isCsv = delimiter.getControl().getEnabled();
		if (!f.exists()){
			MessageDialog.openInformation(getShell(), Messages.FileWizardPage_ErrorTitle, 
					MessageFormat.format(Messages.FileWizardPage_FileNotFound, new Object[]{f.getAbsolutePath()}));
			return null;
		}
		
		try {
			getWizard().getContainer().run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.FileWizardPage_ReadingFileProgress, 2);
					
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.putAll(options);
					if (!supportsShp || isCsv){
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
						MessageDialog.openInformation(getShell(), Messages.FileWizardPage_ErrorTitle, 
								MessageFormat.format(Messages.FileWizardPage_ErrorReadingFile + "\n\n" + e.getMessage(), new Object[]{f.getAbsolutePath()})); //$NON-NLS-1$
					}
					monitor.worked(1);
					monitor.done();
				}
			});
		} catch (Exception e) {
			MessageDialog.openInformation(getShell(), Messages.FileWizardPage_ErrorTitle, 
					MessageFormat.format(Messages.FileWizardPage_ErrorReadingFile + "\n\n" + e.getMessage(), new Object[]{f.getAbsolutePath()})); //$NON-NLS-1$
		}
		return fieldsNames;
	}
}
