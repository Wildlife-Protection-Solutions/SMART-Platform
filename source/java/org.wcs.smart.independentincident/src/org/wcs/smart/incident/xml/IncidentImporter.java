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
package org.wcs.smart.incident.xml;

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
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.xml.model.WaypointType;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for importing a incidents.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class IncidentImporter {
	
	private static final String IMPORTING_INCIDENT_TASKNAME = Messages.IncidentImporter_ImportingTaskName;


	/**
	 * Imports incident data from the given file.
	 * 
	 * @param file  a xml or zip file
	 * @param monitor progress monitor 
	 * @throws Exception
	 */
	public static Waypoint importIncident(File file, IProgressMonitor monitor) throws Exception{
		
		if (SmartUtils.isZip(file)){
			//process as zip file
			monitor.beginTask(IMPORTING_INCIDENT_TASKNAME, 4);
			return importIncidentFromZip(file, monitor);
		}else{
			monitor.beginTask(IMPORTING_INCIDENT_TASKNAME, 3);
			return importIncidentFromFile(file, monitor);
		}
	}
	
	/**
	 * Imports a incident that is an zip file.
	 * 
	 * @param zipFile zip file to import
	 * @param monitor progress monitor
	 * @return incident created or null
	 * @throws Exception
	 */
	private static Waypoint importIncidentFromZip(File zipFile, IProgressMonitor monitor) throws Exception{
		
		WaypointType waypointtype = null;
		//unzip 
		monitor.subTask(Messages.IncidentImporter_ProcessingzipFile);
		File directory = unzip(zipFile);
		if (directory == null || !directory.isDirectory()){
			throw new Exception (MessageFormat.format(Messages.IncidentImporter_ErrorUnzipping, new Object[]{ zipFile.getAbsoluteFile()}));
		}
		monitor.worked(1);
		
		//file xml file
		String[] files = directory.list();
		if (files != null){
			monitor.subTask(Messages.IncidentImporter_ReadingXmlProgress);
			
			for (int i = 0; i < files.length; i ++){
				File f = new File(directory.getAbsoluteFile() + File.separator + files[i]);
				if (f.isFile()){
					//lets try reading the
					try(FileInputStream in = new FileInputStream(f)){
						waypointtype = IncidentXmlManager.readIncident(in);
					}catch (Exception ex){
						IncidentPlugIn.log(null, ex);
						waypointtype = null;
					}
					if (waypointtype != null){
						//we've found it!
						break;
					}
				}
			}
		}
		if (waypointtype == null){
			
			try{
				FileUtils.deleteDirectory(directory);
			}catch (Exception ex){
				IncidentPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
			}
			throw new Exception (Messages.IncidentImporter_XmlNotFound);
		}
		monitor.worked(1);
		
		Waypoint p = convertAndSave(waypointtype, directory, monitor);
		
		monitor.subTask(Messages.IncidentImporter_RemoveTempFiles);
		try{
			FileUtils.deleteDirectory(directory);
		}catch (Exception ex){
			IncidentPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
		}
		return p;
		
	}

	/**
	 * Imports a incident that is a xml file.
	 * @param xmlFile the xml file to import
	 * @return incident created or null
	 * @throws Exception
	 */
	private static Waypoint importIncidentFromFile(File xmlFile, IProgressMonitor monitor) throws Exception{
		WaypointType waypointtype = null;
		try(FileInputStream in = new FileInputStream(xmlFile)){
			monitor.subTask(Messages.IncidentImporter_ReadingXmlProgress);
			waypointtype = IncidentXmlManager.readIncident(in);
			monitor.worked(1);
		}
		if (waypointtype == null){
			throw new Exception(Messages.IncidentImporter_XmlIncidentNotFound);
		}
		return convertAndSave(waypointtype, null, monitor);
	}

	/**
	 * Converts the given xml incident object
	 * into a smart incident object and saves the results
	 * to the database.
	 * <p>Exceptions are throw if un-recoverable errors occur
	 * during the transformation.</p>
	 * <p>Warnings are displayed to the user if some non-required elements
	 * can not be imported.</p>
	 * 
	 * @param xmlIncident the xml Incident object
	 * @param attachmentDirectory the directory where incident attachments are stored or null if no attachments
	 * @param monitor progress monitor
	 * @return created Incident or null
	 * @throws Exception
	 */
	private static Waypoint convertAndSave(WaypointType xmlIncident, File attachmentDirectory, IProgressMonitor monitor) throws Exception {
		XmlToIncident converter = new XmlToIncident();
		Session session = HibernateManager.openSession(new AttachmentInterceptor());
		try {
			monitor.subTask(Messages.IncidentImporter_ValidatingProgress);
			//check if a incident in the database with the given patorl id already exists
			if (xmlIncident.getId() != null){
				Criteria c = session.createCriteria(Waypoint.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.add(Restrictions.eq("id", xmlIncident.getId())) //$NON-NLS-1$
						.add(Restrictions.eq("sourceId", IndepedentIncidentSource.KEY)) //$NON-NLS-1$
						.setProjection(Projections.rowCount()); 
				Long cnt = (Long)c.uniqueResult();
				if (cnt > 0){
					final boolean[] cont = new boolean[]{true};
					final int  pid = xmlIncident.getId();
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.IncidentImporter_ImportDialogTitle, 
									null,
									MessageFormat.format(
											Messages.IncidentImporter_IdDuplicated,new Object[]{String.valueOf(pid)}),
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
			monitor.subTask(Messages.IncidentImporter_ConvertingProgress);
			converter.fromXml(xmlIncident, session, SmartDB.getCurrentConservationArea(), attachmentDirectory);
		} finally {
			if (session.isOpen()){
				session.close();
			}
		}

		List<String> warnings = new ArrayList<String>();
		warnings.addAll(converter.getWarnings());
	
		//display reported conversion warnings if they present
		if (!warnings.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String str: warnings){
				sb.append(str);
				sb.append(SharedUtils.LINE_SEPARATOR);
			}
			final String message = sb.toString();
			final boolean[] cont = new boolean[]{true}; 
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					ConfirmInputDialog dialog = new ConfirmInputDialog(
							Display.getDefault().getActiveShell(),
							Messages.IncidentImporter_OkDialogTitle,
							Messages.IncidentImporter_ErrorMessage,
							message, null);
					if (dialog.open() != ConfirmInputDialog.OK){
						cont[0] = false;

					}	
				}
			});
			if (!cont[0]) {
				return null;
			}

		}
		
		//performing actual save in database
		monitor.subTask(Messages.IncidentImporter_SavingProgress);
		Waypoint imported = converter.getImportedIncident();
		if (imported == null)
			return null;
		session = HibernateManager.openSession(new AttachmentInterceptor());
		session.beginTransaction();
		try {
			session.save(imported);
			session.getTransaction().commit();
		} catch (Exception ex) {
			session.getTransaction().rollback();
			IncidentPlugIn.displayLog(Messages.IncidentImporter_saveError + ex.getLocalizedMessage(), ex);
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
		File tempDir = null;
		try(ZipFile zout = new ZipFile(zipFile)) {
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
					try(InputStream is = zout.getInputStream(entry)){
						FileUtils.copyInputStreamToFile(is, fout);
					}
				}
			}	
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