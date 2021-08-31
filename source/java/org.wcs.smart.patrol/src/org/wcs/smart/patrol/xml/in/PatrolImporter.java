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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
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
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

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
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	 * to call done() on the given monitor
	 * @throws Exception
	 */
	public static Patrol importPatrol(Path file, ImportConfig config, IProgressMonitor monitor) throws Exception{
		
		if (SmartUtils.isZip(file)){
			//process as zip file
			return importXmlToPatrol(file, config, monitor);
		}else{
			return importPatrolFromFile(file, config, monitor);
		}
	}
	
	/**
	 * Imports a patrol that is an zip file.
	 * 
	 * @param zipFile zip file to import
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	 * to call done() on the given monitor
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Patrol importXmlToPatrol(Path zipFile, ImportConfig config, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, IMPORTING_PATROL_TASKNAME, 3);
		progress.subTask(Messages.PatrolImporter_Progress_ProcessingFile);
		progress.split(1);
		Path directory = ZipUtil.unzip(zipFile);
		if (directory == null || !Files.isDirectory(directory)){
			throw new Exception (MessageFormat.format(Messages.PatrolImporter_Error_UnzipError, new Object[]{ zipFile.toAbsolutePath().toString()}));
		}
		
		
		//file xml file
		progress.split(1);
		IXmlToPatrolConverter converter = null;
		Path xmlFile = null;
		List<Path> files = null;
		try(Stream<Path> stream = Files.list(directory)){
			files = stream.collect(Collectors.toList());
		}
		progress.subTask(Messages.PatrolImporter_Progress_ReadingFile);
		for (Path f : files) {
			if (!Files.isDirectory(f)){
				//lets try reading the 
				converter = PatrolXmlManager.findVersion(f);
				if (converter != null){
					//we've found the patrol xml file; otherwise we probably found an attachment file
					xmlFile = f;
					break;
				}
			}
		}
		if (converter == null){
			try{
				SmartUtils.deleteDirectory(directory);
			}catch (Exception ex){
				SmartPatrolPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
			}
			throw new Exception (Messages.PatrolImporter_Error_XmlFileNotFound);
		}
		
		Patrol p = convertAndSave(converter, config, directory, xmlFile, progress.split(1));
		
		progress.subTask(Messages.PatrolImporter_Progress_RemovingTempFiles);
		try{
			SmartUtils.deleteDirectory(directory);
		}catch (Exception ex){
			SmartPatrolPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
		}
		return p;
		
	}

	/**
	 * Imports a patrol that is a xml file.
	 * @param xmlFile the xml file to import
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	 * to call done() on the given monitor
	 * @return patrol created or null
	 * @throws Exception
	 */
	private static Patrol importPatrolFromFile(Path xmlFile, ImportConfig config, IProgressMonitor monitor) throws Exception{
		IXmlToPatrolConverter converter = PatrolXmlManager.findVersion(xmlFile);
		if (converter == null){
			throw new Exception(MessageFormat.format(Messages.PatrolImporter_UnableToProcessFile, xmlFile));
		}
		return convertAndSave(converter, config, null, xmlFile, monitor);
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
	private static Patrol  convertAndSave(IXmlToPatrolConverter converter, final ImportConfig config, 
			Path attachmentDirectory, Path sourceFile, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.PatrolImporter_ReadingProgress, 5);
		
		
		try(Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor())) {
			progress.split(1);
			converter.convertFile(sourceFile, session, SmartDB.getCurrentConservationArea(), attachmentDirectory);
			
			Patrol imported = converter.getImportedPatrol();
			if (!config.isKeepIDs()){
				imported.setId(null);
			}
			
			progress.split(1);
			progress.subTask(Messages.PatrolImporter_Progress_Validating);
			//check if a patrol in the database with the given patrol id already exists
			if (imported.getId() != null){
				
				if (!SmartUtils.isSimpleString(imported.getId(), 
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Patrol.MAX_ID_LENGTH) ) {
					throw new Exception(MessageFormat.format(Messages.XmlToPatrolConverter_InvalidPatrolId,
							imported.getId(), Patrol.MAX_ID_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc));
				}
				
				if (PatrolHibernateManager.isDuplicateId(imported.getId(), SmartDB.getCurrentConservationArea(), session)){
					
					if (!config.isIgnoreWarnings()) {
						//duplicate patrol id
						final boolean[] cont = new boolean[]{true};
						final String pid = imported.getId();
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
					} else {
						//duplicate patrol id but user chose to ignore warnings
						String pid = imported.getId();
						String fileName = sourceFile.getFileName().toString();
						String message = config.isKeepIDs() ? MessageFormat.format(Messages.PatrolImporter_Warn_SameId, imported.getId(), fileName) : MessageFormat.format(Messages.PatrolImporter_Warn_DataDuplicate, pid, fileName);
						config.addWarning(message, sourceFile);
					}
				}
			}	
		}
		progress.split(1);
		progress.subTask(Messages.PatrolImporter_Progress_ConvertingPatrol);
		
		List<String> warnings = new ArrayList<String>();
		warnings.addAll(converter.getWarnings());
		//converting extra data
		List<IConvertedExtraData> convertedExtraData = converter.convertExtraData(attachmentDirectory);
		
		
		//display reported conversion warnings if they present
		progress.split(1);
		if (!config.isIgnoreWarnings() && !warnings.isEmpty()) {
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
		progress.split(1);
		progress.subTask(Messages.PatrolImporter_Progress_Saving);
		Patrol imported = converter.getImportedPatrol();
		if (imported == null)
			return null;
		
		try(Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
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
			}
		}
		
		return imported;
	}
	
	
}


class ConfirmInputDialog extends SmartStyledInputDialog{

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