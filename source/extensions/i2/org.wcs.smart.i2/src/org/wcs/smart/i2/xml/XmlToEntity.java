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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelValueItem;
import org.wcs.smart.i2.xml.entity.Attachment;
import org.wcs.smart.i2.xml.entity.AttributeValue;
import org.wcs.smart.i2.xml.entity.Entity;
import org.wcs.smart.i2.xml.entity.InstanceData;
import org.wcs.smart.i2.xml.entity.Location;
import org.wcs.smart.i2.xml.entity.Record;
import org.wcs.smart.i2.xml.entity.Relationship;
import org.wcs.smart.util.ZipUtil;

import com.ibm.icu.text.MessageFormat;

/**
 * Convert xml entity file to entity records linking when possible.
 * 
 * @author Emily
 *
 */
public class XmlToEntity {

	private ConservationArea ca;
	private List<String> warnings;
	private Path rootPath;
	private Session session;
	
	private Set<IntelEntity> modifiedEntities;
	private Set<IntelRecord> modifiedRecords;
	public XmlToEntity(ConservationArea ca) {
		this.ca = ca;
	}
	
	public void importXmlData(Path zipFile, IProgressMonitor monitor) throws IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.XmlToEntity_ConversionTaskName, 10);
		warnings = new ArrayList<>();
		rootPath = Files.createTempDirectory("smart." + System.nanoTime()); //$NON-NLS-1$
		try {
			try {
				ZipUtil.unzipFolder(zipFile.toFile(), rootPath.toFile());
			} catch (Exception e) {
				throw new IOException(e);
			}
			
			Path xmlFile = rootPath.resolve(EntityToXml.XML_DATA_FILENAME);
			InstanceData data = null;
			try {
				progress.split(1);
				progress.subTask(Messages.XmlToEntity_ReadingSubTask);
				data = readXmlFile(xmlFile);
			}catch (Exception ex) {
				throw new IOException(ex);
			}
			
			try (Session session = HibernateManager.openSession(new AttachmentInterceptor())){
				toEntityData(data, session, progress.split(9));
			}
		}finally{
			//clean up
			try {
				FileUtils.deleteDirectory(rootPath.toFile());
			}catch (Exception ex) {
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	
	private InstanceData readXmlFile(Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(EntityToXml.METADATA_CLASSES_PACKAGE);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<InstanceData> o = (JAXBElement<InstanceData>) unmarshaller.unmarshal(xmlFile.toFile());
		return o.getValue();
	}
	
	private void toEntityData(InstanceData data, Session session, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 7);
		
		this.session = session;

		//process entities
		progress.subTask(Messages.XmlToEntity_ConvertingEntitiesSubTaxk);
		List<IntelEntity> entities = processEntities(data.getEntities(), progress.split(1));
		
		progress.subTask(Messages.XmlToEntity_ConvertingRelationshipSubTask);
		List<IntelEntityRelationship> relationships = processRelationships(data.getRelationships(), entities, progress.split(1));
		
		//validate warnings with user
		if (!warnings.isEmpty()) {
			boolean[] ret = new boolean[] {false};
			Display.getDefault().syncExec(()->{
				WarningDialog warningDialog = new WarningDialog(Display.getDefault().getActiveShell(), Messages.XmlToEntity_WarningDialogTitle, Messages.XmlToEntity_WarningDialogMsg, warnings, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
				if (warningDialog.open() == 0) {
					ret[0] = true;
				}
			});	
			//cancel
			if (!ret[0] ) return;
		}
		
		//save changes
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_SaveTask);
		modifiedEntities = new HashSet<>();
		modifiedRecords = new HashSet<>();
		
		session.beginTransaction();
		try {
			entities.forEach(a->{
				a.getEntityAttachments().forEach(aa -> session.save(aa.getAttachment()));
				session.save(a);
				modifiedEntities.add(a);
				for (IntelEntityRecord r : a.getIntelligenceRecords()) {
					modifiedRecords.add(r.getRecord());
				}
			});
			relationships.forEach(a->{
				session.save(a);
				modifiedEntities.add(a.getSourceEntity());
				modifiedEntities.add(a.getTargetEntity());
			});
			session.getTransaction().commit();
		}catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		
		progress.done();
		
		StringBuilder sb = new StringBuilder();
		if (entities.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToEntity_LoadedEntitiesCnt, entities.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (relationships.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToEntity_LoadedRelationshipsCnt, relationships.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		
		if (sb.length() == 0) {
			sb.append(Messages.XmlToEntity_NothingImportedMsg);
		}else {
			sb.insert(0, Messages.XmlToEntity_ImportedMsg);
		}
		
		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.XmlToEntity_ImportedDialogTitle, sb.toString());
		});

	}
	
	/**
	 * 
	 * @return a list of new and updated entities
	 */
	public Collection<IntelEntity> getNewUpdatedEntities(){
		return this.modifiedEntities;
	}
	
	/**
	 * 
	 * @return a list of updated records
	 */
	public Collection<IntelRecord> getModifiedRecords(){
		return this.modifiedRecords;
	}
	
	
	private List<IntelEntity> processEntities(List<Entity> xmlEntities, IProgressMonitor monitor){
		SubMonitor progress = SubMonitor.convert(monitor, xmlEntities.size());
		List<IntelEntity> newEntities = new ArrayList<>();
		
		for (Entity xmlEntity : xmlEntities) {
			progress.split(1);
			IntelEntity newEntity = new IntelEntity();
			
			IntelEntityType type = findEntityType(xmlEntity.getEntityTypeKey());
			if (type == null) {
				warnings.add(MessageFormat.format(Messages.XmlToEntity_EntityTypeNotFound, xmlEntity.getEntityTypeKey(), xmlEntity.getId()));
				continue;
			}
			newEntity.setComment(xmlEntity.getScratchpad());
			newEntity.setConservationArea(ca);
			newEntity.setEntityType(type);
			newEntity.setAttributes(new ArrayList<>());
			for (AttributeValue xmlValue : xmlEntity.getAttributes()) {
				IntelAttribute attribute = findAttribute(xmlValue.getAttributeKey(), xmlValue.getType().name());
				if (attribute == null) {
					warnings.add(MessageFormat.format(Messages.XmlToEntity_AttributeNotFound, xmlValue.getType().name(),xmlValue.getAttributeKey(),xmlEntity.getId())); 
					continue;
				}
				IntelEntityAttributeValue value = null;
				try{
					value = convertAttributeValue(IntelEntityAttributeValue.class, xmlValue);
				}catch (Exception ex) {
					Intelligence2PlugIn.log(ex.getMessage(), ex);
					warnings.add(MessageFormat.format(Messages.XmlToEntity_AttributeNotFound, xmlValue.getType().name(),xmlValue.getAttributeKey(),xmlEntity.getId()));
					continue;
				}
				value.setAttribute(attribute);
				if (xmlValue.getStringValue() != null) value.setStringValue(xmlValue.getStringValue());
				
				if (xmlValue.getListKey() != null && attribute.getType() == AttributeType.LIST) {
					for (IntelAttributeListItem i : attribute.getAttributeList()) {
						if (i.getKeyId().equalsIgnoreCase(xmlValue.getListKey())){
							value.setAttributeListItem(i);
							break;
						}
					}
					if (value.getAttributeListItem() == null) {
						warnings.add(MessageFormat.format(Messages.XmlToEntity_AttributeListItemNotFound, xmlValue.getListKey(), attribute.getName(), xmlEntity.getId())); 
						continue;
					}
				}
				value.setEntity(newEntity);
				newEntity.getAttributes().add(value);
			}
			
			newEntity.setEntityAttachments(new ArrayList<>());
			for (Attachment xmlAttachment : xmlEntity.getAttachments()) {
				//TODO: look to reuse ?  likely impossible although I supposed we could compare files byte of byte??
				IntelAttachment attachment = new IntelAttachment();
				attachment.setDateCreated(new Date());
				attachment.setCreatedBy(SmartDB.getCurrentEmployee());
				attachment.setConservationArea(ca);
				attachment.setCopyFromLocation(rootPath.resolve(xmlAttachment.getFilename()).toFile());
				attachment.setFilename(xmlAttachment.getFilename());
				attachment.setDescription(xmlAttachment.getDescription());
				
				IntelEntityAttachment ea = new IntelEntityAttachment();
				ea.setAttachment(attachment);
				ea.setEntity(newEntity);
				
				if (xmlAttachment.isIsPrimary()) {
					newEntity.setPrimaryAttachment(attachment);
				}
				newEntity.getEntityAttachments().add(ea);
			}
			
			HashMap<String, IntelRecord> recordMapping = new HashMap<>();
			
			newEntity.setIntelligenceRecords(new ArrayList<>());
			for (Record xmlRecord : xmlEntity.getRecords()) {
				IntelRecord record = findRecord(xmlRecord.getTitle(), xmlRecord.getRecordSourceKey(), xmlEntity.getId());
				if (record == null) continue;
				
				IntelEntityRecord newRecord = new IntelEntityRecord();
				newRecord.setEntity(newEntity);
				newRecord.setRecord(record);
				if (!newEntity.getIntelligenceRecords().contains(newRecord)){
					//record mapping is done based on titles; titles may be dpulicated
					//which may cause multiple mapping here 
					newEntity.getIntelligenceRecords().add(newRecord);
					recordMapping.put(xmlRecord.getRecordKey(), record);
				}
			}
			
			newEntity.setLocations(new ArrayList<>());
			for(Location xmlLocation : xmlEntity.getLocations()) {
				IntelRecord record = recordMapping.get(xmlLocation.getRecordKey());
				if (record == null) {
					warnings.add(MessageFormat.format(Messages.XmlToEntity_RecordLocationNotFound, xmlLocation.getId(),xmlEntity.getId()));
					continue;
				}
				IntelLocation recordLocation = null;
				for (IntelLocation l : record.getLocations()) {
					if (l.getId().equalsIgnoreCase(xmlLocation.getId())){
						recordLocation = l;
					}
				}
				if (recordLocation == null) {
					warnings.add(MessageFormat.format(Messages.XmlToEntity_LocationIdNotFound, xmlLocation.getId(),record.getTitle(),xmlEntity.getId()));
					continue;
				}
				IntelEntityLocation entityLocation = new IntelEntityLocation();
				entityLocation.setEntity(newEntity);
				entityLocation.setLocation(recordLocation);
				//we match based on id's but there is not requirement for unique ids so this may cause duplicates here;
				//instead we do our best and try to match it
				if (!newEntity.getLocations().contains(entityLocation))newEntity.getLocations().add(entityLocation);
			}
			newEntities.add(newEntity);
		}
		return newEntities;
	}
	
	private List<IntelEntityRelationship> processRelationships(List<Relationship> xmlRelationships, List<IntelEntity> newEntities, IProgressMonitor monitor){
		SubMonitor progress = SubMonitor.convert(monitor, xmlRelationships.size());
		List<IntelEntityRelationship> newRelationships = new ArrayList<>();
		
		for (Relationship xmlRelationship : xmlRelationships) {
			progress.split(1);
			IntelEntityRelationship newRelationship = new IntelEntityRelationship();
			
			IntelRelationshipType type = findRelationshipType(xmlRelationship.getRelationshipTypeKey());
			if (type == null) {
				warnings.add(MessageFormat.format(Messages.XmlToEntity_RelationshipTypeNotFound, xmlRelationship.getRelationshipTypeKey()));
				continue;
			}
			newRelationship.setRelationshipType(type);
			
			
			IntelEntity srcEntity = findRelationshipEntity(xmlRelationship.getSourceEntityId(), xmlRelationship.getSourceEntityTypeKey(), newEntities);
			if (srcEntity == null) continue;
			newRelationship.setSourceEntity(srcEntity);
			
			IntelEntity trgEntity = findRelationshipEntity(xmlRelationship.getTargetEntityId(), xmlRelationship.getTargetEntityTypeKey(), newEntities);
			if (trgEntity == null) continue;
			newRelationship.setTargetEntity(trgEntity);
			
			newRelationship.setAttributes(new ArrayList<>());
			for (AttributeValue xmlValue : xmlRelationship.getAttributes()) {
				IntelAttribute attribute = findAttribute(xmlValue.getAttributeKey(), xmlValue.getType().name());
				if (attribute == null) {
					warnings.add(MessageFormat.format(Messages.XmlToEntity_RelAttributeNotFound, xmlValue.getType().name(),xmlValue.getAttributeKey())); 
					continue;
				}
				IntelEntityRelationshipAttributeValue value = null;
				try{
					value = convertAttributeValue(IntelEntityRelationshipAttributeValue.class, xmlValue);
				}catch (Exception ex) {
					Intelligence2PlugIn.log(ex.getMessage(), ex);
					warnings.add(MessageFormat.format(Messages.XmlToEntity_RelAttributeNotFound, xmlValue.getType().name(),xmlValue.getAttributeKey()));
				}
				value.setAttribute(attribute);
				if (xmlValue.getStringValue() != null) value.setStringValue(xmlValue.getStringValue());
				
				if (xmlValue.getListKey() != null && attribute.getType() == AttributeType.LIST) {
					for (IntelAttributeListItem i : attribute.getAttributeList()) {
						if (i.getKeyId().equalsIgnoreCase(xmlValue.getListKey())){
							value.setAttributeListItem(i);
							break;
						}
					}
					if (value.getAttributeListItem() == null) {
						warnings.add(MessageFormat.format(Messages.XmlToEntity_RelAttributeListItemNotFound, xmlValue.getType().name(),xmlValue.getAttributeKey())); 
						continue;
					}
				}
				value.setRelationship(newRelationship);
				newRelationship.getAttributes().add(value);
			}
			
			newRelationship.setSource(IntelEntityRelationship.Source.ENTITY);
			newRelationship.setSourceId(newRelationship.getSourceEntity().getUuid());
			
			newRelationships.add(newRelationship);
		}
		return newRelationships;
	}
	public <T extends IntelValueItem> T convertAttributeValue(Class<T> clazz, AttributeValue xmlValue) throws Exception {
		T result = clazz.newInstance();
		if (xmlValue.getDoubleValue() != null) result.setNumberValue(xmlValue.getDoubleValue());
		if (xmlValue.getDoubleValue2() != null) result.setNumberValue2(xmlValue.getDoubleValue2());
		
		return result;
	}
	
	public IntelRecord findRecord(String title, String sourceKey, String entityId) {
		IntelRecordSource source = QueryFactory.buildQuery(session, IntelRecordSource.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", sourceKey}).uniqueResult(); //$NON-NLS-1$
		if (source == null) {
			warnings.add(MessageFormat.format(Messages.XmlToEntity_RecordSrcNotfound, sourceKey, title, entityId));
			return null;
		}
		
		List<IntelRecord> records = QueryFactory.buildQuery(session, IntelRecord.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"recordSource", source}, //$NON-NLS-1$
				new Object[] {"title", title}).list(); //$NON-NLS-1$
		if (records.size() == 1) {
			return records.get(0);
		}else if (records.size() == 0) {
			warnings.add(MessageFormat.format(Messages.XmlToEntity_RecordNotFound, title, source.getName(), entityId));
		}else if (records.size() > 1) {
			warnings.add(MessageFormat.format(Messages.XmlToEntity_MultipleRecordsFound, title, source.getName(), entityId));
		}
		return null;
	}
	
	public IntelEntity findRelationshipEntity(String entityId, String entityTypeKey, List<IntelEntity> newEntities) {
		IntelEntityType type = findEntityType(entityTypeKey);
		if (type == null) {
			warnings.add(MessageFormat.format(Messages.XmlToEntity_RelationshipEntityTypeNotFound, entityTypeKey));
			return null;
		}
		
		List<IntelEntity> possibleEntities = new ArrayList<>();
		for (IntelEntity ie : newEntities) {
			if (ie.getEntityType().equals(type)) {
				if (ie.getIdAttributeAsText().equals(entityId)) {
					possibleEntities.add(ie);
				}
			}
		}
		
		try(ScrollableResults scroll = QueryFactory.buildQuery(session, IntelEntity.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"entityType", type}).scroll()){ //$NON-NLS-1$
		
			while(scroll.next()) {
				IntelEntity entity = (IntelEntity) scroll.get()[0];
				if (entity.getIdAttributeAsText().equals(entityId)) {
					possibleEntities.add(entity);
				}
			}
		}
		
		if (possibleEntities.size() == 1) {
			return possibleEntities.get(0);
		}else if (possibleEntities.size() == 0) {
			warnings.add(MessageFormat.format(Messages.XmlToEntity_RelationshipEntitiesNotFound, type.getName(), entityId));
		}else {
			warnings.add(MessageFormat.format(Messages.XmlToEntity_MultipleRelationshipEntitiesFound, type.getName(), entityId));
		}
		return null;
	}
	
	
	public IntelAttribute findAttribute(String attributeKey, String attributeType) {
		IntelAttribute.AttributeType type = IntelAttribute.AttributeType.valueOf(attributeType);
		IntelAttribute attribute = QueryFactory.buildQuery(session, IntelAttribute.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", attributeKey}, //$NON-NLS-1$
				new Object[] {"type", type}).uniqueResult(); //$NON-NLS-1$
		return attribute;
	}
	
	public IntelEntityType findEntityType(String entityTypeKey) {
		return QueryFactory.buildQuery(session, IntelEntityType.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", entityTypeKey}).uniqueResult(); //$NON-NLS-1$
	}
	
	public IntelRelationshipType findRelationshipType(String relationshipTypeKey) {
		return QueryFactory.buildQuery(session, IntelRelationshipType.class,
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", relationshipTypeKey}).uniqueResult(); //$NON-NLS-1$
	}
}
