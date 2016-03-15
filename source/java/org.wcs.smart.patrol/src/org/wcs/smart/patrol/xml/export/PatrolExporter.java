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
package org.wcs.smart.patrol.xml.export;

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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.xml.PatrolToXmlConverter;
import org.wcs.smart.patrol.xml.PatrolXmlManager;
import org.wcs.smart.patrol.xml.XmlExtraDataContributionFactory;
import org.wcs.smart.patrol.xml.external.IXmlExtraDataContribution;
import org.wcs.smart.patrol.xml.model.ExtraDataType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Class responsible for exporting
 * a patrol to an xml file.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolExporter {

	
	/**
	 * Writes a simple patrol to the provided file.
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
	 * the patrol object to the session, converts the object,
	 * and writes the results to a file.</p>
	 * 
	 * @param patrol patrol to export
	 * @param file file to write to
	 * @param includeAttachments if attachments should be included
	 * @param monitor progress monitor 
	 * @return the file written the file written
	 * 
	 * @throws Exception 
	 */
	public static File exportPatrol(Patrol patrol, File file, boolean includeAttachments, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.PatrolExporter_Progress_Exporting, includeAttachments ? 4 : 2);
		Session session = HibernateManager.openSession();
		try {
			session.refresh(patrol);
			
			monitor.subTask(Messages.PatrolExporter_Progress_Converting);
			PatrolType xml = PatrolToXmlConverter.toXml(patrol);
			for (IXmlExtraDataContribution edc : XmlExtraDataContributionFactory.getContributions()) {
				List<ExtraDataType> extraData = edc.exportData(patrol);
				if (extraData != null) {
					xml.getExtraData().addAll(extraData);
				}
			}
			monitor.worked(1);
			
			if (!includeAttachments){
				return exportPatrolWithoutAttachments(xml, file, monitor);
			}else{
				return exportPatrolWithAttachments(patrol, xml, file, monitor);
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
	private static File exportPatrolWithoutAttachments(PatrolType xml, File file, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.PatrolExporter_Progress_WritingToFile);
		try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
			PatrolXmlManager.writeDataModel(xml, out);
		}
		monitor.worked(1);
		return file;
	}
	
	/**
	 * Writes the patrol including attachments
	 */
	private static File exportPatrolWithAttachments(Patrol patrol, PatrolType xml, File f, IProgressMonitor monitor) throws Exception{
		int index = f.getName().lastIndexOf('.');
		String name = f.getName();
		if (index >= 0){
			name= name.substring(0, index);
		}
		File xmlFile = File.createTempFile("temp_patrol_export", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		exportPatrolWithoutAttachments(xml, xmlFile, monitor);
		
		
		monitor.subTask(Messages.PatrolExporter_Progress_PackagingResults);
		//create zip file
		File zipFile = new File(f.getParent() + File.separator + name + ".zip"); //$NON-NLS-1$
		
		try(ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile))) {
			zout.setLevel(Deflater.DEFAULT_COMPRESSION);

			/* add xml file to zip */
			zout.putNextEntry(new ZipEntry(name	+ ".xml")); //$NON-NLS-1$
			
			try(FileInputStream inStream = new FileInputStream(xmlFile)){
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = inStream.read(buffer)) > 0) {
					zout.write(buffer, 0, bytesRead);
				}
			}
			monitor.worked(1);

			/* add all attachments */
			List<ISmartAttachment> allAttach = new ArrayList<ISmartAttachment>();
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay legday : leg.getPatrolLegDays()) {
					for (PatrolWaypoint wp : legday.getWaypoints()) {
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
			}
			for (ISmartAttachment att : allAttach){
				File attFile = att.getFullFile();
				zout.putNextEntry(new ZipEntry(PatrolXmlManager.ATTACHMENT_DIR_NAME + ZipUtil.DIR_PATH_SEPERATOR + att.getFilename()));
				try(FileInputStream inStream = new FileInputStream(attFile)) {
					byte[] buffer = new byte[1024];
					int bytesRead;
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
        	SmartPatrolPlugIn.log(null, ex);
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
