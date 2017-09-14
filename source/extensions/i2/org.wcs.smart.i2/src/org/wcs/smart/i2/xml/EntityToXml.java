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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelValueItem;
import org.wcs.smart.i2.xml.entity.Attachment;
import org.wcs.smart.i2.xml.entity.AttributeType;
import org.wcs.smart.i2.xml.entity.AttributeValue;
import org.wcs.smart.i2.xml.entity.Entity;
import org.wcs.smart.i2.xml.entity.InstanceData;
import org.wcs.smart.i2.xml.entity.Location;
import org.wcs.smart.i2.xml.entity.ObjectFactory;
import org.wcs.smart.i2.xml.entity.Record;
import org.wcs.smart.i2.xml.entity.Relationship;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Export a set of entities to xml file.
 * 
 * @author Emily
 *
 */
public class EntityToXml {
	
	public static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.i2.xml.entity"; //$NON-NLS-1$
	
	public static final String XML_DATA_FILENAME = "entities.xml"; //$NON-NLS-1$
	
	private Session session;
	
	private InstanceData data;
	private Set<Path> filesToInclude;
	
	public EntityToXml(Session session) {
		this.session = session;
	}
	
	public void export(Path outputFile, List<UUID> entities, boolean includeRecords, boolean includeRelationships, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.EntityToXml_TaskName, 4);
		toXml(entities, includeRecords, includeRelationships, progress.split(3));
		if (data  != null) {
			writeData(outputFile, progress.split(1));
		}else {
			throw new IOException(Messages.IntelDataToXml_ConvservationFailedMsg);
		}
		
	}
	
	private void writeData(Path outputFile, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		
		monitor.subTask(Messages.EntityToXml_ZipSubTask);
		progress.split(1);
		
		Path tempDir = Files.createTempDirectory("smart." + System.nanoTime()); //$NON-NLS-1$
		try {
			//write xml data
			Path xmlFile = tempDir.resolve(XML_DATA_FILENAME);
			try {
				writeToFile(data, xmlFile);
			} catch (JAXBException e) {
				throw new IOException("Unable to write intelligence data to xml file", e); //$NON-NLS-1$
			}
			
			SubMonitor sub = progress.split(1);
			sub.setWorkRemaining(filesToInclude.size());
			//copy other files
			for (Path src : filesToInclude) {
				Path trg = tempDir.resolve(src.getFileName());
				Files.copy(src, trg);
				sub.worked(1);
			}
			
			//zip together 
			progress.split(1);
			List<Path> files = Files.list(tempDir).collect(Collectors.toList());
			File[] toExport = new File[files.size()];
			for (int i = 0; i < files.size(); i ++) {
				toExport[i] = files.get(i).toFile();
			}
			ZipUtil.createZip(toExport, outputFile.toFile(), new NullProgressMonitor());
		}finally {
			//clean up
			try {
				FileUtils.deleteDirectory(tempDir.toFile());
			}catch (Exception ex) {
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		
	}
	
	private void toXml(List<UUID> entities, boolean includeRecords, boolean includeRelationships, IProgressMonitor monitor) {
	
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelDataToXml_ExportTask2, 3);
		
		Set<IntelEntity> entitiesToExport = new HashSet<>();
		Set<IntelEntityRelationship> relationshipsToExport = new HashSet<>();
		filesToInclude = new HashSet<>();
		
		progress.subTask(Messages.EntityToXml_LoadingSubTask);
		progress.split(1);
		
		if (entities != null) {
			entities.forEach(a->{
				IntelEntity entity = session.get(IntelEntity.class, a);
				if (entity != null) {
					entitiesToExport.add(entity);
					
					if (includeRelationships) {
						CriteriaBuilder cb = session.getCriteriaBuilder();
						CriteriaQuery<IntelEntityRelationship> c2 = cb.createQuery(IntelEntityRelationship.class);
						Root<IntelEntityRelationship> from2 = c2.from(IntelEntityRelationship.class);
						c2.where(cb.or(
								cb.equal(from2.get("sourceEntity"), entity), //$NON-NLS-1$
								cb.equal(from2.get("targetEntity"), entity) //$NON-NLS-1$
								));
						relationshipsToExport.addAll(session.createQuery(c2).getResultList());
					}
				}
			});
		}
		
		
		progress.subTask(Messages.EntityToXml_ConvertingEntitiesSubTask);
		List<Entity> xmlEntities = convertEntities(entitiesToExport, includeRecords, progress.split(1));
		
		progress.subTask(Messages.EntityToXml_ConvertingRelationshipsSubTask);
		
		
		List<Relationship> xmlRelationships = new ArrayList<>();
		SubMonitor sub = progress.split(1);
		if (includeRelationships) xmlRelationships = convertRelationships(relationshipsToExport, sub);
		
		progress.subTask(Messages.EntityToXml_ConvertingXmlSubTask);
		
		data = new InstanceData();
		data.getEntities().addAll(xmlEntities);
		data.getRelationships().addAll(xmlRelationships);
		
		progress.worked(1);
	}
	
	private void writeToFile(InstanceData data, Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		ObjectFactory objFactor = new ObjectFactory();
		JAXBElement<InstanceData> element = objFactor.createData(data);
		marshaller.marshal(element, xmlFile.toFile());
	}
	
	private List<Entity> convertEntities(Set<IntelEntity> ientities, boolean includeRecords, IProgressMonitor monitor){
		List<Entity> xmlEntities = new ArrayList<>();
		SubMonitor progress = SubMonitor.convert(monitor, ientities.size());
		for (IntelEntity entity : ientities) {
			progress.split(1);
			Entity xmlEntity = new Entity();
			xmlEntity.setEntityTypeKey(entity.getEntityType().getKeyId());
			xmlEntity.setScratchpad(entity.getComment());
			xmlEntity.setId(entity.getIdAttributeAsText());
			if (entity.getEntityAttachments() != null) {
				for (IntelEntityAttachment attach : entity.getEntityAttachments()) {
					Attachment xmlAttachment = new Attachment();
					if (attach.getAttachment().equals(entity.getPrimaryAttachment())) {
						xmlAttachment.setIsPrimary(true);
					}else {
						xmlAttachment.setIsPrimary(false);
					}
					try {
						attach.getAttachment().computeFileLocation(session);
					} catch (Exception e) {
						Intelligence2PlugIn.log(e.getMessage(), e);
					}
					xmlAttachment.setFilename(attach.getAttachment().getFilename());
					xmlAttachment.setDescription(attach.getAttachment().getDescription());
					filesToInclude.add(attach.getAttachment().getAttachmentFile().toPath());
					
					xmlEntity.getAttachments().add(xmlAttachment);
				}
			}
			
			if (entity.getAttributes() != null) {
				for(IntelEntityAttributeValue value : entity.getAttributes()) {
					AttributeValue xmlValue = convertAttribueValue(value);
					xmlEntity.getAttributes().add(xmlValue);
				}
			}
			
			if (includeRecords && entity.getIntelligenceRecords() != null) {
				for (IntelEntityRecord r : entity.getIntelligenceRecords()) {
					Record xmlRecord = new Record();
					xmlRecord.setTitle(r.getRecord().getTitle());
					if(r.getRecord().getRecordSource() != null) {
						xmlRecord.setRecordSourceKey(r.getRecord().getRecordSource().getKeyId());
					}
					xmlRecord.setRecordKey(UuidUtils.uuidToString(r.getRecord().getUuid()));
					
					xmlEntity.getRecords().add(xmlRecord);
				}
			}
			
			if (includeRecords && entity.getLocations() != null) {
				for (IntelEntityLocation l : entity.getLocations()) {
					Location xmlLocation = new Location();
					xmlLocation.setId(l.getLocation().getId());
					xmlLocation.setRecordKey(UuidUtils.uuidToString(l.getLocation().getRecord().getUuid()));
					
					xmlEntity.getLocations().add(xmlLocation);
				}
				
			}
			
			xmlEntities.add(xmlEntity);
		}
		return xmlEntities;
	}
	
	
	private List<Relationship> convertRelationships(Set<IntelEntityRelationship> irelationships, IProgressMonitor monitor){
		SubMonitor progress = SubMonitor.convert(monitor, irelationships.size());
		List<Relationship> xmlRelationships = new ArrayList<>();
		for (IntelEntityRelationship relationship : irelationships) {
			progress.split(1);
			Relationship xmlRelationship = new Relationship();
			
			xmlRelationship.setRelationshipTypeKey(relationship.getRelationshipType().getKeyId());
			xmlRelationship.setSourceEntityId(relationship.getSourceEntity().getIdAttributeAsText());
			xmlRelationship.setSourceEntityTypeKey(relationship.getSourceEntity().getEntityType().getKeyId());
			xmlRelationship.setTargetEntityId(relationship.getTargetEntity().getIdAttributeAsText());
			xmlRelationship.setTargetEntityTypeKey(relationship.getTargetEntity().getEntityType().getKeyId());
			
			if (relationship.getAttributes() != null && !relationship.getAttributes().isEmpty()) {
				for (IntelEntityRelationshipAttributeValue value : relationship.getAttributes()) {
					AttributeValue xmlValue = convertAttribueValue(value);
					xmlRelationship.getAttributes().add(xmlValue);
				}
			}
			xmlRelationships.add(xmlRelationship);
		}
		return xmlRelationships;
	}
	
	private AttributeValue convertAttribueValue(IntelValueItem value) {
		AttributeValue xmlValue = new AttributeValue();
		xmlValue.setAttributeKey(value.getAttribute().getKeyId());
		xmlValue.setType(AttributeType.valueOf(value.getAttribute().getType().name()));
		if (value.getNumberValue() != null) xmlValue.setDoubleValue(value.getNumberValue());
		if (value.getNumberValue2() != null) xmlValue.setDoubleValue2(value.getNumberValue2());
		if (value.getStringValue() != null) xmlValue.setStringValue(value.getStringValue());
		if (value.getAttributeListItem() != null) xmlValue.setListKey(value.getAttributeListItem().getKeyId());
		return xmlValue;
	}

}
