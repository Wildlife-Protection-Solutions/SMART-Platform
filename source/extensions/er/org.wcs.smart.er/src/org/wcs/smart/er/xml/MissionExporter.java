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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.xml.model.missions.MissionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for exporting
 * a mission to an xml file.
 * 
 * @author Jeff
 * @since 4.0.0
 */
public class MissionExporter {

	/**
	 * Writes a simple mission to the provided file.
	 * 
	 * <p>Does not check to determine if
	 * the file already exists.  That check must be done outside this
	 * function.
	 * </p>
	 * <p>If the attachments are to be included then a
	 * zip file is created with the xml file and all attachments
	 * </p>
	 * 
	 * <p>This function opens a hibernate session; attaches
	 * the mission object to the session, converts the object,
	 * and writes the results to a file.</p>
	 * 
	 * @param mission mission to export
	 * @param file file to write to
	 * @param includeAttachments if attachments should be included
	 * @param monitor progress monitor 
	 * @return the file written the file written
	 * 
	 * @throws Exception 
	 */
	public static File exportMission(Mission mission, File file, boolean includeAttachments, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.MissionExporter_0, includeAttachments ? 4 : 2);
		Session session = HibernateManager.openSession();
		try {
			session.refresh(mission);
	
			monitor.subTask(Messages.MissionExporter_1);
			MissionType xml = MissionToXmlConverter.toXml(mission);
			
			monitor.worked(1);
			
			if (!includeAttachments){
				return exportMissionWithoutAttachments(xml, file, monitor);
			}else{
				for (MissionDay md : mission.getMissionDays()){
					for (SurveyWaypoint wp : md.getWaypoints()){
						ObservationHibernateManager.computeAttachmentLocations(wp.getWaypoint(), session);
					}
				}
				return exportMissionWithAttachments(mission, xml, file, monitor);
			}
		} finally {
			if (session.isOpen()) {
				session.close();
			}
		}
	}
	
	
	/**
	 * Writes the patrol without including attachments
	 */
	private static File exportMissionWithoutAttachments(MissionType xml, File file, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.MissionExporter_2);
		try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
			MissionXmlManager.writeDataModel(xml, out);
		}
		monitor.worked(1);
		return file;
	}
	
	/**
	 * Writes the patrol including attachments
	 */
	private static File exportMissionWithAttachments(Mission mission, MissionType xml, File f, IProgressMonitor monitor) throws Exception{
		int index = f.getName().lastIndexOf('.');
		String name = f.getName();
		if (index >= 0){
			name= name.substring(0, index);
		}
		File xmlFile = File.createTempFile("temp_patrol_export", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		exportMissionWithoutAttachments(xml, xmlFile, monitor);
		
		
		monitor.subTask(Messages.MissionExporter_3);
		//create zip file
		File zipFile = new File(f.getParent() + File.separator + name + ".zip"); //$NON-NLS-1$
		try(ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile))) {
			zout.setLevel(Deflater.DEFAULT_COMPRESSION);

			/* add xml file to zip */
			zout.putNextEntry(new ZipEntry(name	+ ".xml")); //$NON-NLS-1$

			byte[] buffer = new byte[1024];
			int bytesRead;
			try(FileInputStream inStream = new FileInputStream(xmlFile)) {
				while ((bytesRead = inStream.read(buffer)) > 0) {
					zout.write(buffer, 0, bytesRead);
				}
			}
			
			monitor.worked(1);

			/* add all attachments */
			List<ISmartAttachment> allAttach = new ArrayList<ISmartAttachment>();
			for (MissionDay md : mission.getMissionDays()){
				for (SurveyWaypoint wp : md.getWaypoints()) {
					if (wp.getWaypoint().getAttachments() != null){
						allAttach.addAll(wp.getWaypoint().getAttachments());
					}
					for (WaypointObservation wo : wp.getWaypoint().getObservations()){
						if (wo.getAttachments() != null){
							allAttach.addAll(wo.getAttachments());
						}
					}
				}
			}
			for (ISmartAttachment att : allAttach){
				File attFile = att.getAttachmentFile();
				zout.putNextEntry(new ZipEntry(MissionXmlManager.ATTACHMENT_DIR_NAME + File.separator + att.getFilename()));

				
				try(FileInputStream inStream = new FileInputStream(attFile)) {
					while ((bytesRead = inStream.read(buffer)) > 0) {
						zout.write(buffer, 0, bytesRead);
					}
				}
			}
		}
        monitor.worked(1);
        
        try{
        	//delete temp file
        	xmlFile.delete();
        }catch(Exception ex){
        	EcologicalRecordsPlugIn.log(null, ex);
        }
        
        return zipFile;
	}
	
	/**
	 * Determines the output file name for patrol export
	 * based on given input file
	 * and if attachments are included.
	 * 
	 * @param dir
	 * @param name
	 * @param includeAttributes
	 * @return
	 */
	public static File getOutputFile(File dir, String name, boolean includeAttachs) throws Exception {
		name = SmartUtils.getFileName(name);
		String ext = includeAttachs ? ".zip" : ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		return new File(dir, name + ext);
	}
	
}

