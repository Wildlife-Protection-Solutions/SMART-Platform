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
package org.wcs.smart.er.ui.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.SurveyDesignListView;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditor;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.xml.SurveyDesignFromXmlConverter;
import org.wcs.smart.er.xml.SurveyDesignXMLManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.FieldDataPerspective;

/**
 * Handler for importing entity types.
 * @author Jeff
 *
 */

public class SurveyDesignImportHandler extends AbstractHandler {

	private String errorMessage = null;
	private Exception exception = null;
	private SurveyDesign newDesign = null;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ImportEntityTypeDialog dialog = new ImportEntityTypeDialog(HandlerUtil.getActiveShell(event));
		if (dialog.open() != Window.OK){
			return null;
		}
			
		final File importFile = dialog.getFile();
	
		errorMessage = null;
		exception = null;
		newDesign = null;
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.SurveyDesignImportHandler_0, 100);
					
					monitor.subTask(Messages.SurveyDesignImportHandler_1);
					org.wcs.smart.er.xml.model.surveydesign.SurveyDesign xmlsd = null;
					try{
						FileInputStream fin = new FileInputStream(importFile);
						try{
							xmlsd = SurveyDesignXMLManager.readDataModel(fin);
						}finally{
							fin.close();
						}
					}catch (Exception ex){
						errorMessage = Messages.SurveyDesignImportHandler_2;
						exception = ex;
						return;
					}
					monitor.worked(50);
					
					monitor.subTask(Messages.SurveyDesignImportHandler_3);
				
					Session session = HibernateManager.openSession();
					try{
						SurveyDesign sd = SurveyDesignFromXmlConverter.fromXml(xmlsd, session);
						
						//ensure key doesn't already exist
						List<?> existingDesigns = session.createCriteria(SurveyDesign.class)
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))  //$NON-NLS-1$
								.add(Restrictions.eq("keyId", sd.getKeyId())).list(); //$NON-NLS-1$
						if (existingDesigns.size() > 0){
							errorMessage = MessageFormat.format(Messages.SurveyDesignImportHandler_4, new Object[]{sd.getKeyId()});
							return;
						}
						//save to the database
						try{						
							session.beginTransaction();
												
							//update/add new mission properties	
							for( MissionProperty mp : sd.getMissionProperties()){
								session.saveOrUpdate(mp.getAttribute());
							}
							

							//update/add new sampling unit attributes
							for (SurveyDesignSamplingUnitAttribute sdua: sd.getSamplingUnitAttributes()){
								session.saveOrUpdate(sdua.getSamplingUnitAttribute());
							}
							
							//save survey design
							session.saveOrUpdate(sd);
							
							//save sampling Units
							List<SamplingUnit> units =  SurveyDesignFromXmlConverter.getSamplingUnits(xmlsd, sd, session);
							for (SamplingUnit su : units){
								session.saveOrUpdate(su);
							}

							session.getTransaction().commit();
							newDesign = sd;
						}catch (Exception ex){
							session.getTransaction().rollback();
							throw ex;
						}
					}catch(ParseException parse){
						errorMessage = parse.getMessage();
						exception = parse;
						return;
					}catch(Exception ex){
						errorMessage = "An Error occured when loading Survey Design: " + "\n\n" + ex.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
						exception = ex;
						return;
					}finally{
						session.close();
					}
					
					
					if (newDesign != null){
						SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.SURVEY_DESIGN_ADDED, newDesign);
					}
				}
			});
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
			}
		
		
		if (errorMessage != null || exception != null){
			EcologicalRecordsPlugIn.displayLog(errorMessage != null ? errorMessage : exception.getMessage(), exception);
		}else if (newDesign != null){
			//open editor
			try {
				FieldDataPerspective.openPerspective(SurveyDesignListView.ID);
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new SurveyDesignEditorInput(newDesign.getName(), newDesign.getUuid(), newDesign.getKeyId(), newDesign.getState()), SurveyDesignEditor.ID);
			} catch (PartInitException e) {

			}
			
		}
		return null;
	}
	
	class ImportEntityTypeDialog extends TitleAreaDialog{

		private static final String LAST_DIR_KEY = "LAST_IMPORT_DIR"; //$NON-NLS-1$
		
		private Text txtFile;
		private File file;
		
		public ImportEntityTypeDialog(Shell parentShell) {
			super(parentShell);
		}
		
		public File getFile(){
			return this.file;
		}
		
		protected void okPressed() {
			file = new File(txtFile.getText());
			if (!file.exists()) {
				MessageDialog.openError(getShell(), Messages.SurveyDesignImportHandler_8, 
						MessageFormat.format(Messages.SurveyDesignImportHandler_9,new Object[] { file.toString() }));
				return;
			}
			EcologicalRecordsPlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, file.toString());

			super.okPressed();
		}
		
		protected Control createDialogArea(Composite parent) {
			
			setTitle(Messages.SurveyDesignImportHandler_10);
			setMessage(Messages.SurveyDesignImportHandler_11);
			getShell().setText(Messages.SurveyDesignImportHandler_10);
			
			Composite p = (Composite) super.createDialogArea(parent);
			
			Composite contents = new Composite(p, SWT.NONE);
			contents.setLayout(new GridLayout(3, false));
			contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(contents, SWT.NONE);
			l.setText(Messages.SurveyDesignImportHandler_13);
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			txtFile = new Text(contents, SWT.BORDER);
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			
			String location = EcologicalRecordsPlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
			if (location != null){
				txtFile.setText(location);
			}else{
				txtFile.setText(""); //$NON-NLS-1$
			}
			
			txtFile.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					getButton(IDialogConstants.OK_ID).setEnabled(txtFile.getText().length() > 0);
				}
			});
			
			Button btnBrowse = new Button(contents, SWT.NONE);
			btnBrowse.setText(Messages.SurveyDesignImportHandler_14);
			btnBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
					
					String ext = "xml"; //$NON-NLS-1$
					String name= Messages.SurveyDesignImportHandler_15;
					
					String[] extensions = new String[]{"*." + ext, "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
					String[] names = new String[]{name + " (*." + ext + ")", Messages.SurveyDesignImportHandler_16}; //$NON-NLS-1$ //$NON-NLS-2$
					
					fd.setFilterExtensions(extensions);
					fd.setFilterNames(names);
					
					fd.setFilterPath(txtFile.getText());
					fd.setFileName(txtFile.getText());
					
					String f = fd.open();
					if (f != null) {
						txtFile.setText(f);
					}
				}
			});
			
			
			return p;
		}
		
	}
	

}