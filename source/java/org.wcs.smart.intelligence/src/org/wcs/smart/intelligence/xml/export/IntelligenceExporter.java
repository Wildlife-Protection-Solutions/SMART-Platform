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
package org.wcs.smart.intelligence.xml.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceAttachment;
import org.wcs.smart.intelligence.xml.IIntelligenceXmlDataConstants;
import org.wcs.smart.intelligence.xml.IntelligenceToXmlConverter;
import org.wcs.smart.intelligence.xml.model.IntelligenceType;
import org.wcs.smart.intelligence.xml.model.ObjectFactory;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for exporting
 * a intelligence to an xml file.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceExporter {

	public static File exportIntelligence(Intelligence intelligence, File file, boolean includeAttachments, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.IntelligenceExporter_Exporting, includeAttachments ? 4 : 2);
		Session session = HibernateManager.openSession();
		try {
			session.refresh(intelligence);
			
			if (intelligence.getAttachments() != null){
				for (ISmartAttachment a : intelligence.getAttachments()){
					a.computeFileLocation(session);
				}
			}
			monitor.subTask(Messages.IntelligenceExporter_Converting);
			IntelligenceType xml = IntelligenceToXmlConverter.toXml(intelligence);
			monitor.worked(1);
			
			if (!includeAttachments){
				return exportIntelligenceWithoutAttachments(xml, file, monitor);
			}else{
				return exportIntelligenceWithAttachments(intelligence, xml, file, monitor);
			}
		} finally {
			session.close();
		}
	}

	private static File exportIntelligenceWithoutAttachments(IntelligenceType xml, File file, IProgressMonitor monitor) throws JAXBException, IOException {
		monitor.subTask(Messages.IntelligenceExporter_WritingFile);
		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))){
			writeDataModel(xml, out);
		}
		monitor.worked(1);
		return file;
	}
	
	private static File exportIntelligenceWithAttachments(Intelligence intelligence, IntelligenceType xml, File f, IProgressMonitor monitor) throws IOException, JAXBException {
		int index = f.getName().lastIndexOf('.');
		String name = f.getName();
		if (index >= 0){
			name= name.substring(0, index);
		}
		File xmlFile = File.createTempFile("temp_intelligence_export", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		exportIntelligenceWithoutAttachments(xml, xmlFile, monitor);
		
		monitor.subTask(Messages.IntelligenceExporter_PackAttachments);
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
			for (IntelligenceAttachment att : intelligence.getAttachments()) {
				File attFile = att.getAttachmentFile();
				zout.putNextEntry(new ZipEntry(IIntelligenceXmlDataConstants.ATTACHMENT_DIR_NAME + File.separator + att.getFilename()));

				try(FileInputStream inStream = new FileInputStream(attFile);) {
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
        	IntelligencePlugIn.log(null, ex);
        }
        
        return zipFile;
		
	}

	/**
	 * Determines the output file name for export
	 * based on given input intelligence name
	 * and if attachments are included.
	 * 
	 * @param inputFile
	 * @param includeAttachs
	 * @return
	 */
	public static File getOutputFile(File dir, String name, boolean includeAttachs) throws Exception {
		name = SmartUtils.getFileName(name);
		String ext = includeAttachs ? ".zip" : ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		return new File(dir, name + ext);
	}

	/**
	 * Writes a xml intelligence object to a file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param intelligence xml intelligence to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDataModel(IntelligenceType intelligence, OutputStream file) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(IIntelligenceXmlDataConstants.METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<IntelligenceType> element = objFactor.createIntelligence(intelligence);
		marshaller.marshal(element, file);
	}
	
}
