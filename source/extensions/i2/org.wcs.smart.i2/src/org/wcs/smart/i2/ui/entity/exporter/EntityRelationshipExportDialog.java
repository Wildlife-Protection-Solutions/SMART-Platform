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
package org.wcs.smart.i2.ui.entity.exporter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.entity.exporter.EntityRelationshipExporter;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.TransparentInfoDialog;

/**
 * Dialog for exporting entities and relationships to csv file.
 * @author Emily
 *
 */
public class EntityRelationshipExportDialog extends TitleAreaDialog{

	private String LAST_DIR_KEY = "org.wcs.smart.i2.ui.entity.exporter.EntityRelationshipExportDialog.DIR";
	private String LAST_DEGREE_KEY = "org.wcs.smart.i2.ui.entity.exporter.EntityRelationshipExportDialog.DEGREE";
	
	private IntelEntity entity;
	
	private Text txtOutput;
	private Text txtDegree;
	
	public EntityRelationshipExportDialog(IntelEntity entity, Shell parentShell) {
		super(parentShell);
		this.entity = entity;
	}
	
	public Control createDialogArea(Composite parent){
		Composite main = (Composite) super.createDialogArea(parent);
		main = new Composite(main, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Entity:");
		
		l = new Label(main, SWT.NONE);
		l.setText(this.entity.getIdAttributeAsText());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(main, SWT.NONE);
		l.setText("Output Directory:");
		l.setToolTipText("Two files will be created in this directory - one for entities and one for relationships");
		
		txtOutput = new Text(main, SWT.BORDER);
		txtOutput.setText(getDefaultDirectory());
		txtOutput.addListener(SWT.Modify, e->validate());
		txtOutput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnBrowse = new Button(main, SWT.PUSH);
		btnBrowse.setText("...");
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnBrowse.addListener(SWT.Selection, e->{
			DirectoryDialog dd = new DirectoryDialog(getShell());
			dd.setFilterPath(txtOutput.getText());
			dd.setText("Output Directory");
			dd.setMessage("Select the directory to write files to.  Two files will be created, one for entities and one for elationships");
			String newFolder = dd.open();
			if (newFolder != null){
				txtOutput.setText(newFolder);
			}
		});
		
		l = new Label(main, SWT.NONE);
		l.setText("Degrees:");
		l.setToolTipText("The number or relationship degrees to include in the export");
		
		txtDegree = new Text(main, SWT.BORDER);
		txtDegree.setText(getDefaultDegree());
		txtDegree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtDegree.addListener(SWT.Modify, e->validate());
		
		setTitle(MessageFormat.format("Export {0}",entity.getIdAttributeAsText()));
		getShell().setText("Export Entity and Relationships");
		setMessage("Export entities and relationships for importing into graphing tools");
		return main;
	}
	
	private boolean validate(){
		try{
			int x = Integer.parseInt(txtDegree.getText());
			if (x <= 0){
				setErrorMessage("Degree must be an integer greater than 0.");	
			}else{
				setErrorMessage(null);
			}
		}catch (Exception ex){
			setErrorMessage("Degree must be an integer greater than 0.");	
		}
		
		if (txtOutput.getText().trim().isEmpty()){
			setErrorMessage("You must select an output folder.");
		}
		
		if (getErrorMessage() == null){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			return true;
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return false;
		}
	}
	
	private String getDefaultDirectory(){
		String dir = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(LAST_DIR_KEY);
		if (dir == null || dir.isEmpty()){
			dir = System.getProperty("user.dir");
		}
		return dir;
	}
	private String getDefaultDegree(){
		int x = Intelligence2PlugIn.getDefault().getPreferenceStore().getInt(LAST_DEGREE_KEY);
		if (x <= 3){
			return "3";
		}
		return String.valueOf(x);
	}

	private void setDefaultValues(){
		Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_DIR_KEY, txtOutput.getText());
		Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_DEGREE_KEY, Integer.parseInt(txtDegree.getText()));
	}
	
	public void okPressed(){
		setDefaultValues();
		if (doExport()){
			super.okPressed();
			
			String message = "Entity/Relationship export complete.";
			TransparentInfoDialog ti = new TransparentInfoDialog(getParentShell(), message);
			ti.open();
		}
	}
	
	private boolean doExport(){
		if (!validate()) return false;
		
		final boolean[] isOk = new boolean[]{false};
		final Path outputDir = Paths.get(txtOutput.getText());
		final int degrees = Integer.parseInt(txtDegree.getText());
		
		if (!Files.exists(outputDir)){
			if (!MessageDialog.openQuestion(getShell(), "Export", MessageFormat.format("The directory {0} does not exist.  Do you want to create it?", outputDir.toString()))){
				return false;
			}
			try {
				Files.createDirectories(outputDir);
			} catch (IOException e) {
				Intelligence2PlugIn.displayLog(MessageFormat.format("Could not create directory {0}: {1}", outputDir.toString(), e.getMessage()), e);;
				return false;
			}
		}
		
		String name = entity.getIdAttributeAsText();
		Path eFile = EntityRelationshipExporter.getEntityFile(outputDir, name);
		Path rFile = EntityRelationshipExporter.getRelationshipFile(outputDir, name);
		
		if (Files.exists(eFile) || Files.exists(rFile)){
			if (!MessageDialog.openQuestion(getShell(), "Export", MessageFormat.format("The files {0} and {1} will be overwritten.  Are you sure you want to continue?", eFile.toString(), rFile.toString()))){
				return false;
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					
					EntityRelationshipExporter exporter = new EntityRelationshipExporter();
					if (!exporter.exportEntity(entity, degrees, outputDir, monitor)){
						isOk[0] = false;
					}else{
						isOk[0] = true;
					}
					
					if (monitor.isCanceled()){
						Display.getDefault().syncExec(()->{
							MessageDialog.openInformation(getShell(), "Cancelled", "Export cancelled.");
						});
						isOk[0] = false;
					}
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog("Error exporting entities and relationships: " + e.getMessage(), e);
			return false;
		}
		return isOk[0];
		
	}
	
}
