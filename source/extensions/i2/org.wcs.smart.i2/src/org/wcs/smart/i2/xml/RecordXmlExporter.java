/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.xml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.xml.record.AttachmentType;
import org.wcs.smart.i2.xml.record.AttributeType;
import org.wcs.smart.i2.xml.record.LabelUuid;
import org.wcs.smart.i2.xml.record.LocationType;
import org.wcs.smart.i2.xml.record.ObjectFactory;
import org.wcs.smart.i2.xml.record.ObservationAttributeType;
import org.wcs.smart.i2.xml.record.ObservationType;
import org.wcs.smart.i2.xml.record.RecordAttributeType;
import org.wcs.smart.i2.xml.record.RecordType;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Converts Intelligences Record to XML/zip file
 * 
 * Entities and record source attributes are looked up by uuid so users will need to 
 * import into a Conservation Area with these uuids (share between SAME Conservation Area).
 * @author Emily
 *
 */
public class RecordXmlExporter {

	public static final String DATE_FORMAT_STR = ("yyyy-MM-dd HH:mm:ss.SSS");  //$NON-NLS-1$
	
	public static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.i2.xml.record"; //$NON-NLS-1$
	
	public static final String ATTACHMENT_DIR = "attachments"; //$NON-NLS-1$
	
	public static void writeRecord(RecordType record, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		ObjectFactory factory = new ObjectFactory();
		marshaller.marshal(factory.createIntelRecord(record), file);
	}
	
	private boolean overwriteAll = false;
	private Path destFolder;
	
	public RecordXmlExporter(Path destFolder){
		this.destFolder = destFolder;
	}
	
	/**
	 * 
	 * @param recordUuid
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility 
	 * to call done() on the given monitor
	 * @return
	 * @throws Exception
	 */
	public  boolean exportRecord(UUID recordUuid, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor,2);
		
		ObjectFactory factory = new ObjectFactory();
		RecordType xmlRecord = null;
		String fileName = null;
		Path outputFile = null;
		List<File> attachmentsToInclude = new ArrayList<File>();
			
