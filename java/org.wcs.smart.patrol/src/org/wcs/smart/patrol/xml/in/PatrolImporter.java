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
package org.wcs.smart.patrol.xml.in;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.patrol.xml.PatrolXmlManager;
import org.wcs.smart.patrol.xml.XmlExtraDataContributionFactory;
import org.wcs.smart.patrol.xml.XmlToPatrolConverter;
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;
import org.wcs.smart.patrol.xml.external.IXmlExtraDataContribution;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for importing a patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolImporter {
	
	private static final String IMPORTING_PATROL_TASKNAME = Messages.PatrolImporter_Progress_TaskName;


	/**
	 * Imports patrol data from the given file.
	 * 
	 * @param file  a xml or zip file
	 * @param monitor progress monitor 
	 * @throws Exception
	 */
	public static Patrol importPatrol(File file, ImportConfig config, IProgressMonitor monitor) throws Exception{
		
		if (SmartUtils.isZip(file)){
			//process as zip file
			monitor.beginTask(IMPORTING_PATROL_TASKNAME, 4);
			return importXmlToPatrol(file, config, monitor);
		}else{
			monitor.beginTask(IMPORTING_PATROL_TASKNAME, 3);
			return importPatrolFromFile(file, config, monitor);
		}
	}
	
	/**
	 * Imports a patrol that is an zip file.
	 * 
	 * @param zipFile zip file to import
	 * @param monitor progress monitor
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Patrol importXmlToPatrol(File zipFile, ImportConfig config, IProgressMonitor monitor) throws Exception{
		
		PatrolType ptype = null;
		//unzip 
		monitor.subTask(Messages.PatrolImporter_Progress_ProcessingFile);
		File directory = unzip(zipFile);
		if (directory == null || !directory.isDirectory()){
			throw new Exception (MessageFormat.format(Messages.PatrolImporter_Error_UnzipError, new Object[]{ zipFile.getAbsoluteFile()}));
		}
		monitor.worked(1);
		
		//file xml file
		String[] files = directory.list();
		
		monitor.subTask(Messages.PatrolImporter_Progress_ReadingFile);
		for (int i = 0; i < files.length; i ++){
			File f = new File(directory.getAbsoluteFile() + File.separator + files[i]);
			if (f.isFile()){
				//lets try reading the 
				FileInputStream in = new FileInputStream(f);
				try{
					ptype = PatrolXmlManager.readDataModel(in);
				}catch (Exception ex){
					SmartPatrolPlugIn.log(null, ex);
					ptype = null;
				}finally{
					in.close();
				}
				if (ptype != null){
					//we've found it!
					break;
				}
			}
		}
		if (ptype == null){
			
			try{
				FileUtils.deleteDirectory(directory);
			}catch (Exception ex){
				SmartPatrolPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
			}
			throw new Exception (Messages.PatrolImporter_Error_XmlFileNotFound);
		}
		monitor.worked(1);
		
		Patrol p = convertAndSave(ptype, config, directory, zipFile, monitor);
		
		monitor.subTask(Messages.PatrolImporter_Progress_RemovingTempFiles);
		try{
			FileUtils.deleteDirectory(directory);
		}catch (Exception ex){
			SmartPatrolPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
		}
		return p;
		
	}

	/**
	 * Imports a patrol that is a xml file.
	 * @param xmlFile the xml file to import
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Patrol importPatrolFromFile(File xmlFile, ImportConfig config, IProgressMonitor monitor) throws Exception{
		PatrolType ptype = null;
		FileInputStream in = new FileInputStream(xmlFile);
		try{
			monitor.subTask(Messages.PatrolImporter_Progress_ReadingXml);
			ptype = PatrolXmlManager.readDataModel(in);
			monitor.worked(1);
		}finally{
			in.close();
		}
		if (ptype == null){
			throw new Exception(Messages.PatrolImporter_Error_ReadingPatrolXmlFile);
		}
		return convertAndSave(ptype, config, null, xmlFile, monitor);
	}

	/**
	 * Converts the given xml patrol object
	 * into a smart patrol object and saves the results
	 * to the database.
	 * <p>Exceptions are throw if un-recoverable errors occur
	 * during the transformation.</p>
	 * <p>Warnings are displayed to the user if some non-required elements
	 * can not be imported.</p>
	 * 
	 * @param xmlPatrol the xml Patrol object
	 * @param attachmentDirectory the directory where patrol attachments are stored or null if no attachments
	 * @param monitor progress monitor
	 * @return created Patrol or null
	 * @throws Exception
	 */
	private static Patrol  convertAndSave(PatrolType xmlPatrol, final ImportConfig config, 
			File attachmentDirectory, File sourceFile, IProgressMonitor monitor) throws Exception {
		XmlToPatrolConverter converter = new XmlToPatrolConverter();
		Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		try {
			monitor.subTask(Messages.PatrolImporter_Progress_Validating);
			//check if a patrol in the database with the given patrol id already exists
			if (xmlPatrol.getId() != null){
				if (PatrolHibernateManager.isDuplicateId(xmlPatrol.getId(), SmartDB.getCurrentConservationArea(), session)){
					
					//duplicate patrol id
					final boolean[] cont = new boolean[]{true};
					final String pid = xmlPatrol.getId();
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							String message = config.isKeepIDs() ? MessageFormat.format(Messages.PatrolImporter_ConfirmationMessage_SameId, pid) : MessageFormat.format(Messages.PatrolImporter_ConfirmationMessage, pid);
							MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.PatrolImporter_ImportPatrol_DialogTitle, 
									null, message,
									MessageDialog.QUESTION, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
							int ret = dialog.open();
							if (ret == 1){
								//do not import
								cont[0] = false;
								return ;
							}		
						}

					});
					if(!cont[0]){
						return null;
					}
				}
			}		
			monitor.subTask(Messages.PatrolImporter_Progress_ConvertingPatrol);
			converter.fromXml(xmlPatrol, config.isKeepIDs(), session, SmartDB.getCurrentConservationArea(), attachmentDirectory);
		} finally {
			if (session.isOpen()){
				session.close();
			}
		}

		List<String> warnings = new ArrayList<String>();
		warnings.addAll(converter.getWarnings());
		//converting extra data
		List<IConvertedExtraData> convertedExtraData = new ArrayList<IConvertedExtraData>();
		for (IXmlExtraDataContribution edc : XmlExtraDataContributionFactory.getContributions()) {
			IConvertedExtraData extraData = edc.fromXml(xmlPatrol.getExtraData());
			if (extraData != null) {
				if (extraData.getWarnings() != null) {
					warnings.addAll(extraData.getWarnings());
				}
				convertedExtraData.add(extraData);
			}
		}
		monitor.worked(1);

		//display reported conversion warnings if they present
		if (!config.isIgnoreWarnings() && !warnings.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String str: warnings){
				sb.append(str);
				sb.append(SmartUtils.LINE_SEPARATOR);
			}
			final String message = sb.toString();
			final boolean[] cont = new boolean[]{true}; 
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					ConfirmInputDialog dialog = new ConfirmInputDialog(
							Display.getDefault().getActiveShell(),
							Messages.PatrolImporter_PatrolImport_ErrorDialogTitle,
							Messages.PatrolImporter_PatrolImport_ErrorMessage,
							message, null);
					if (dialog.open() != ConfirmInputDialog.OK){
						cont[0] = false;

					}	
				}
			});
			if (!cont[0]) {
				return null;
			}

		} else {
			config.addWarnings(warnings, sourceFile);
		}
		
		//performing actual save in database
		monitor.subTask(Messages.PatrolImporter_Progress_Saving);
		Patrol imported = converter.getImportedPatrol();
		if (imported == null)
			return null;
		session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		session.beginTransaction();
		try {
			PatrolHibernateManager.savePatrol(imported, session, true);
			for (IConvertedExtraData extraData : convertedExtraData) {
				extraData.saveInTransaction(session, imported);
			}
			session.getTransaction().commit();
		} catch (Exception ex) {
			session.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog(Messages.PatrolHibernateManager_Error_CouldNoSavePatrol + ex.getLocalizedMessage(), ex);
			return null;
		}finally{
			session.close();
		}
		monitor.worked(1);
		return imported;
	}
	
	/**
	 * Unzips the content of the zip
	 * file to a temporary directory.
	 * 
	 * @param zipFile the zip file to unzip
	 * @return the location of the files
	 * @throws Exception
	 */
	private static File unzip(File zipFile) throws Exception{
		ZipFile zout = new ZipFile(zipFile);
		
		File tempDir = null;
		try {
			tempDir = File.createTempFile(zipFile.getName(),
					Long.toString(System.nanoTime()));
			tempDir.delete();
			tempDir.mkdir();
		
			Enumeration<? extends ZipEntry> elements = zout.entries();
			while(elements.hasMoreElements()){
				ZipEntry entry = elements.nextElement();
				
				File fout = new File(tempDir.getAbsoluteFile() + File.separator + entry.getName());
				if (entry.isDirectory()){
					FileUtils.forceMkdir(fout);
				}else{
					InputStream is = zout.getInputStream(entry);
					try{
						FileUtils.copyInputStreamToFile(is, fout);
					}finally{
						is.close();
					}
				}
			}
			
		} finally {
			zout.close();
		}
		return tempDir;
	}	
}


class ConfirmInputDialog extends InputDialog{

	/**
	 * @param parentShell
	 * @param dialogTitle
	 * @param dialogMessage
	 * @param initialValue
	 * @param validator
	 */
	public ConfirmInputDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String initialValue,
			IInputValidator validator) {
		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
		
		
	}
	
	@Override
	public int getInputTextStyle(){
		return SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite res = (Composite) super.createDialogArea(parent);
		((GridData)(res.getChildren()[0]).getLayoutData()).grabExcessVerticalSpace = false;
		
		//((GridData)this.getText().getLayoutData()).heightHint = 200;
		((GridData)this.getText().getLayoutData()).widthHint = 500;
		((GridData)this.getText().getLayoutData()).grabExcessVerticalSpace = true;
		((GridData)this.getText().getLayoutData()).verticalAlignment = SWT.FILL;
		this.getText().setEditable(false);
		
		((GridData)parent.getLayoutData()).heightHint = 400;
		return res;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}