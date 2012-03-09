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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.PatrolXmlManager;
import org.wcs.smart.patrol.xml.XmlToPatrolConverter;
import org.wcs.smart.patrol.xml.model.PatrolType;

/**
 * Class responsible for importing a patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolImporter {
	
	/**
	 * Imports patrol data from the given file.
	 * 
	 * @param file  a xml or zip file
	 * @param monitor progress monitor 
	 * @throws Exception
	 */
	public static Patrol importPatrol(File file, IProgressMonitor monitor) throws Exception{
		
		if (isZip(file)){
			//process as zip file
			monitor.beginTask("Importing Patrol", 4);
			return importXmlToPatrol(file, monitor);
		}else{
			monitor.beginTask("Importing Patrol", 3);
			return importPatrolFromFile(file, monitor);
		}
	}
	
	/**
	 * Determine if the given file is a zip file or not.
	 * 
	 * @param file the file to check
	 * @return <code>true</code> if file is zip file, <code>false</code> otherwise
	 */
	private static boolean isZip(File file){
		try{
			ZipFile zout = new ZipFile(file);
			zout.entries();
			zout.close();
			return true;
		}catch (Exception ex){
			
		}
		return false;
		
	}
	
	/**
	 * Imports a patrol that is an zip file.
	 * 
	 * @param zipFile zip file to import
	 * @param monitor progress monitor
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Patrol importXmlToPatrol(File zipFile, IProgressMonitor monitor) throws Exception{
		
		PatrolType ptype = null;
		//unzip 
		monitor.subTask("Processing zip file.");
		File directory = unzip(zipFile);
		if (directory == null || !directory.isDirectory()){
			throw new Exception ("Error unzipping file " + zipFile.getAbsoluteFile());
		}
		monitor.worked(1);
		
		//file xml file
		String[] files = directory.list();
		
		monitor.subTask("Reading xml file.");
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
				SmartPatrolPlugIn.log("Error deleting temporary directory", ex);
			}
			throw new Exception ("Patrol xml file not found in zip file.");
		}
		monitor.worked(1);
		
		Patrol p = convertAndSave(ptype, directory, monitor);
		
		monitor.subTask("Removing temporary files.");
		try{
			FileUtils.deleteDirectory(directory);
		}catch (Exception ex){
			SmartPatrolPlugIn.log("Error deleting temporary directory", ex);
		}
		return p;
		
	}

	/**
	 * Imports a patrol that is a xml file.
	 * @param xmlFile the xml file to import
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Patrol importPatrolFromFile(File xmlFile, IProgressMonitor monitor) throws Exception{
		PatrolType ptype = null;
		FileInputStream in = new FileInputStream(xmlFile);
		try{
			monitor.subTask("Reading XML");
			ptype = PatrolXmlManager.readDataModel(in);
			monitor.worked(1);
		}catch (Exception ex){
			ptype = null;
		}finally{
			in.close();
		}
		return convertAndSave(ptype, null, monitor);
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
	private static Patrol  convertAndSave(PatrolType xmlPatrol, File attachmentDirectory, IProgressMonitor monitor)
			throws Exception {
		Session session = HibernateManager.openSession();
		try{
			monitor.subTask("Converting");
			XmlToPatrolConverter converter = new XmlToPatrolConverter();
			converter.fromXml(xmlPatrol, session, SmartDB.getCurrentConservationArea(), attachmentDirectory);
			monitor.worked(1);
			
			if (converter.getWarnings().size() > 0){
				StringBuilder sb = new StringBuilder();
				for (String str: converter.getWarnings()){
					sb.append(str);
					sb.append(System.getProperty("line.separator"));
				}
				ConfirmInputDialog dialog = new ConfirmInputDialog(
						Display.getCurrent().getActiveShell(),
						"Patrol Import",
						"Some data could not be imported.  The following list identifies the problems the occurred while importing the patrol data.  If you continue the data identified below will not be imported.  Would you like to continue with the import?",
						sb.toString(), null);
				if (dialog.open() != ConfirmInputDialog.OK){
					return null;
				}			
			}
			Patrol imported = converter.getImportedPatrol();
			
			monitor.subTask("Saving");
			if (!PatrolHibernateManager.savePatrol(imported, session)){
				imported = null;
			}
			monitor.worked(1);
			return imported;
		}finally{
			if (session.isOpen()){
				session.close();
			}
		}
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
		Control res = super.createDialogArea(parent);
		((GridData)this.getText().getLayoutData()).heightHint = 200;
		((GridData)this.getText().getLayoutData()).widthHint = 500;
		this.getText().setEditable(false);
		return res;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}