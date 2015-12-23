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
package org.wcs.smart.entity.ui;

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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.editor.EntityTypeEditor;
import org.wcs.smart.entity.ui.editor.EntityTypeEditorInput;
import org.wcs.smart.entity.ui.typelist.EntityTypeLabelProvider;
import org.wcs.smart.entity.xml.EntityTypeToXmlConverter;
import org.wcs.smart.entity.xml.EntityTypeXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for exporting entity types.
 * @author Emily
 *
 */
public class ExportEntityTypeHandler {

	@Execute
	public void execute(Shell activeShell, EPartService pService){
		EntityType et = null;
		
		Object part = E3Utils.getSourceObject(pService.getActivePart());
		if (part instanceof EntityTypeEditor){
			IEditorInput input = ((EntityTypeEditor)part).getEditorInput();
			if (input != null && input instanceof EntityTypeEditorInput){
				et = new EntityType();
				et.setUuid(((EntityTypeEditorInput)input).getUuid());
			}
		}
		
		ExportEntityTypeDialog dialog = new ExportEntityTypeDialog(activeShell, et);
		if (dialog.open() != Window.OK) return;
			
		final File exportDir = dialog.getDirectory();
		final List<EntityType> types = dialog.getTypes();
			
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ExportEntityTypeHandler_ProgressLabel, types.size() * 4);
					int exportedCnt = 0;
					Session s = HibernateManager.openSession();
					final boolean[] overwriteall = new boolean[]{false};
					try{
						for (EntityType et : types){
							et = (EntityType) s.load(EntityType.class, et.getUuid());
							monitor.subTask(MessageFormat.format(Messages.ExportEntityTypeHandler_ExportProgress, new Object[]{et.getName()}));
							monitor.worked(1);
							final File exportFile = new File(exportDir, URLUtils.cleanFilename(et.getName()) + ".xml"); //$NON-NLS-1$
							
							if (!overwriteall[0] && exportFile.exists()){
								final boolean[] cont = new boolean[]{true};
								pmd.getShell().getDisplay().syncExec(new Runnable(){

									@Override
									public void run() {
							
										MessageDialog md = new MessageDialog(pmd.getShell(), Messages.ExportEntityTypeHandler_OverwriteOption,
												MessageDialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO),
												MessageFormat.format(Messages.ExportEntityTypeHandler_OverwriteMsg, new Object[]{exportFile.toString()}), 
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
							try(FileOutputStream fout = new FileOutputStream(exportFile)){
								EntityTypeXmlManager.writeDataModel(EntityTypeToXmlConverter.toXml(et, new SubProgressMonitor(monitor, 3)),
									fout);
								exportedCnt++;
							}catch (Exception ex){
								EntityPlugIn.displayLog(Messages.ExportEntityTypeHandler_ExportError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
							}
							
						}
					}finally{
						s.close();
					}
					final int lExportCnt = exportedCnt;
					pmd.getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(pmd.getShell(), Messages.ExportEntityTypeHandler_DialogTitle, 
									MessageFormat.format(Messages.ExportEntityTypeHandler_ExportComplete, new Object[]{lExportCnt, types.size()}));		
							}});
						
					
					monitor.done();
				}
			});
		}catch (Exception ex){
			EntityPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	class ExportEntityTypeDialog extends TitleAreaDialog{

		private static final String LAST_DIR_KEY = "LAST_EXPORT_DIR"; //$NON-NLS-1$
		
		private Text txtFile;
		private EntityType et;
		private File file;
		private CheckboxTableViewer tblEntities;
		private List<EntityType> types;
		
		public ExportEntityTypeDialog(Shell parentShell, EntityType et) {
			super(parentShell);
			this.et = et;
		}
		
		public File getDirectory(){
			return this.file;
		}
		
		public List<EntityType> getTypes(){
			return this.types;
		}
		
		protected void okPressed() {
			file = new File(txtFile.getText());
			types = new ArrayList<EntityType>();
			for (Object x : tblEntities.getCheckedElements()){
				if (x instanceof EntityType){
					types.add((EntityType)x);
				}
			}
			if (!file.exists()){
				if (!MessageDialog.openConfirm(getShell(), Messages.ExportEntityTypeHandler_WarningDialogTitle, 
						MessageFormat.format(Messages.ExportEntityTypeHandler_DirectoryNotExistant, new Object[]{file.toString()}))){
					return;
				}else{
					SmartUtils.createDirectory(file);
				}
			}
			if (!file.isDirectory()){
				MessageDialog.openError(getShell(), Messages.ExportEntityTypeHandler_WarningDialogTitle, 
						MessageFormat.format(Messages.ExportEntityTypeHandler_InvalidDirectory, new Object[]{file.toString()}));
				return;
				
			}
			
			
			EntityPlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, file.toString());
			
			super.okPressed();
		}
		
		protected Control createDialogArea(Composite parent) {
			
			setTitle(Messages.ExportEntityTypeHandler_DialogTitleA);
			setMessage(Messages.ExportEntityTypeHandler_DialogMessage);
			getShell().setText(Messages.ExportEntityTypeHandler_DialogTitleB);
			
			Composite p = (Composite) super.createDialogArea(parent);
			
			Composite contents = new Composite(p, SWT.NONE);
			contents.setLayout(new GridLayout(3, false));
			contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(contents, SWT.NONE);
			l.setText(Messages.ExportEntityTypeHandler_FileLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			txtFile = new Text(contents, SWT.BORDER);
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			String location = EntityPlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
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
			btnBrowse.setText(Messages.ExportEntityTypeHandler_BrowseLabel);
			btnBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DirectoryDialog dd = new DirectoryDialog(getShell());
					dd.setFilterPath(txtFile.getText());
					dd.setMessage(Messages.ExportEntityTypeHandler_OutputDirectory);
					dd.setText(Messages.ExportEntityTypeHandler_ExportLabel);
					
					String f = dd.open();
					if (f != null){
						txtFile.setText(f);
					}
				}
			});
			
			l = new Label(contents, SWT.NONE);
			l.setText(Messages.ExportEntityTypeHandler_TypesLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
			
			tblEntities = CheckboxTableViewer.newCheckList(contents, SWT.BORDER | SWT.MULTI);
			tblEntities.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
			((GridData)tblEntities.getTable().getLayoutData()).heightHint = 300;
			tblEntities.setContentProvider(ArrayContentProvider.getInstance());
			tblEntities.setLabelProvider(new EntityTypeLabelProvider());
			tblEntities.setInput(new Object[]{Messages.ExportEntityTypeHandler_LoadingLabel});
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
					
					final List<EntityType> items = new ArrayList<EntityType>();
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try{
						items.addAll(EntityHibernateManager.getInstance().getEntityTypes(s));
						Collections.sort(items, new Comparator<EntityType>(){
							@Override
							public int compare(EntityType et1, EntityType et2) {
								return Collator.getInstance().compare(et1.getName(), et2.getName());
							}});
					}finally{
						s.getTransaction().rollback();
						s.close();
					}
					getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							tblEntities.setInput(items);
							if (et != null){
								tblEntities.setChecked(et, true);
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
	
	public static class ExportEntityTypeHandlerWrapper extends DIHandler<ExportEntityTypeHandler>{
		public ExportEntityTypeHandlerWrapper(){
			super(ExportEntityTypeHandler.class);
		}
	}
}