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

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.AssetXmlToAssetData;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Wizard page for collecting xml file details and importing xml data.
 * 
 * @author Emily
 *
 */
public class XmlFileWizardPage extends WizardPage {
	
	public static final String PREFERENCE_DIR_KEY = XmlFileWizardPage.class.getCanonicalName() + ".dir";  //$NON-NLS-1$
	
	private Text txtFile;
	
	protected XmlFileWizardPage() {
		super("XML_FILE_PAGE");
	}

	@Override
	public IWizardPage getNextPage() {
		return null;
	}
	
	public Path getFile() {
		return Paths.get(txtFile.getText());
	}
	
	public boolean doFinish() {
		Path xmlFile = getFile();
		AssetPlugIn.getDefault().getPreferenceStore().putValue(PREFERENCE_DIR_KEY, xmlFile.toString());
		if (!Files.exists(xmlFile)) {
			MessageDialog.openWarning(getShell(), "Not Found", MessageFormat.format("File {0} not found", xmlFile.toString()));
		}
		final boolean[] ok = new boolean[] {true};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true,  true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					AssetXmlToAssetData dd = new AssetXmlToAssetData(SmartDB.getCurrentConservationArea());
					try {
						dd.importXmlData(xmlFile, monitor);
					}catch(OperationCanceledException ex) {
						Display.getDefault().syncExec(()-> MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Canceled", "Operation canceled by user"));
					}catch (Exception ex) {
						AssetPlugIn.displayLog("Error importing asset model data: "  + ex.getMessage(), ex);
						ok[0] = false;
					}
				}
			});
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Error importing  asset model data: "  + ex.getMessage(), ex);
			ok[0] = false;
		}
		return ok[0];
		
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite fileComp = new Composite(main, SWT.NONE);
		fileComp.setLayout(new GridLayout(3, false));
		fileComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(fileComp, SWT.NONE);
		l.setText("File:");
		
		txtFile = new Text(fileComp, SWT.BORDER);
		txtFile.setText("");
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		String initDir = AssetPlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
		if (initDir != null) txtFile.setText(initDir);
		
		Button btnBrowse = new Button(fileComp, SWT.PUSH);
		btnBrowse.setText("...");
		btnBrowse.addListener(SWT.Selection,e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[] {"*.xml", "*.*"});
			fd.setFilterNames(new String[] {"XML File (*.xml)", "All Files (*.*)"});
			
			if (!txtFile.getText().trim().isEmpty()) fd.setFilterPath(txtFile.getText());
			String file = fd.open();
			if (file != null) txtFile.setText(file);
		});
		
		setTitle("Import Asset Data");
		setMessage("Select xml file to import");
		
		setControl(main);
	}

}
