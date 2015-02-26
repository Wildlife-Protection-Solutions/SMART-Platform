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
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
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
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.xml.EntityTypeFromXmlConverter;
import org.wcs.smart.entity.xml.EntityTypeXmlManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Handler for importing entity types.
 * @author Emily
 *
 */
public class ImportEntityTypeHandler{

	@Execute
	public void execute(Shell activeShell, final IEclipseContext context){
		ImportEntityTypeDialog dialog = new ImportEntityTypeDialog(activeShell);
		if (dialog.open() != Window.OK) return;
		
			
		final File importFile = dialog.getFile();
		final MWindow activeWindow = context.get(MWindow.class);
		final Object[] returnInfo = new Object[3]; //0 = errormessage; 1 = exception; 2 = newType
		
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ImportEntityTypeHandler_ImportProgress1, 100);
					
					monitor.subTask(Messages.ImportEntityTypeHandler_ImportProgress2);
					org.wcs.smart.entity.xml.model.EntityType xmlEntityType = null;
					try(FileInputStream fin = new FileInputStream(importFile)){
						xmlEntityType = EntityTypeXmlManager.readDataModel(fin);
					}catch (Exception ex){
						returnInfo[0] = Messages.ImportEntityTypeHandler_XmlError;
						returnInfo[1] = ex;
						return;
					}
					monitor.worked(50);
					
					monitor.subTask(Messages.ImportEntityTypeHandler_ImportProgress3);
				
					Session session = HibernateManager.openSession();
					try{
						EntityType et = EntityTypeFromXmlConverter.fromXml(xmlEntityType, session);
						
						//ensure key doesn't already exist
						List<?> types = session.createCriteria(EntityType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).add(Restrictions.eq("keyId", et.getKeyId())).list(); //$NON-NLS-1$ //$NON-NLS-2$
						if (types.size() > 0){
							returnInfo[0] = MessageFormat.format(Messages.ImportEntityTypeHandler_ErrorDuplicateKey, new Object[]{et.getKeyId()});
							return;
						}
						
						Set<String> processed = new HashSet<String>();
						processed.add(et.getDmAttribute().getKeyId());	//this will automatically be added
						StringBuilder sb = new StringBuilder();
						for (EntityAttribute ea : et.getAttributes()){
							if (ea.getDmAttribute().getUuid() == null && !processed.contains(ea.getDmAttribute().getKeyId())){
								//this is a new attribute
								sb.append(ea.getDmAttribute().getName());
								sb.append(", "); //$NON-NLS-1$
								processed.add(ea.getDmAttribute().getKeyId());
							}
						}
						
						if (sb.length() > 0){
							sb.deleteCharAt(sb.length()-1);
							sb.deleteCharAt(sb.length()-1);
							
							final String importValidation = MessageFormat.format(Messages.ImportEntityTypeHandler_AddedAttributesWarn,new Object[]{sb.toString()});
							final boolean[] cont = new boolean[]{false};
							pmd.getShell().getDisplay().syncExec(new Runnable(){

								@Override
								public void run() {
									cont[0] = MessageDialog.openConfirm(pmd.getShell(), Messages.ImportEntityTypeHandler_ImportDialogTitle, importValidation);
								}});
							if (!cont[0]){
								return;
							}
						}
							
							//save to the database
						session.beginTransaction();
						try{
							session.saveOrUpdate(et.getDmAttribute());
							for (EntityAttribute ea : et.getAttributes()){
								session.saveOrUpdate(ea.getDmAttribute());
							}
							session.saveOrUpdate(et);
							session.getTransaction().commit();
							returnInfo[2] = et;
						}catch (Exception ex){
							session.getTransaction().rollback();
							throw ex;
						}
						
					}catch(ParseException parse){
						returnInfo[0] = parse.getMessage();  
						returnInfo[1] = parse;
						return;
					}catch(Exception ex){
						returnInfo[0] = Messages.ImportEntityTypeHandler_ImportError + "\n\n" + ex.getMessage(); //$NON-NLS-1$  
						returnInfo[1] = ex;
						return;
					}finally{
						session.close();
					}
					
					
					if (returnInfo[2] != null){
						pmd.getShell().getDisplay().syncExec(new Runnable(){

							@Override
							public void run() {
								EntityType newType = (EntityType) returnInfo[2];
								
								//fire events
								EntityEventManager.getInstance().fireEvent(EntityEventManager.ENTITY_TYPE_ADDED, newType);
								//fire data model modified events
								DataModelManager.getInstance().fireChangeListeners();
								
								//open
								(new OpenEntityTypeHandler()).openEntityType(newType, activeWindow);
						}});
					}
				}
			});
			}catch (Exception ex){
				EntityPlugIn.log(ex.getMessage(), ex);
			}
		
		
		if (returnInfo[0] != null || returnInfo[1] != null){
			EntityPlugIn.displayLog(returnInfo[0] != null ? (String)returnInfo[0]: ((Throwable)returnInfo[1]).getMessage(), (Throwable)returnInfo[1]);
		}
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

			if (!file.exists()){
				MessageDialog.openError(getShell(), Messages.ImportEntityTypeHandler_ErrorDialogTitle, 
						MessageFormat.format(Messages.ImportEntityTypeHandler_FileNotFoundError, new Object[]{file.toString()}));
				return;
			}
			EntityPlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, file.toString());
			
			super.okPressed();
		}
		
		protected Control createDialogArea(Composite parent) {
			
			setTitle(Messages.ImportEntityTypeHandler_FileDialogTitle);
			setMessage(Messages.ImportEntityTypeHandler_FileDialogMessage);
			getShell().setText(Messages.ImportEntityTypeHandler_FileDialogText);
			
			Composite p = (Composite) super.createDialogArea(parent);
			
			Composite contents = new Composite(p, SWT.NONE);
			contents.setLayout(new GridLayout(3, false));
			contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(contents, SWT.NONE);
			l.setText(Messages.ImportEntityTypeHandler_FileLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			txtFile = new Text(contents, SWT.BORDER);
			txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			
			String location = EntityPlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
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
			btnBrowse.setText(Messages.ImportEntityTypeHandler_BrowseLabel);
			btnBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
					
					String ext = "xml"; //$NON-NLS-1$
					String name= Messages.ImportEntityTypeHandler_XMLFileType;
					
					String[] extensions = new String[]{"*." + ext, "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
					String[] names = new String[]{name + " (*." + ext + ")", Messages.ImportEntityTypeHandler_AllFiles}; //$NON-NLS-1$ //$NON-NLS-2$
					
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
	
	public static class ImportEntityTypeHandlerWrapper extends DIHandler<ImportEntityTypeHandler>{
		public ImportEntityTypeHandlerWrapper(){
			super(ImportEntityTypeHandler.class);
		}
	}
}