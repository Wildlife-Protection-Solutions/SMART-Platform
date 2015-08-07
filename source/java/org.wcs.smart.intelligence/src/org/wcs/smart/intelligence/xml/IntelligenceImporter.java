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
package org.wcs.smart.intelligence.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.xml.model.IntelligenceType;
import org.wcs.smart.intelligence.xml.model.LabelType;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for importing an intelligence.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceImporter {

	public static Intelligence importIntelligence(File file, IProgressMonitor monitor) throws Exception {
		if (SmartUtils.isZip(file)) {
			//process as zip file
			monitor.beginTask(Messages.IntelligenceImporter_Importing, 4);
			return importXmlToIntelligence(file, monitor);
		} else {
			monitor.beginTask(Messages.ImportIntelligenceHandler_LoadingFile, 3);
			return importIntelligenceFromFile(file, monitor);
		}
	}

	/**
	 * Reads intelligence data from an xml file.
	 * <p>
	 * User is required to close input stream.
	 * </p>
	 * 
	 * @param file input stream to read patrol data from
	 * @return
	 * @throws JAXBException
	 */
	public static IntelligenceType readDataModel(InputStream file) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(IIntelligenceXmlDataConstants.METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		@SuppressWarnings("unchecked")
		JAXBElement<IntelligenceType> o = (JAXBElement<IntelligenceType>) un.unmarshal(file);
		IntelligenceType x = o.getValue();
		return x;
	}
	
	/**
	 * Imports an intelligence that is a xml file.
	 * @param xmlFile the xml file to import
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Intelligence importIntelligenceFromFile(File xmlFile, IProgressMonitor monitor) throws Exception{
		IntelligenceType itype = null;
		try(FileInputStream in = new FileInputStream(xmlFile)){
			monitor.subTask(Messages.IntelligenceImporter_ReadingFile);
			itype = readDataModel(in);
			monitor.worked(1);
		}
		if (itype == null){
			throw new Exception(Messages.IntelligenceImporter_ReadingFile_Error);
		}
		return convertAndSave(itype, null, monitor, xmlFile.getName());
	}

	/**
	 * Imports an Intelligence that is in a zip file.
	 * 
	 * @param zipFile zip file to import
	 * @param monitor progress monitor
	 * @return created intelligence or null
	 * @throws Exception
	 */
	private static Intelligence importXmlToIntelligence(File zipFile, IProgressMonitor monitor) throws Exception {
		
		IntelligenceType itype = null;
		//unzip 
		monitor.subTask(Messages.IntelligenceImporter_ExtractingZip);
		File directory = unzip(zipFile);
		if (directory == null || !directory.isDirectory()){
			throw new Exception (MessageFormat.format(Messages.IntelligenceImporter_ExtractingZip_Error, zipFile.getAbsoluteFile()));
		}
		monitor.worked(1);
		
		//file xml file
		String[] files = directory.list();
		
		monitor.subTask(Messages.IntelligenceImporter_ReadingFile);
		for (int i = 0; i < files.length; i ++){
			File f = new File(directory.getAbsoluteFile() + File.separator + files[i]);
			if (f.isFile()){
				//lets try reading the
				try (FileInputStream in = new FileInputStream(f)){
					itype = readDataModel(in);
				} catch (Exception ex) {
					IntelligencePlugIn.log(null, ex);
					itype = null;
				}
				if (itype != null){
					//we've found it!
					break;
				}
			}
		}
		if (itype == null) {
			try {
				FileUtils.deleteDirectory(directory);
			} catch (Exception ex) {
				IntelligencePlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
			}
			throw new Exception(Messages.IntelligenceImporter_ReadingFile_Error);
		}
		monitor.worked(1);
		
		Intelligence intelligence = convertAndSave(itype, directory, monitor, zipFile.getName());
		
		monitor.subTask(Messages.IntelligenceImporter_RemovingTempFiles);
		try {
			FileUtils.deleteDirectory(directory);
		} catch (Exception ex) {
			IntelligencePlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
		}
		return intelligence;
		
	}
	
	/**
	 * Converts the given xml intelligence object
	 * into a smart intelligence object and saves the results
	 * to the database.
	 * <p>Exceptions are throw if un-recoverable errors occur
	 * during the transformation.</p>
	 * <p>Warnings are displayed to the user if some non-required elements
	 * can not be imported.</p>
	 * 
	 * @param xml the xml intelligence object
	 * @param monitor progress monitor
	 * @return created Patrol or null
	 * @throws Exception
	 */
	private static Intelligence convertAndSave(IntelligenceType xml, File attachmentDirectory, IProgressMonitor monitor, final String filename) throws Exception {
		Session session = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			monitor.subTask(Messages.IntelligenceImporter_Validating);
			//check if a intelligence in the database with the given name already exists
			if (xml.getName() != null) {
				Language language = SmartDB.getCurrentLanguage();
				String name = findNameInLanguage(language, xml.getName());
				if (name == null) {
					//we were unable to find name in current language, try default
					language = SmartDB.getCurrentConservationArea().getDefaultLanguage();
					name = findNameInLanguage(language, xml.getName());
				}
				if (name == null) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.IntelligenceImporter_DuplicateDialog_Title,
									MessageFormat.format(Messages.IntelligenceImporter_FileError1 + "\n\n" + Messages.IntelligenceImporter_Name_NoMatch_Error1, filename)); //$NON-NLS-1$
						}
					});
					return null;
				}
				if (!SmartUtils.isSimpleString(name,
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
						org.wcs.smart.ca.Label.MAX_LENGTH, 1)) {
					final String fname = name;
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.IntelligenceImporter_DuplicateDialog_Title,
									MessageFormat.format(Messages.IntelligenceImporter_FileError2 + "\n\n" + Messages.IntelligenceImporter_InvalidName, fname, org.wcs.smart.ca.Label.MAX_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, filename)); //$NON-NLS-1$
						}
					});
					return null;
				}
				
				Query query = session.createQuery("SELECT l.value FROM Label l, Intelligence i WHERE l.id.language = :language AND lower(l.value) like :name AND l.id.element.uuid = i.uuid"); //$NON-NLS-1$
				query.setParameter("language", language); //$NON-NLS-1$
				query.setParameter("name", name.toLowerCase()); //$NON-NLS-1$
				List<?> result = query.list();
				if (result != null && result.size() > 0){
					final boolean[] cont = new boolean[]{true};
					final String pid = name;
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.IntelligenceImporter_DuplicateDialog_Title, 
									null,
									MessageFormat.format(Messages.IntelligenceImporter_DuplicateDialog_Message, pid),
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
			monitor.subTask(Messages.IntelligenceImporter_Converting);
			XmlToIntelligenceConverter converter = new XmlToIntelligenceConverter();
			converter.fromXml(xml, session, SmartDB.getCurrentConservationArea(), attachmentDirectory);
			monitor.worked(1);
			
			if (converter.getWarnings().size() > 0){
				StringBuilder sb = new StringBuilder();
				for (String str: converter.getWarnings()){
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
								Messages.IntelligenceImporter_WarnDialog_Title,
								MessageFormat.format(Messages.IntelligenceImporter_WarnDialog_Message, filename),
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
			Intelligence imported = converter.getImportedIntelligence();
			
			monitor.subTask(Messages.IntelligenceImporter_Saving);
			if (!IntelligenceHibernateManager.saveIntelligence(imported, session)){
				imported = null;
			}
			monitor.worked(1);
			return imported;
		} finally {
			if (session.isOpen()) {
				session.close();
			}
		}
	}

	private static String findNameInLanguage(Language language, List<LabelType> names) {
		String code = language.getCode();
		String name = null;
		for (LabelType labelType : names) {
			if (code.equals(labelType.getLanguageCode())) {
				name = labelType.getValue();
				break;
			}
		}
		return name;
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

class ConfirmInputDialog extends InputDialog {

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
