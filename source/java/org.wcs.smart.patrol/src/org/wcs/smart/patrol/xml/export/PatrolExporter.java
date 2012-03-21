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
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;
import org.wcs.smart.patrol.xml.PatrolToXmlConverter;
import org.wcs.smart.patrol.xml.PatrolXmlManager;
import org.wcs.smart.patrol.xml.model.PatrolType;

/**
 * Class responsible for exporting
 * a patrol to an xml file.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolExporter {

	
	/**
	 * Writes a patrol to a file in xml file.
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
		monitor.beginTask("Exporting Patrol", includeAttachments ? 4 : 2);
		Session session = HibernateManager.openSession();
		try {
			session.update(patrol);
			
			monitor.subTask("Converting patrol");
			PatrolType xml = PatrolToXmlConverter.toXml(patrol);
			monitor.worked(1);
			
			if (!includeAttachments){
				return exportPatrolWithoutAttachments(xml, file, monitor);
			}else{
				return exportPatrolWithAttachments(patrol, xml, file, monitor);
			}
		} finally {
			session.close();
		}
	}
	
	
	/**
	 * Writes the patrol without including attachments
	 */
	private static File exportPatrolWithoutAttachments(PatrolType xml, File file, IProgressMonitor monitor) throws Exception {
		monitor.subTask("Writing patrol to file");
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			PatrolXmlManager.writeDataModel(xml, out);
		} finally {
			out.close();
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
		File xmlFile = File.createTempFile(name, ".xml");
		exportPatrolWithoutAttachments(xml, xmlFile, monitor);
		
		
		monitor.subTask("Packaging Attachments");
		//create zip file
		File zipFile = new File(f.getParent() + File.separator + name + ".zip");
		ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));
		try {
			zout.setLevel(Deflater.DEFAULT_COMPRESSION);

			/* add xml file to zip */
			zout.putNextEntry(new ZipEntry(name	+ ".xml"));
			FileInputStream inStream = new FileInputStream(xmlFile);

			byte[] buffer = new byte[1024];
			int bytesRead;
			try {
				while ((bytesRead = inStream.read(buffer)) > 0) {
					zout.write(buffer, 0, bytesRead);
				}
			} finally {
				inStream.close();
			}
			monitor.worked(1);

			/* add all attachments */
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay legday : leg.getPatrolLegDays()) {
					for (Waypoint wp : legday.getWaypoints()) {
						for (WaypointAttachment att : wp.getAttachments()) {
							File attFile = att.getFullFile();
							zout.putNextEntry(new ZipEntry(PatrolXmlManager.ATTACHMENT_DIR_NAME + File.separator + att.getFilename()));

							inStream = new FileInputStream(attFile);
							try {
								while ((bytesRead = inStream.read(buffer)) > 0) {
									zout.write(buffer, 0, bytesRead);
								}
							} finally {
								inStream.close();
							}
						}
					}
				}
			}
			// close
		} finally {
			zout.close();
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
	 * @param inputFile
	 * @param includeAttributes
	 * @return
	 */
	public static File getOutputFile(String inputFile, boolean includeAttributes){
		if (!includeAttributes){
			return new File(inputFile);
		}else{
			//turn into zip file
			File in = new File(inputFile);
			int index = in.getName().lastIndexOf('.');
			String name = in.getName();
			if (index > 0){
				name = name.substring(0, index);
			}
			String zip = in.getParent() + File.separator + name + ".zip";
			return new File(zip);
		}
	}
}
