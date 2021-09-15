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
package org.wcs.smart.er.xml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.ui.mision.editor.WaypointAttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Class responsible for importing a mission.
 * 
 * @author Emily, Jeff
 * @since 1.0.0, 4.0
 */
public class MissionImporter {
	
	private static final String IMPORTING_MISSION_TASKNAME = Messages.MissionImporter_0;


	/**
	 * Imports mission data from the given file.
	 * 
	 * @param file  a xml or zip file
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @throws Exception
	 */
	public static Mission importMission(Path file, boolean keepIDs, IProgressMonitor monitor) throws Exception{
		return importXmlToMission(file, keepIDs, monitor);
	}
	
	/**
	 * Imports a mission that is an zip file.
	 * 
	 * @param zipFile zip file to import
	 * @param monitor progress monitor
	 * @return mission created or null
	 * @throws Exception
	 */
	private static Mission importXmlToMission(Path zipFile, boolean keepIDs, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, IMPORTING_MISSION_TASKNAME, 4);
		
		progress.split(1);
		
		List<Path> files;
		Path directory;
		
		if (SmartUtils.isZip(zipFile)){
			//unzip 
			directory = ZipUtil.unzip(zipFile);
			if (directory == null || !Files.isDirectory(directory)){
				throw new Exception (MessageFormat.format(Messages.MissionImporter_1, new Object[]{ zipFile.toAbsolutePath().toString()}));
			}
			//file xml file
			try(Stream<Path> stream = Files.list(directory)){
				files = stream.collect(Collectors.toList());
			}
		}else{
			files = Collections.singletonList(zipFile);
			directory = zipFile;
		}
		
		progress.split(1);
		IXmlToMissionConverter converter = null;
		Path xmlFile = null;
	
		//search for xml file
		progress.subTask(Messages.MissionImporter_2);
		for(Path f : files) {
			if (!Files.isDirectory(f)){
				try {
					converter = MissionXmlManager.findXmlConverter(f);
					xmlFile = f;
				}catch (Exception ex) {
					
				}
				if (converter != null) break; //found it
			}
		}
		
		if (converter == null){
			try{
				if(Files.isDirectory(directory)){
					SmartUtils.deleteDirectory(directory);
				}
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
			}
			throw new Exception (Messages.MissionImporter_3);
		}
		
		Mission m = convertAndSave(converter, xmlFile, keepIDs, directory, progress.split(2));
		
		progress.subTask(Messages.MissionImporter_4);
		try{
			if(Files.isDirectory(directory)){
				SmartUtils.deleteDirectory(directory);
			}
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
		}
		return m;
		
	}


	/**
	 * Converts the given xml mission object
	 * into a smart mission object and saves the results
	 * to the database.
	 * <p>Exceptions are throw if un-recoverable errors occur
	 * during the transformation.</p>
	 * <p>Warnings are displayed to the user if some non-required elements
	 * can not be imported.</p>
	 * 
	 * @param xmlMission the xml mission object
	 * @param attachmentDirectory the directory where mission attachments are stored or null if no attachments
	 * @param monitor progress monitor
	 * @return created mission or null
	 * @throws Exception
	 */
	private static Mission  convertAndSave(IXmlToMissionConverter converter, Path file, final boolean keepIDs, Path attachmentDirectory, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, IMPORTING_MISSION_TASKNAME, 3);
		
		try (Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
			progress.split(1);
			progress.subTask(Messages.MissionImporter_5);
			
			converter.fromXml(file, keepIDs, session, SmartDB.getCurrentConservationArea(), attachmentDirectory);
			
			Mission xmlMission = converter.getImportedMission();
			//check if a mission in the database with the given mission id already exists
			if (xmlMission != null && xmlMission.getId() != null){
				if (SurveyHibernateManager.isDuplicateId(xmlMission.getId(), SmartDB.getCurrentConservationArea(), session)){
					
					//duplicate mission id
					final boolean[] cont = new boolean[]{true};
					final String pid = xmlMission.getId();
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							String message = keepIDs ? MessageFormat.format(Messages.MissionImporter_6, pid) : MessageFormat.format(Messages.MissionImporter_7, pid);
							MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.MissionImporter_8, 
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
			progress.subTask(Messages.MissionImporter_9);
			
		}

		progress.split(1);
		
		try(Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor())) {
			List<String> errors = new ArrayList<String>();
			errors.addAll(converter.getValidationErrors());
			//display errors in xml file
			if (!errors.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (String str: errors){
					sb.append(str);
					sb.append(SharedUtils.LINE_SEPARATOR);
				}
				final String message = sb.toString();
				Display.getDefault().syncExec(new Runnable() {

					@Override
					public void run() {
						MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(),Messages.MissionImporter_14,null,message,
								MessageDialog.ERROR, new String[]{IDialogConstants.OK_LABEL}, 1);
						dialog.open();
						return ;
					}
				});
				return null;
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
							Messages.MissionImporter_10,
							Messages.MissionImporter_11,
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
		progress.split(1);
		progress.subTask(Messages.MissionImporter_12);
		Mission imported = converter.getImportedMission();
		
		//performing actual save in database
		try(Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
			session.beginTransaction();
			try {
	
				if (imported == null)
					return null;
				Survey survey = getSurveyById(imported, session);
				if (survey == null){
					//no existing survey, save the new one from the xml file.
					session.saveOrUpdate(imported.getSurvey());
				}else{
					//there is an existing survey with this id, associate the mission with it instead of creating a new one.
					imported.setSurvey(survey);
					survey.getMissions().add(imported);
				}
			
				SurveyHibernateManager.saveMission(imported, session, true);
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				EcologicalRecordsPlugIn.displayLog(Messages.MissionImporter_13 + " " + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
				return null;
			}
		}
		
		return imported;
	}
	
	private static Survey getSurveyById(Mission imported, Session session) {
		return SurveyHibernateManager.getInstance().getSurveyById(session, imported.getSurvey().getId());
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

