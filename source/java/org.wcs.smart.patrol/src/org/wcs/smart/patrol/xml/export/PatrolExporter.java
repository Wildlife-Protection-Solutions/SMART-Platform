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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.ObservationHibernateManager;
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
import org.wcs.smart.patrol.xml.external.IXmlExtraDataContribution.PatrolXmlContribution;
import org.wcs.smart.patrol.xml.model.v14.PatrolType;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;
import org.wcs.smart.util.Zipper;

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
	public static Path exportPatrol(Patrol patrol, Path file, boolean includeAttachments, Map<Object,Object> options, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.PatrolExporter_Progress_Exporting2, includeAttachments ? 4 : 2);
		
		List<Path> additionalFiles = new ArrayList<>();
		
		List<PatrolXmlContribution> extras = new ArrayList<>();
		
		Session session = HibernateManager.openSession();
		try {
			session.refresh(patrol);
			
			monitor.subTask(Messages.PatrolExporter_Progress_Converting2);
			PatrolType xml = PatrolToXmlConverter.toXml(patrol);
			
			
			for (IXmlExtraDataContribution edc : XmlExtraDataContributionFactory.getContributions()) {
				PatrolXmlContribution extraData = edc.exportData(patrol, options);
				if (extraData != null) {
					extras.add(extraData);
					xml.getExtraData().addAll(extraData.getExtraData());
					additionalFiles.addAll(extraData.getExtraFiles());
				}
			}
			monitor.worked(1);
			
			if (!includeAttachments && additionalFiles.isEmpty()){
				return exportPatrolWithoutAttachments(xml, file, monitor);
			}else if (!includeAttachments && !additionalFiles.isEmpty()) {
				return exportPatrolWithAttachments(patrol, xml, file, false, additionalFiles, monitor);
			}else{
				for (PatrolLeg pl : patrol.getLegs()){
					for (PatrolLegDay pld : pl.getPatrolLegDays()){
						for (PatrolWaypoint wp : pld.getWaypoints()){
							ObservationHibernateManager.computeAttachmentLocations(wp.getWaypoint(), session);
						}
					}
				}
				
				return exportPatrolWithAttachments(patrol, xml, file, true, additionalFiles, monitor);
			}
		} finally {
			if (session.isOpen()) {
				session.close();
			}
			
			for (PatrolXmlContribution c : extras) {
				c.cleanUp();
			}
		}
	}
	
	
	/**
	 * Writes the patrol without including attachments
	 */
	private static Path exportPatrolWithoutAttachments(PatrolType xml, Path file, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.PatrolExporter_Progress_WritingToFile2);
		try(BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
			PatrolXmlManager.writeDataModel(xml, out);
		}
		monitor.worked(1);
		return file;
	}
	
	/**
	 * Writes the patrol including attachments
	 */
	private static Path exportPatrolWithAttachments(Patrol patrol, PatrolType xml, Path f, 
			boolean includePatrolAttachments,
			List<Path> additionalFiles, IProgressMonitor monitor) throws Exception{
		int index = f.getFileName().toString().lastIndexOf('.');
		String name = f.getFileName().toString();
		if (index >= 0){
			name= name.substring(0, index);
		}
		Path xmlFile = Files.createTempFile("temp_patrol_export", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		exportPatrolWithoutAttachments(xml, xmlFile, monitor);
		
		
		monitor.subTask(Messages.PatrolExporter_Progress_PackagingResults);
		//create zip file
		Path zipFile = f.getParent().resolve(name + ".zip"); //$NON-NLS-1$
		
		Zipper zipper = Zipper.create(zipFile);
		try {
			
			zipper.addFile(xmlFile, name + ".xml"); //$NON-NLS-1$
			monitor.worked(1);

			//add additional files
			for (Path p : additionalFiles) {
				zipper.addFile(p, p.getFileName().toString());				
			}
			
			if (includePatrolAttachments) {
				/* add all attachments */
				List<ISmartAttachment> allAttach = new ArrayList<ISmartAttachment>();
				for (PatrolLeg leg : patrol.getLegs()) {
					for (PatrolLegDay legday : leg.getPatrolLegDays()) {
						for (PatrolWaypoint wp : legday.getWaypoints()) {
							if (wp.getWaypoint().getAttachments() != null){
								allAttach.addAll(wp.getWaypoint().getAttachments());
							}
							for (WaypointObservation wo : wp.getWaypoint().getAllObservations()){
								if (wo.getAttachments() != null){
									allAttach.addAll(wo.getAttachments());
								}
							}
						}
					}
				}
				for (ISmartAttachment att : allAttach){
					zipper.addFile(att, PatrolXmlManager.ATTACHMENT_DIR_NAME + ZipUtil.DIR_PATH_SEPERATOR + att.getFilename());					
				}
			}
		}finally {
			zipper.close();
		}
        monitor.worked(1);
        
        try{
        	//delete temp file
        	Files.delete(xmlFile);
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
	public static Path getOutputFile(Path dir, String name, boolean includeAttachs) throws Exception {
		name = SmartUtils.getFileName(name);
		String ext = includeAttachs ? ".zip" : ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		return dir.resolve(name + ext);
	}
	
}