		try(Session session = HibernateManager.openSession()){
			IntelRecord record = (IntelRecord) session.get(IntelRecord.class, recordUuid);
			if (record == null) throw new Exception(Messages.RecordXmlExporter_RecordNotFound);
			progress.subTask(MessageFormat.format(Messages.RecordXmlExporter_SubTaskName, record.getTitle()));
			xmlRecord = factory.createRecordType();
			fileName = URLUtils.cleanFilename(record.getTitle());
			if (fileName.length() > 100){
				fileName = fileName.substring(0, 100);
			}
			outputFile = destFolder.resolve(fileName + ".zip"); //$NON-NLS-1$
			if (!overwriteAll && Files.exists(outputFile)){
				final String oFile = outputFile.toString();
				final int ok[] = new int[]{-1};
				Display.getDefault().syncExec(()->{
					MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(), Messages.RecordXmlExporter_OverwriteDialog,
							null, MessageFormat.format(Messages.RecordXmlExporter_OverwriteMessage, oFile),
							MessageDialog.QUESTION,
							new String[]{IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
					ok[0] = md.open();
				});
				if (ok[0] == 0){
					overwriteAll = true;
				}else if (ok[0] == 1){
					
				}else if (ok[0] == 2){
					return false;
				}
			}
			
			xmlRecord.setTitle(record.getTitle());
			xmlRecord.setStatus(record.getStatus().name());
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(record.getPrimaryDate());
			xmlRecord.setPrimaryDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
			
			if (record.getDescription() != null) xmlRecord.setNarrative(record.getDescription());
			if (record.getComment() != null) xmlRecord.setScratchpad(record.getComment());
			
			if (record.getRecordSource() != null){
				LabelUuid src = factory.createLabelUuid();
				src.setName(record.getRecordSource().getKeyId());
				src.setUuid(UuidUtils.uuidToString(record.getRecordSource().getUuid()));
				xmlRecord.setSource(src);
			}
			
			if (record.getAttributes() != null){
				for (IntelRecordAttributeValue attribute : record.getAttributes()){
					
					RecordAttributeType xmlAttributeValue = factory.createRecordAttributeType();
					
					LabelUuid aUuid = factory.createLabelUuid();
					if (attribute.getAttribute().findNameNull(  SmartDB.getCurrentConservationArea().getDefaultLanguage() ) != null) {
						xmlAttributeValue.setAlias(attribute.getAttribute().findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
					}
					if (attribute.getAttribute().getEntityType() != null) {
						if (attribute.getAttribute().getIsMultiple() != null && attribute.getAttribute().getIsMultiple()) {
							xmlAttributeValue.setType(AttributeType.MULTI_ENTITY);	
						}else {
							xmlAttributeValue.setType(AttributeType.SINGLE_ENTITY);
						}
						
					}else {
						if (attribute.getAttribute().getIsMultiple() != null && attribute.getAttribute().getIsMultiple()) {
							xmlAttributeValue.setType(AttributeType.MULTI_ATTRIBUTE);
						}else {
							xmlAttributeValue.setType(AttributeType.SINGLE_ATTRIBUTE);	
						}
					}
					aUuid.setUuid(UuidUtils.uuidToString(attribute.getAttribute().getUuid()));
					if (attribute.getAttribute().getAttribute() != null){
						aUuid.setName(attribute.getAttribute().getAttribute().getKeyId());	
					}else if (attribute.getAttribute().getEntityType() != null){
						aUuid.setName(attribute.getAttribute().getEntityType().getKeyId());
					}
					xmlAttributeValue.setRecordAttribute(aUuid);
					
					xmlAttributeValue.setNumberValue1(attribute.getNumberValue());
					xmlAttributeValue.setNumberValue2(attribute.getNumberValue2());
					xmlAttributeValue.setStringValue(attribute.getStringValue());
					
					if (attribute.getAttributeListItems() != null){
						for (IntelRecordAttributeValueList listAttribute : attribute.getAttributeListItems()){
							LabelUuid listItem = factory.createLabelUuid();;
							listItem.setUuid(UuidUtils.uuidToString(listAttribute.getId().getElementUuid()));
							
							if (listAttribute.getId().getValue().getAttribute().getAttribute() != null) {
								//attribute type
								IntelAttributeListItem item = session.get(IntelAttributeListItem.class, listAttribute.getId().getElementUuid());
								listItem.setName(item.getKeyId());
							}else if (listAttribute.getId().getValue().getAttribute().getEntityType() != null) {
								//entity type
								IntelEntity ie = session.get(IntelEntity.class, listAttribute.getId().getElementUuid());
								listItem.setName(ie.getIdAttributeAsText());
							}
							
							xmlAttributeValue.getListValue().add(listItem);
						}
					}	
					xmlRecord.getAttributes().add(xmlAttributeValue);
				}
			}
			
			if (record.getAttachments() != null){
				for (IntelRecordAttachment attachment : record.getAttachments()){
					attachment.getAttachment().computeFileLocation(session);
					AttachmentType xmlAttachment = factory.createAttachmentType();
					xmlAttachment.setFilename(attachment.getAttachment().getFilename());
					xmlRecord.getAttachments().add(xmlAttachment);
					attachmentsToInclude.add(attachment.getAttachment().getAttachmentFile());
					
					//entity attachments
					for (IntelEntityRecord entity : record.getEntities()){
						for (IntelEntityAttachment ea : entity.getEntity().getEntityAttachments()){
							if (!ea.getAttachment().equals(attachment.getAttachment())) continue;
							LabelUuid entityAttachment = new LabelUuid();
							entityAttachment.setUuid(UuidUtils.uuidToString(entity.getEntity().getUuid()));
							xmlAttachment.getEntities().add(entityAttachment);
						}
					}
					
				}
			}
			
			if (record.getEntities() != null){
				for (IntelEntityRecord r : record.getEntities()){
					LabelUuid entity = factory.createLabelUuid();
					entity.setUuid(UuidUtils.uuidToString(r.getEntity().getUuid()));
					entity.setName(r.getEntity().getIdAttributeAsText());
					entity.setKeyid(r.getEntity().getEntityType().getKeyId());
					xmlRecord.getEntities().add(entity);
				}
			}
			
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STR);
			if (record.getLocations() != null){
				for (IntelLocation location : record.getLocations()){
					LocationType xmlLocation = factory.createLocationType();
					xmlRecord.getLocations().add(xmlLocation);
					xmlLocation.setComment(location.getComment());
					xmlLocation.setDatetime(dateFormat.format(location.getDateTime()));
					xmlLocation.setGeometry( location.getGeom() );
					xmlLocation.setId(location.getId());
					
					List<IntelEntityLocation> entities = QueryFactory.buildQuery(session, IntelEntityLocation.class, "id.location", location).getResultList(); //$NON-NLS-1$
					for (IntelEntityLocation entity : entities){
						LabelUuid xmlEntity =factory.createLabelUuid();;
						xmlEntity.setUuid(UuidUtils.uuidToString(entity.getEntity().getUuid()));
						xmlEntity.setName(entity.getEntity().getIdAttributeAsText());
						xmlEntity.setKeyid(entity.getEntity().getEntityType().getKeyId());
						xmlLocation.getEntities().add(xmlEntity);
					}
					
					if (location.getObservations() != null){
						for (IntelObservation observation : location.getObservations()){
							ObservationType xmlObservation = factory.createObservationType();
							xmlLocation.getObservation().add(xmlObservation);
							
							LabelUuid xmlCategory = factory.createLabelUuid();
							xmlCategory.setUuid(UuidUtils.uuidToString(observation.getCategory().getUuid()));
							xmlCategory.setName(observation.getCategory().getHkey());
							
							xmlObservation.setCategory(xmlCategory);
							
							for (IntelObservationAttribute attribute : observation.getObservationAttributes()){
								ObservationAttributeType xmlObsAttribute = factory.createObservationAttributeType();
								
								LabelUuid xmlAttribute = factory.createLabelUuid();
								xmlAttribute.setUuid(UuidUtils.uuidToString(attribute.getAttribute().getUuid()));
								xmlAttribute.setName(attribute.getAttribute().getKeyId());
								xmlObsAttribute.setAttribute(xmlAttribute);
								
								xmlObsAttribute.setNumberValue(attribute.getNumberValue());
								xmlObsAttribute.setStringValue(attribute.getStringValue());
								
								if (attribute.getAttributeListItem() != null){
									LabelUuid xmlList = factory.createLabelUuid();
									xmlList.setUuid(UuidUtils.uuidToString(attribute.getAttributeListItem().getUuid()));
									xmlList.setName(attribute.getAttributeListItem().getKeyId());
									xmlObsAttribute.setListValue(xmlList);
								}
								if (attribute.getAttributeTreeNode() != null){
									LabelUuid xmlTree = factory.createLabelUuid();
									xmlTree.setUuid(UuidUtils.uuidToString(attribute.getAttributeTreeNode().getUuid()));
									xmlTree.setName(attribute.getAttributeTreeNode().getHkey());
									xmlObsAttribute.setTreeValue(xmlTree);
								}
								
								xmlObservation.getAttributes().add(xmlObsAttribute);
							}
							
						}
					}
				}
			}
		}
		
		Path tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
		Path xmlFile = tempDir.resolve(fileName + ".xml"); //$NON-NLS-1$
		try(OutputStream out = Files.newOutputStream(xmlFile)){
			writeRecord(xmlRecord, out);
		}
		Path attachDir = null;
		if (attachmentsToInclude.size() > 0){
			attachDir = tempDir.resolve(ATTACHMENT_DIR);
			Files.createDirectory(attachDir);
			for (File f : attachmentsToInclude){
				Files.copy(f.toPath(), attachDir.resolve(f.getName()));
			}
		}
		progress.worked(1);
		
		File[] zip = new File[]{xmlFile.toFile()};
		if (attachDir != null){
			zip = new File[]{xmlFile.toFile(), attachDir.toFile()};
		}
		//zip of Files
		ZipUtil.createZip(zip, outputFile.toFile(), progress.split(1));
			
		try{
			FileUtils.deleteDirectory(tempDir.toFile());
		}catch (Exception ex){
			ex.printStackTrace();
		}

		return true;
	}
}
