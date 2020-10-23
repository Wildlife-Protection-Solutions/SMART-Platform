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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IIncidentXmlImporter;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Class responsible for importing a incidents.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class IncidentImporter implements IIncidentXmlImporter{
	
	private static final String IMPORTING_INCIDENT_TASKNAME = Messages.IncidentImporter_ImportingTaskName;

	public static final IncidentImporter INSTANCE = new IncidentImporter();
	
	protected IncidentImporter() {
		
	}

	public boolean canImport(Path file) {
		try {
			if (SmartUtils.isZip(file)){
				
				try(ZipFile zip = new ZipFile(file.toFile())){
					
					Iterator<? extends ZipEntry> it = zip.entries().asIterator();
					while(it.hasNext()) {
						ZipEntry ze = it.next();
						
						if (ze.getName().endsWith("xml")) { //$NON-NLS-1$
							try(InputStream is = zip.getInputStream(ze)){
								String ns = IIncidentXmlImporter.findNamespace(is);
	
								boolean ok = supportsNamespace(ns);
								if (ok) return true;
							}
						}
					}
				}
				
				return false;
			}else{
				try(InputStream is = Files.newInputStream(file)){
					String ns = IIncidentXmlImporter.findNamespace(is);
					return supportsNamespace(ns);
				}
			}
		}catch (IOException ex) {
			return false;
		}
	}
	
	private boolean supportsNamespace(String ns) {
		if (ns == null) return false;
		if (ns.equals(org.wcs.smart.incident.xml.model.v20.ObjectFactory._Waypoint_QNAME.getNamespaceURI())) return true;
		if (ns.equals(org.wcs.smart.incident.xml.model.v21.ObjectFactory._Waypoint_QNAME.getNamespaceURI())) return true;
		if (ns.equals(org.wcs.smart.incident.xml.model.v22.ObjectFactory._Waypoint_QNAME.getNamespaceURI())) return true;
		return false;
	}
	/**
	 * Imports incident data from the given file.
	 * 
	 * @param file  a xml or zip file
	 * @param monitor progress monitor 
	 * @throws Exception
	 */
	public Waypoint importIncident(Path file, IProgressMonitor monitor) throws Exception{
		
		if (SmartUtils.isZip(file)){
			//process as zip file
			return importIncidentFromZip(file, monitor);
		}else{
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
	private static Waypoint importIncidentFromZip(Path zipFile, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, IMPORTING_INCIDENT_TASKNAME, 2);
		Path xmlfile = null;
		//unzip 
		progress.subTask(Messages.IncidentImporter_ProcessingzipFile);
		progress.split(1);
		Path directory = ZipUtil.unzip(zipFile);
		if (directory == null || !Files.isDirectory(directory)){
			throw new Exception (MessageFormat.format(Messages.IncidentImporter_ErrorUnzipping, new Object[]{ zipFile.toAbsolutePath().toString()}));
		}
		
		
		//file xml file
		List<Path> files = Files.list(directory).collect(Collectors.toList());
		
		progress.subTask(Messages.IncidentImporter_ReadingXmlProgress);
			
		for(Path f : files) {
			if (!Files.isDirectory(f)) {
				//lets try reading the
				try{
					IncidentXmlManager.findVersion(f);
					xmlfile = f;
				}catch (Exception ex){
					IncidentPlugIn.log(null, ex);
					xmlfile = null;
				}
				if (xmlfile != null){
					//we've found it!
					break;
				}
				
			}
		}
		
		if (xmlfile == null){	
			try{
				SmartUtils.deleteDirectory(directory);
			}catch (Exception ex){
				IncidentPlugIn.log("Error deleting temporary directory", ex); //$NON-NLS-1$
			}
			throw new Exception (Messages.IncidentImporter_XmlNotFound);
		}
		
		Waypoint p = convertAndSave(xmlfile, directory, progress.split(1));
		
		progress.subTask(Messages.IncidentImporter_RemoveTempFiles);
		try{
			SmartUtils.deleteDirectory(directory);
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
	private static Waypoint importIncidentFromFile(Path xmlFile, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, IMPORTING_INCIDENT_TASKNAME, 2);
		return convertAndSave(xmlFile, null, progress.split(1));
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
	private static Waypoint convertAndSave(Path incidentFile, Path attachmentDirectory, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, IMPORTING_INCIDENT_TASKNAME, 2);
		IXmlToIncidentConverter converter = IncidentXmlManager.findVersion(incidentFile);
		
		try (Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			progress.split(1);
			progress.subTask(Messages.IncidentImporter_ConvertingProgress);
			
			//convert
			converter.fromXml(incidentFile, session, SmartDB.getCurrentConservationArea(), attachmentDirectory);

			progress.split(1);
			progress.subTask(Messages.IncidentImporter_ValidatingProgress);
			Waypoint wp = converter.getImportedIncident();
			
			//check if a incident already exists
			Long cnt = QueryFactory.buildCountQuery(session, Waypoint.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"id", wp.getId() }, //$NON-NLS-1$
					new Object[] {"sourceId", IndepedentIncidentSource.KEY}); //$NON-NLS-1$
			if (cnt > 0){
				final boolean[] cont = new boolean[]{true};
				final String  pid = wp.getId();
				Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), Messages.IncidentImporter_ImportDialogTitle, 
									null,
									MessageFormat.format(
											Messages.IncidentImporter_IdDuplicated,new Object[]{pid}),
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
		progress.split(1);
		progress.subTask(Messages.IncidentImporter_SavingProgress);
		Waypoint imported = converter.getImportedIncident();
		if (imported == null)
			return null;
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor())){
			session.beginTransaction();
			try {
				session.save(imported);
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				IncidentPlugIn.displayLog(Messages.IncidentImporter_saveError + ex.getLocalizedMessage(), ex);
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