
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
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.catalog.URLUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.xml.SurveyDesignToXmlConverter;
import org.wcs.smart.er.xml.SurveyDesignXMLManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
* Handler for exporting survey designs.
* @author Jeff
*
*/

public class SurveyDesignExportHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SurveyDesign sd = null;
		
		if (HandlerUtil.getActiveEditor(event) != null){
			IEditorInput input = HandlerUtil.getActiveEditor(event).getEditorInput();
			if (input != null && input instanceof SurveyDesignEditorInput){
				sd = new SurveyDesign();
				sd.setUuid(((SurveyDesignEditorInput)input).getUuid());
			}
		}
		
		ExportSurveyDesignDialog dialog = new ExportSurveyDesignDialog(HandlerUtil.getActiveShell(event), sd);
		if (dialog.open() != Window.OK){
			return null;
		}
			
		final File exportDir = dialog.getDirectory();
		final List<SurveyDesignEditorInput> types = dialog.getTypes();
			
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(HandlerUtil.getActiveShell(event));
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Exporting Survey Designs", types.size() * 4);
					int exportedCnt = 0;
					Session s = HibernateManager.openSession();
					final boolean[] overwriteall = new boolean[]{false};
					try{
						for (SurveyDesignEditorInput sdei : types){
							SurveyDesign sd = (SurveyDesign) s.load(SurveyDesign.class, sdei.getUuid());
							monitor.subTask(MessageFormat.format("Exporting Survey Design: {0}", new Object[]{sd.getName()}));
							monitor.worked(1);
							final File exportFile = new File(exportDir, URLUtils.cleanFilename(sd.getName()) + ".xml"); //$NON-NLS-1$
							
							if (!overwriteall[0] && exportFile.exists()){
								final boolean[] cont = new boolean[]{true};
								pmd.getShell().getDisplay().syncExec(new Runnable(){

									@Override
									public void run() {
							
										MessageDialog md = new MessageDialog(pmd.getShell(), "Overwrite File?",
												MessageDialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO),
												MessageFormat.format("The file alreasy exists, do you want to overwrite the existing file: {0}", new Object[]{exportFile.toString()}), 
												MessageDialog.INFORMATION,
												new String[]{IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.YES_LABEL, IDialogConstants.SKIP_LABEL},
												0);
									
										int ret = md.open();
										if (ret == 2){
											cont[0] = false;
										}else if (ret == 0){
											overwriteall[0] = true;
										}
									}});
								
								if (!cont[0]){
									continue;
								}
							}
							try{
								FileOutputStream fout = new FileOutputStream(exportFile);
								try{
									SurveyDesignXMLManager.writeDataModel(SurveyDesignToXmlConverter.toXml(sd, s, new SubProgressMonitor(monitor, 3)),
										fout);
									exportedCnt++;
								}finally{
									fout.close();
								}
							}catch (Exception ex){
								EcologicalRecordsPlugIn.displayLog("Error exporting Survey Designs:  {0}" + "\n\n" + ex.getMessage(), ex);
							}
							
						}
					}finally{
						s.close();
					}
					final int lExportCnt = exportedCnt;
					pmd.getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(pmd.getShell(), "Exporting...", 
									MessageFormat.format("Export Complete {0}", new Object[]{lExportCnt, types.size()}));		
							}});
						
					
					monitor.done();
				}
			});
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		return null;
	}
	
	class ExportSurveyDesignDialog extends TitleAreaDialog{

		private static final String LAST_DIR_KEY = "LAST_EXPORT_DIR"; //$NON-NLS-1$
		
		private Text txtFile;
		private SurveyDesign sd;
		private File file;
		private CheckboxTableViewer tblEntities;
		private List<SurveyDesignEditorInput> types;
		
		public ExportSurveyDesignDialog(Shell parentShell, SurveyDesign sd) {
			super(parentShell);
			this.sd = sd;
		}
		
		public File getDirectory(){
			return this.file;
		}
		
		public List<SurveyDesignEditorInput> getTypes(){
			return this.types;
		}
		
		protected void okPressed() {
			file = new File(txtFile.getText());
			types = new ArrayList<SurveyDesignEditorInput>();
			for (Object x : tblEntities.getCheckedElements()){
				if (x instanceof SurveyDesignEditorInput){
					types.add( ((SurveyDesignEditorInput)x) );
				}
			}
			if (!file.exists()){
				if (!MessageDialog.openConfirm(getShell(), "Directory does not exist.", 
						MessageFormat.format("The selected directory does not exist: {0}", new Object[]{file.toString()}))){
					return;
				}else{
					SmartUtils.createDirectory(file);
				}
			}
			if (!file.isDirectory()){
				MessageDialog.openError(getShell(), "Invalid Filename", 
						MessageFormat.format("The file name is already in use as a directory name, please select a different file name.", new Object[]{file.toString()}));
				return;
				
			}
			
			
			EcologicalRecordsPlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, file.toString());
			
			super.okPressed();
		}
		
		protected Control createDialogArea(Composite parent) {
			
			setTitle("Survey Design Export");
			setMessage("Select which Survery Designs you would like to export below.");
			getShell().setText("use the checkboxes below");
			
			Composite p = (Composite) super.createDialogArea(parent);
			
			Composite contents = new Composite(p, SWT.NONE);
			contents.setLayout(new GridLayout(3, false));
			contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(contents, SWT.NONE);
			l.setText("File to export to :");
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			txtFile = new Text(contents, SWT.BORDER);
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			String location = EcologicalRecordsPlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
			if (location == null){
				location = System.getProperty("user.home"); //$NON-NLS-1$
			}
			
			File init = new File(location);
			txtFile.setText(init.getAbsolutePath());
			txtFile.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					getButton(IDialogConstants.OK_ID).setEnabled(txtFile.getText().length() > 0);
				}
			});
			
			Button btnBrowse = new Button(contents, SWT.NONE);
			btnBrowse.setText("Browse:");
			btnBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DirectoryDialog dd = new DirectoryDialog(getShell());
					dd.setFilterPath(txtFile.getText());
					dd.setMessage("Select Output Directory");
					dd.setText("Directory:");
					
					String f = dd.open();
					if (f != null){
						txtFile.setText(f);
					}
				}
			});
			
			l = new Label(contents, SWT.NONE);
			l.setText("Survey Design List:");
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
			
			tblEntities = CheckboxTableViewer.newCheckList(contents, SWT.BORDER | SWT.MULTI);
			tblEntities.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			((GridData)tblEntities.getTable().getLayoutData()).heightHint = 300;
			tblEntities.setContentProvider(ArrayContentProvider.getInstance());
			tblEntities.setLabelProvider(new SurveyDesignLabelProvider());
			tblEntities.setInput(new Object[]{"temporary messages with {0} results"});
			tblEntities.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					getButton(OK).setEnabled(tblEntities.getCheckedElements().length > 0);	
				}
			});
			tblEntities.getTable().addKeyListener(new KeyAdapter() {
				
				@Override
				public void keyPressed(KeyEvent e) {
					if (tblEntities.getSelection().isEmpty()){
						return;
					}
					if (e.keyCode == SWT.SPACE){
						IStructuredSelection selection = ((IStructuredSelection)tblEntities.getSelection());
						selection.getFirstElement();
						boolean value = tblEntities.getChecked(   selection.getFirstElement() );
						for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
							Object tp = (Object) iterator.next();
							tblEntities.setChecked(tp, !value);
						}
						e.doit = false;
						getButton(OK).setEnabled(tblEntities.getCheckedElements().length > 0);	
					}
					
				}
			});
			
			Job j = new Job("loadentities"){ //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					
					final List<SurveyDesignEditorInput> items = new ArrayList<SurveyDesignEditorInput>();
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try{
						items.addAll(SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(s, null ) ); 
						Collections.sort(items, new Comparator<SurveyDesignEditorInput>(){
							@Override
							public int compare(SurveyDesignEditorInput sd1, SurveyDesignEditorInput sd2) {
								return Collator.getInstance().compare(sd1.getName(), sd2.getName());
							}});
					}finally{
						s.getTransaction().rollback();
						s.close();
					}
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							tblEntities.setInput(items);
							if (sd != null){
								tblEntities.setChecked(sd, true);
							}
						}});
					
					return Status.OK_STATUS;
				}};
			j.setSystem(true);
			j.schedule();
			return p;
		}
		
		@Override
		protected boolean isResizable() {
			return true;
		}
		
	}
}