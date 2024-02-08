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
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.WKBReader;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.GeometrySource;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelObservationAttributeList;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.record.importer.RecordImportEngine;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.xml.record.AttachmentType;
import org.wcs.smart.i2.xml.record.AttributeType;
import org.wcs.smart.i2.xml.record.LabelUuid;
import org.wcs.smart.i2.xml.record.LocationType;
import org.wcs.smart.i2.xml.record.ObservationAttributeType;
import org.wcs.smart.i2.xml.record.ObservationType;
import org.wcs.smart.i2.xml.record.RecordAttributeType;
import org.wcs.smart.i2.xml.record.RecordType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Record XML Importer.
 * 
 * @author Emily
 *
 */
public class RecordXmlImporter {

	@SuppressWarnings("unchecked")
	public static RecordType readRecord(InputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(RecordXmlExporter.METADATA_CLASSES_PACKAGE);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		JAXBElement<RecordType> o = (JAXBElement<RecordType>) unmarshaller.unmarshal(file);
		return o.getValue();
	}

	private DateTimeFormatter dateFormater = DateTimeFormatter.ofPattern(RecordXmlExporter.DATE_FORMAT_STR);
	
	private Session session;
	//records and entity locations to save
	private List<IntelRecord> records;
	private HashMap<IntelRecord, List<IntelEntityLocation>> entityLocations;
	private HashMap<IntelRecord, List<IntelEntityAttachment>> entityAttachments;
	//list of warnings
	private HashMap<Path, List<String>> allWarnings;
	//temporary directories to cleanup after attachments imported
	private List<Path> tempDirs;
	private boolean isZip = false;
	
	public RecordXmlImporter(Session session){
		this.session = session;
		records = new ArrayList<IntelRecord>();
		entityLocations = new HashMap<>();
		entityAttachments = new HashMap<>();
		allWarnings = new HashMap<Path, List<String>>();
		tempDirs = new ArrayList<Path>();
	}
	
	/**
	 * Saves all records to the database; if cannot save any of the records all will fail.
	 * @param broker
	 * @return a two element object array - the first is 0 if error occured while processing events; 1 if everything went ok; the second 
	 * is the status string.  Will return null if user cancelled.
	 * @throws Exception
	 */
	public Object[] finishSingleTransaction(IEventBroker broker) throws Exception{
		ArrayList<IntelRecord> loaded = new ArrayList<>();
		ArrayList<IntelRecord> deleted = new ArrayList<>();
		ArrayList<IntelEntity> modified = new ArrayList<>();
		try {
			if (!showWarnings()) return null;
			session.beginTransaction();
			try {
				for (IntelRecord r : records){
					if (!doSave(r, session, modified, deleted)) continue;
					loaded.add(r);
				}
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				Intelligence2PlugIn.log(ex.getMessage(), ex);
				throw ex;
			}
			
			try{
				broker.send(IntelEvents.RECORD_NEW, loaded);
				if (!deleted.isEmpty()) broker.send(IntelEvents.RECORD_DELETE, deleted);
				if (!modified.isEmpty()) broker.send(IntelEvents.ENTITY_MODIFIED, modified);
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);

				return new Object[] {0,MessageFormat.format(Messages.RecordXmlImporter_ProcessingCompleteMsgEventError, loaded.size(), records.size())};
			}
		}finally {
			cleanUp();
		}
		return new Object[] {1, MessageFormat.format(Messages.RecordXmlImporter_ProcessingCompleteMsg, loaded.size(), records.size())};		
	}
	
	/**
	 * Saves records one at a time.  If one fails it will move on to the next
	 * and try to save that one.
	 * 
	 * @param broker
	 * @return if something was saved; false if the uesr cancelled
	 */
	public boolean finish(IEventBroker broker){
		ArrayList<IntelRecord> loaded = new ArrayList<>();
		ArrayList<IntelRecord> deleted = new ArrayList<>();
		ArrayList<IntelEntity> modified = new ArrayList<>();

		StringBuilder error = new StringBuilder();
		try {
			if (!showWarnings()) return false;
			
			for (IntelRecord r : records){
				session.beginTransaction();
				try{
					if (!doSave(r, session, modified, deleted)) {
						session.getTransaction().rollback();
						continue;
					}
					session.getTransaction().commit();
					loaded.add(r);
				}catch (Exception ex){
					session.getTransaction().rollback();
					error.append(MessageFormat.format(Messages.RecordXmlImporter_SaveError, r.getTitle(), ex.getMessage()));
					Intelligence2PlugIn.log(ex.getMessage(), ex);
				}
			}
			
			try{
				broker.send(IntelEvents.RECORD_NEW, loaded);
				if (!deleted.isEmpty()) broker.send(IntelEvents.RECORD_DELETE, deleted);
				if (!modified.isEmpty()) broker.send(IntelEvents.ENTITY_MODIFIED, modified);
			}catch (Exception ex){
				error.append(Messages.RecordXmlImporter_EventError + ex.getMessage());
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}finally {
			cleanUp();
		}
		
		Display.getDefault().syncExec(()->{
			if (error.length() > 0){
				error.insert(0, MessageFormat.format(Messages.RecordXmlImporter_ImportFinishedError, loaded.size(), records.size()));
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.RecordXmlImporter_ImportCompleteTitle, error.toString());
			}else{
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.RecordXmlImporter_ImportCompleteTitle, MessageFormat.format(Messages.RecordXmlImporter_ImportFinished, loaded.size(), records.size()));
			}
		});
		return true;
	}
	
	private boolean doSave(IntelRecord r, Session s, ArrayList<IntelEntity> modified, ArrayList<IntelRecord> deleted) throws Exception{
		
		if (!IntelSecurityManager.INSTANCE.canCreateRecord(r.getProfile())) {
			throw new Exception(MessageFormat.format(Messages.RecordXmlImporter_NoPermission, r.getProfile().getName()));
		}
		Long cnt = QueryFactory.buildCountQuery(session, IntelRecord.class, 
				new Object[] {"conservationArea", r.getConservationArea()}, //$NON-NLS-1$
				new Object[] {"title", r.getTitle()}); //$NON-NLS-1$
		int action = RecordImportEngine.confirmDuplicate(cnt, r.getTitle());
		if (action == 2) {
			//delete
			IntelRecord delete= QueryFactory.buildQuery(session, IntelRecord.class, 
					new Object[] {"conservationArea", r.getConservationArea()},  //$NON-NLS-1$
					new Object[] {"title", r.getTitle()}).uniqueResult();  //$NON-NLS-1$
			for (IntelEntityRecord d : delete.getEntities()) {
				modified.add(d.getEntity());
			}
			RecordManager.INSTANCE.deleteRecord(delete, session);
			deleted.add(delete);
		}else if (action == 1) {
			//save
			
		}else if (action == 0) {
			//skip
			return false;
		}
		
		List<IntelEntityLocation> locations = entityLocations.get(r);
		if (r.getAttachments() != null){
			for (IntelRecordAttachment recordAttachment : r.getAttachments()){
				session.saveOrUpdate(recordAttachment.getAttachment());
			}
		}
		
		List<IntelEntityAttachment> attachments = entityAttachments.get(r);
		if (attachments != null) {
			for (IntelEntityAttachment entityAttachment : attachments){
				session.saveOrUpdate(entityAttachment);
				modified.add(entityAttachment.getEntity());
			}
		}
		
		session.saveOrUpdate(r);
		if (locations != null){
			for (IntelEntityLocation l : locations) session.saveOrUpdate(l);
		}
		return true;
	}
	
	private boolean showWarnings() {
		List<String> warnings = new ArrayList<String>();
		for (Entry<Path, List<String>> warn : allWarnings.entrySet()){
			if (!warn.getValue().isEmpty()){
				warnings.add(warn.getKey().toString());
				warnings.addAll(warn.getValue());
				warnings.add("----------------------------------"); //$NON-NLS-1$
			}
		}
		if (!warnings.isEmpty()){
			final int[] ret = new int[]{-1};
			Display.getDefault().syncExec(()->{
				WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.RecordXmlImporter_WarningsDialogTitle, 
						Messages.RecordXmlImporter_WarningsMessage, warnings, 
						new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
				ret[0] = wd.open();
			});
			
			if (ret[0] == 1){
				cleanUp();
				return false;
			}
		}
		return true;
	}
	
	
	
	private void cleanUp(){
		for (Path p : tempDirs){
			try{
				SmartUtils.deleteDirectory(p);
			}catch (Exception ex){
			}
		}		
	}
	
	/**
	 * 
	 * @param zipFile zip file or xml file to import
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility 
	 * to call done() on the given monitor
	 */
	public void importRecord(Path zipFile, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		
		progress.subTask(MessageFormat.format(Messages.RecordXmlImporter_TaskName, zipFile.toString()));
		List<String> warnings = allWarnings.get(zipFile);
		if (warnings == null){
			warnings = new ArrayList<String>();
			allWarnings.put(zipFile, warnings);
		}
		
		Path tempDir = null;
		try{
			RecordType type = null;
			
			Path xmlFile = null;
			
			if (SmartUtils.isZip(zipFile)) {
				isZip = true;
				//unzip file
				tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
				tempDirs.add(tempDir);
				ZipUtil.unzipFolder(zipFile, tempDir);
				
				//search temp dir for xml file
				
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(tempDir)) {
		            for (Path path : directoryStream) {
		            	if (!Files.isDirectory(path) && path.toString().endsWith(".xml")){ //$NON-NLS-1$
		            		xmlFile = path;
		            		break;
		            	}
		            }
		        } catch (IOException ex) {}
			}else {
				isZip = false;
				xmlFile = zipFile;
			}
			
			if (xmlFile == null) throw new Exception(MessageFormat.format(Messages.RecordXmlImporter_NoXmlFileFound, zipFile.toString()));
			
			try(InputStream in = Files.newInputStream(xmlFile)){
				type = readRecord(in);
			}catch (Exception ex){
				Intelligence2PlugIn.log(ex.getMessage(), ex);
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_ParseError, xmlFile.getFileName().toString(), ex.getMessage()));
				return;
			}
			if (type == null){
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_ParseError2, xmlFile.getFileName().toString()));
				return;
			}
			
			if (type.getTitle() == null || type.getTitle().trim().isEmpty()) {
				throw new Exception(Messages.RecordXmlImporter_TitleRequired);
			}
			
			IntelProfile profile = findProfile(session,  type.getProfileKey());
			if(profile == null) {
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_ProfileFound, type.getProfileKey(), xmlFile.getFileName().toString()));
				return;
			}
			List<IntelEntityLocation> entitylocations = new ArrayList<>();
			IntelRecord newRecord = new IntelRecord();
			newRecord.setProfile(profile);
			newRecord.setConservationArea(SmartDB.getCurrentConservationArea());
			newRecord.setTitle(type.getTitle().trim());
			if (type.getPrimaryDate() != null) {
				try {
					newRecord.setPrimaryDate(SmartUtils.toLocalDateTime(type.getPrimaryDate()));
				}catch (Exception ex) {
					//try just date
					try {
						newRecord.setPrimaryDate(SmartUtils.toLocalDate(type.getPrimaryDate()).atStartOfDay());
					}catch (Exception ex2) {
						throw new Exception(MessageFormat.format(Messages.RecordXmlImporter_DateParseError, type.getPrimaryDate().toString()), ex2);
					}
					
				}
			}else {
				newRecord.setPrimaryDate(LocalDateTime.now());
			}
			if (type.getScratchpad() != null) {
				String sp = type.getScratchpad().trim();
				if (sp.length() > IntelRecord.SCRATCH_MAX_LENGTH) {
					warnings.add(MessageFormat.format(Messages.RecordXmlImporter_scratchpadToLong, newRecord.getTitle()));
					sp = sp.substring(0,IntelRecord.SCRATCH_MAX_LENGTH);
				}				
				newRecord.setComment(sp);
			}
			if (type.getNarrative() != null) {
				String sp = type.getNarrative().trim();
				if (sp.length() > IntelRecord.SCRATCH_MAX_LENGTH) {
					warnings.add(MessageFormat.format(Messages.RecordXmlImporter_NarrativeToLong, newRecord.getTitle()));
					sp = sp.substring(0,IntelRecord.SCRATCH_MAX_LENGTH);
				}				
				newRecord.setDescription(sp);
			}

			try {
				newRecord.setStatus(IntelRecord.Status.valueOf(type.getStatus()));
			} catch (Exception ex) {
				throw new Exception(MessageFormat.format(
						Messages.RecordXmlImporter_InvalidStatus, type.getStatus()));
			}

			newRecord.setRecordSource(parseRecordSource(type.getSource(), session, warnings));

			if (newRecord.getRecordSource() != null) {
				List<IntelRecordAttributeValue> recordAttributes = parseRecordSourceAttributes(
						type.getAttributes(), newRecord,
						session, warnings);
				newRecord.setAttributes(recordAttributes);
			}

			if (type.getEntities() != null && !type.getEntities().isEmpty()) {
				newRecord.setEntities(new ArrayList<IntelEntityRecord>());
				for (LabelUuid e : type.getEntities()) {
					IntelEntity entity = parseEntity(e, findEntityType(session, e.getKeyid()), session, warnings);
					if (entity != null) {
						IntelEntityRecord r = new IntelEntityRecord();
						r.setEntity(entity);
						r.setRecord(newRecord);
						newRecord.getEntities().add(r);
					}
				}
			}

			if (type.getAttachments() != null && !type.getAttachments().isEmpty()) {
				if (isZip) {
					newRecord.setAttachments(new ArrayList<IntelRecordAttachment>());
					for (AttachmentType xmlAttachment : type.getAttachments()) {
						Path importFile = xmlFile.getParent().resolve(RecordXmlExporter.ATTACHMENT_DIR).resolve(xmlAttachment.getFilename());
						IntelAttachment newAttachment = new IntelAttachment();
						newAttachment.setCopyFromLocation(importFile);
						newAttachment.setFilename(xmlAttachment.getFilename());
						newAttachment.setConservationArea(SmartDB.getCurrentConservationArea());
						newAttachment.setDateCreated(LocalDateTime.now());
						newAttachment.setCreatedBy(SmartDB.getCurrentEmployee());
						
						IntelRecordAttachment attach = new IntelRecordAttachment();
						attach.setAttachment(newAttachment);
						attach.setRecord(newRecord);
						newRecord.getAttachments().add(attach);
						
						List<IntelEntityAttachment> newAttachments = new ArrayList<>();
						for (LabelUuid entityUuid : xmlAttachment.getEntities()) {
							for (IntelEntityRecord entityRecord : newRecord.getEntities()) {
								if (entityRecord.getEntity().getUuid().equals(UuidUtils.stringToUuid(entityUuid.getUuid()))) {
									IntelEntityAttachment entityAttachment = new IntelEntityAttachment();
									entityAttachment.setAttachment(newAttachment);
									entityAttachment.setEntity(entityRecord.getEntity());
									newAttachments.add(entityAttachment);
									break;
								}
							}
						}
						entityAttachments.put(newRecord, newAttachments);
					}
				}else {
					if (!type.getAttachments().isEmpty()) {
						warnings.add(MessageFormat.format(Messages.RecordXmlImporter_attachmentwarning, type.getAttachments().size()));
					}
				}
			}
			
			if (type.getLocations() != null && !type.getLocations().isEmpty()) {
				newRecord.setLocations(new ArrayList<IntelLocation>());

				for (LocationType xmlLocation : type.getLocations()) {

					IntelLocation newLocation = new IntelLocation();
					if (xmlLocation.getComment() != null) newLocation.setComment(xmlLocation.getComment());
					if (xmlLocation.getId() != null) newLocation.setId(xmlLocation.getId());
					newLocation.setDateTime(LocalDateTime.parse(xmlLocation.getDatetime(), dateFormater));
					newLocation.setConservationArea(SmartDB.getCurrentConservationArea());
					newLocation.setGeom(xmlLocation.getGeometry());
					newLocation.setRecord(newRecord);
					newRecord.getLocations().add(newLocation);

					if (xmlLocation.getEntities() != null && !xmlLocation.getEntities().isEmpty()) {

						for (LabelUuid xmlEntity : xmlLocation.getEntities()) {
							IntelEntity entity = parseEntity(xmlEntity, findEntityType(session, xmlEntity.getKeyid()), session, warnings);
							if (entity != null) {
								IntelEntityLocation ieLocation = new IntelEntityLocation();
								ieLocation.setEntity(entity);
								ieLocation.setLocation(newLocation);
								entitylocations.add(ieLocation);
							}
						}
					}

					if (xmlLocation.getObservation() != null && !xmlLocation.getObservation().isEmpty()) {
						newLocation.setObservations(new ArrayList<>());
						for (ObservationType xmlObservation : xmlLocation.getObservation()) {
							Category c = parseCategory(xmlObservation.getCategory(), session, warnings);
							if (c != null) {
								IntelObservation newObservation = new IntelObservation();
								newObservation.setCategory(c);
								newObservation.setLocation(newLocation);
								newLocation.getObservations().add(newObservation);

								if (xmlObservation.getAttributes() != null && !xmlObservation.getAttributes().isEmpty()) {
									newObservation.setObservationAttributes(parseObservationAttributes(xmlObservation.getAttributes(), newObservation, session, warnings));
								}
							}
						}
					}
				}
			}
			
			records.add(newRecord);
			entityLocations.put(newRecord,  entitylocations);
		}catch (Exception ex){
			warnings.clear();
			warnings.add(MessageFormat.format(Messages.RecordXmlImporter_ProcessingError1, zipFile.toString(), ex.getMessage()));
			Intelligence2PlugIn.log(MessageFormat.format(Messages.RecordXmlImporter_ProcessingError2, zipFile.toString(), ex.getMessage()), ex);
		}
		progress.worked(1);
	}
	
	private boolean nullEquals(Boolean test, boolean value) {
		if (test == null) return !value;
		return test.booleanValue() == value;
	}
	
	private List<IntelRecordAttributeValue> parseRecordSourceAttributes(List<RecordAttributeType> attributes, IntelRecord record, Session session, List<String> warnings){
		if (attributes == null || attributes.isEmpty()) return null;
		List<IntelRecordAttributeValue> newValues = new ArrayList<IntelRecordAttributeValue>();
		HashSet<IntelRecordSourceAttribute> usedAttributes = new HashSet<>();
		
		for (RecordAttributeType recordAttribute : attributes){
			UUID attributeUuid = UuidUtils.stringToUuid(recordAttribute.getRecordAttribute().getUuid());
			
			//first search for uuid
			IntelRecordSourceAttribute srcAttribute = null;
			for (IntelRecordSourceAttribute a : record.getRecordSource().getAttributes()){
				if (a.getUuid().equals(attributeUuid)){
					srcAttribute = a;
					break;
				}
			}

			if (srcAttribute == null) {
				List<IntelRecordSourceAttribute> possibleMatches = new ArrayList<>();

				if (recordAttribute.getType().equals(AttributeType.MULTI_ATTRIBUTE) || 
						recordAttribute.getType().equals(AttributeType.SINGLE_ATTRIBUTE)) {					
					boolean isMulti = recordAttribute.getType().equals(AttributeType.MULTI_ATTRIBUTE);
					//search by key
					for (IntelRecordSourceAttribute a : record.getRecordSource().getAttributes()) {
						if (nullEquals(a.getIsMultiple(), isMulti) 
								&& a.getAttribute() != null 
								&& a.getAttribute().getKeyId().equals(recordAttribute.getRecordAttribute().getName())) {
							if (!usedAttributes.contains(a)){
								possibleMatches.add(a);
							}
						}
					}
				}else if (recordAttribute.getType().equals(AttributeType.MULTI_ENTITY) || 
						recordAttribute.getType().equals(AttributeType.SINGLE_ENTITY)) {
					
					boolean isMulti = recordAttribute.getType().equals(AttributeType.MULTI_ENTITY);
					for (IntelRecordSourceAttribute a : record.getRecordSource().getAttributes()) {
						if (nullEquals(a.getIsMultiple(), isMulti)  && a.getEntityType() != null && a.getEntityType().getKeyId().equals(recordAttribute.getRecordAttribute().getName())) {
							if (!usedAttributes.contains(a)){
								possibleMatches.add(a);
							}
						}
					}
				}
				
				if (possibleMatches.size() == 1) {
					srcAttribute = possibleMatches.get(0);
				}else if (possibleMatches.size() > 1) {
					String alias = recordAttribute.getAlias();
					for (IntelRecordSourceAttribute poss : possibleMatches) {
						if ((alias == null || alias.length() == 0) && poss.getNames().isEmpty()) {
							srcAttribute = poss;
							break;
						}
						for (Label l :poss.getNames()) {
							if (l.getValue().equalsIgnoreCase(alias)) {
								srcAttribute = poss;
								break;
							}
						}
						if (srcAttribute != null) break;
					}
					if (srcAttribute == null) srcAttribute = possibleMatches.get(0);
				}
			}
			
			if (srcAttribute == null){
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_SourceAttributenotFound, recordAttribute.getRecordAttribute().getName(), record.getRecordSource().getName()));
				continue;
			}
			usedAttributes.add(srcAttribute);
			
			IntelRecordAttributeValue newValue = new IntelRecordAttributeValue();
			newValue.setAttribute(srcAttribute);
			newValue.setRecord(record);
			if (srcAttribute.getAttribute() != null){
				switch(srcAttribute.getAttribute().getType()){
				case BOOLEAN:
				case NUMERIC:
					if (recordAttribute.getNumberValue1() == null) continue;
					newValue.setNumberValue(recordAttribute.getNumberValue1());
					break;
				case DATE:
				case TEXT:
					if (recordAttribute.getStringValue() == null) continue;
					newValue.setStringValue(recordAttribute.getStringValue());
					break;
				case LIST:
					if (recordAttribute.getListValue() == null || recordAttribute.getListValue().isEmpty()) continue;
					
					List<IntelRecordAttributeValueList> listItems = new ArrayList<IntelRecordAttributeValueList>();
					for (LabelUuid list : recordAttribute.getListValue()){
						IntelAttributeListItem item = parseAttributeListItem(list,srcAttribute.getAttribute(), session, warnings);
						
						if (item != null){
							IntelRecordAttributeValueList newItem = new IntelRecordAttributeValueList();
							newItem.getId().setElementUuid(item.getUuid());
							newItem.getId().setValue(newValue);
							listItems.add(newItem);
						}
					}
					if (nullEquals(srcAttribute.getIsMultiple(), false) && listItems.size() > 1){
						listItems = Collections.singletonList(listItems.get(0));
					}
					newValue.setAttributeListItems(listItems);
					
					break;
				case EMPLOYEE:
					if (recordAttribute.getListValue() == null || recordAttribute.getListValue().isEmpty()) continue;
					
					List<IntelRecordAttributeValueList> listItems2 = new ArrayList<IntelRecordAttributeValueList>();
					for (LabelUuid list : recordAttribute.getListValue()){
						Employee item = parseEmployeeListItem(list, srcAttribute.getAttribute(), session, warnings);
						
						if (item != null){
							IntelRecordAttributeValueList newItem = new IntelRecordAttributeValueList();
							newItem.getId().setElementUuid(item.getUuid());
							newItem.getId().setValue(newValue);
							listItems2.add(newItem);
						}
					}
					if (nullEquals(srcAttribute.getIsMultiple(), false) && listItems2.size() > 1){
						listItems2 = Collections.singletonList(listItems2.get(0));
					}
					newValue.setAttributeListItems(listItems2);
					
					break;
					
				case POSITION:
					if (recordAttribute.getNumberValue1() == null || recordAttribute.getNumberValue2() == null) continue;
					newValue.setNumberValue(recordAttribute.getNumberValue1());
					newValue.setNumberValue2(recordAttribute.getNumberValue2());
					break;
				}
			}else if (srcAttribute.getEntityType() != null){
				if (recordAttribute.getListValue() == null || recordAttribute.getListValue().isEmpty()) continue;
				
				List<IntelRecordAttributeValueList> listItems = new ArrayList<IntelRecordAttributeValueList>();
				for (LabelUuid list : recordAttribute.getListValue()){
					IntelEntity item = parseEntity(list,srcAttribute.getEntityType(), session, warnings);
					if (item != null){
						IntelRecordAttributeValueList newItem = new IntelRecordAttributeValueList();
						newItem.getId().setElementUuid(item.getUuid());
						newItem.getId().setValue(newValue);
						listItems.add(newItem);
					}
				}
				if (!srcAttribute.getIsMultiple() && listItems.size() > 1){
					listItems = Collections.singletonList(listItems.get(0));
				}
				newValue.setAttributeListItems(listItems);
			}
			newValues.add(newValue);
		}
		return newValues;		
	}
	
	private IntelRecordSource parseRecordSource(LabelUuid source, Session session, List<String> warnings){
		if (source == null) return null;
		
		//search uuids
		IntelRecordSource src = (IntelRecordSource) session.get(IntelRecordSource.class, UuidUtils.stringToUuid(source.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return src;
		}
		
		//search keys
		List<IntelRecordSource> sources = QueryFactory.buildQuery(session, IntelRecordSource.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
		for (IntelRecordSource s : sources){
			if (s.getKeyId().equals(source.getName())){
				return s;
			}
	
		}
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_SourceNotFound, source.getName()));
		return null;
	}
	
	
	private IntelAttributeListItem parseAttributeListItem(LabelUuid attributeListItem, IntelAttribute attribute, Session session, List<String> warnings){
		if (attributeListItem == null) return null;
		
		//search uuids
		IntelAttributeListItem src = (IntelAttributeListItem) session.get(IntelAttributeListItem.class, UuidUtils.stringToUuid(attributeListItem.getUuid()));
		if (src != null && src.getAttribute().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return src;
		}
		
		//search keys
		for (IntelAttributeListItem a : attribute.getAttributeList()){
			if (a.getKeyId().equals(attributeListItem.getName())){
				return a;
			}
	
		}
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_SourceAttributeListItemNotFound, attribute.getName(), attributeListItem.getName()));
		return null;
	}
	
	private Employee parseEmployeeListItem(LabelUuid employeeListItem, IntelAttribute attribute, Session session, List<String> warnings){
		if (employeeListItem == null) return null;
		
		//search uuids
		Employee ee = (Employee) session.get(Employee.class, UuidUtils.stringToUuid(employeeListItem.getUuid()));
		if (ee != null && ee.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return ee;
		}
		
		//search by name
		List<Employee> allEmployees = QueryFactory.buildQuery(session, Employee.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		for (Employee e : allEmployees) {
			if (SmartLabelProvider.getFullLabel(e).equals(employeeListItem.getName())) {
				return e;
			}
		}
		
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_EmployeeNotFound, attribute.getName(), employeeListItem.getName()));
		return null;
	}
	
	/*
	 * search for entity type with given key in the current conservation area
	 */
	private IntelEntityType findEntityType(Session session, String entityTypeKey) {
		return QueryFactory.buildQuery(session,  IntelEntityType.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"keyId", entityTypeKey}).uniqueResult(); //$NON-NLS-1$
	}
	private IntelProfile findProfile(Session session, String profileKey) {
		return QueryFactory.buildQuery(session,  IntelProfile.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"keyId", profileKey}).uniqueResult(); //$NON-NLS-1$
	}
	
	private IntelEntity parseEntity(LabelUuid entityListItem, IntelEntityType type, Session session, List<String> warnings){
		if (entityListItem == null) return null;
		
		//search uuids
		IntelEntity src = (IntelEntity) session.get(IntelEntity.class, UuidUtils.stringToUuid(entityListItem.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea()) && (type == null || src.getEntityType().equals(type))){
			return src;
		}
		
		//search id by type; if no type don't search
		if (type == null) return null;
		Query<IntelEntity> search = QueryFactory.buildQuery(session, IntelEntity.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"entityType", type}); //$NON-NLS-1$
		List<IntelEntity> ids = new ArrayList<>();
		try(ScrollableResults<IntelEntity> results = search.scroll()) {
			while(results.next()) {
				IntelEntity entity = results.get();
				if ( entity.getIdAttributeAsText().equals(entityListItem.getName()) ){
					ids.add(entity);
				}
			}
		}
		if (ids.size() == 1) return ids.get(0);
		if (ids.size() > 1) {
			warnings.add(MessageFormat.format(Messages.RecordXmlImporter_MultiEntitiesFound, entityListItem.getName(), type.getName()));	
			return null;
		}
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_EntityOfTypeNotFound, entityListItem.getName(), type.getName()));
		return null;
	}
	
	
	private Category parseCategory(LabelUuid categoryItem, Session session, List<String> warnings){
		if (categoryItem == null) return null;
		
		//search uuids
		Category src = (Category) session.get(Category.class, UuidUtils.stringToUuid(categoryItem.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea()) ){
			return src;
		}
		
		Category c = QueryFactory.buildQuery(session, Category.class,
				 new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				 new Object[] {"hkey", categoryItem.getName()}).uniqueResult(); //$NON-NLS-1$
		if (c != null) return c;
		
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_CategoryNotFound, categoryItem.getName()));
		return null;
	}
	
	
	private List<IntelObservationAttribute> parseObservationAttributes(List<ObservationAttributeType> attributes, IntelObservation newObservation, Session session, List<String> warnings){
		if (attributes == null || attributes.isEmpty()) return null;

		List<IntelObservationAttribute> newValues = new ArrayList<IntelObservationAttribute>();
		
		for (ObservationAttributeType recordAttribute : attributes){
			UUID attributeUuid = UuidUtils.stringToUuid(recordAttribute.getAttribute().getUuid());
			
			Attribute srcAttribute = null;
			List<Attribute> allAttributes = new ArrayList<Attribute>();
			newObservation.getCategory().getAllAttribute(allAttributes, null);
			for (Attribute a : allAttributes){
				if (a.getUuid().equals(attributeUuid)){
					srcAttribute = a;
					break;
				}
			}
			if (srcAttribute == null){
				for (Attribute a : allAttributes){
					if (a.getKeyId().equals(recordAttribute.getAttribute().getName())){
						srcAttribute = a;
						break;
					}
				}	
			}
			if (srcAttribute == null || !srcAttribute.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_CategoryAttributeNotFound, newObservation.getCategory().getName(), recordAttribute.getAttribute().getName()));
				continue;
			}

			IntelObservationAttribute newValue = new IntelObservationAttribute();
			newValue.setAttribute(srcAttribute);
			newValue.setObservation(newObservation);
			
			switch(srcAttribute.getType()){
				case BOOLEAN:
				case NUMERIC:
					if (recordAttribute.getNumberValue() == null) continue;
					newValue.setNumberValue(recordAttribute.getNumberValue());
					break;
				case DATE:
				case TEXT:
					if (recordAttribute.getStringValue() == null) continue;
					newValue.setStringValue(recordAttribute.getStringValue());
					break;
				case LIST:
					if (recordAttribute.getListValue().isEmpty()) continue;
					
					LabelUuid uuiditem = recordAttribute.getListValue().get(0);
					//search uuid
					UUID listUuid = UuidUtils.stringToUuid(uuiditem.getUuid());
					for (AttributeListItem listItem : srcAttribute.getAttributeList()){
						if (listItem.getUuid().equals(listUuid)){
							newValue.setAttributeListItem(listItem);
							break;
						}
					}
					if (newValue.getAttributeListItem() == null){
						//search key
						for (AttributeListItem listItem : srcAttribute.getAttributeList()){
							if (listItem.getKeyId().equals(uuiditem.getName())){
								newValue.setAttributeListItem(listItem);
								break;
							}
						}
					}
					if (newValue.getAttributeListItem() == null){
						warnings.add(MessageFormat.format(Messages.RecordXmlImporter_AttributeListNotFound, srcAttribute.getName(), uuiditem.getName()));
						continue;
					}
					
					break;
				case MLIST:
					newValue.setAttributeListItems(new ArrayList<>());
					
					for(LabelUuid luuiditem : recordAttribute.getListValue()) {
						
						AttributeListItem match = null;
						
						//search uuid
						UUID listItemUuid = UuidUtils.stringToUuid(luuiditem.getUuid());
						for (AttributeListItem listItem : srcAttribute.getAttributeList()){
							if (listItem.getUuid().equals(listItemUuid)){
								match = listItem;
								break;
							}
						}
						if (match == null){
							//search key
							for (AttributeListItem listItem : srcAttribute.getAttributeList()){
								if (listItem.getKeyId().equals(luuiditem.getName())){
									match = listItem;
									break;
								}
							}
						}
						
						if (match == null){
							warnings.add(MessageFormat.format(Messages.RecordXmlImporter_AttributeListNotFound, srcAttribute.getName(), luuiditem.getName()));
							continue;
						}else {
							IntelObservationAttributeList al = new IntelObservationAttributeList();
							al.setAttributeLisItem(match);
							al.setObservationAttribute(newValue);
							newValue.getAttributeListItems().add(al);
						}
					}
					break;
				case TREE:
					if (recordAttribute.getTreeValue() == null) continue;
					
					AttributeTreeNode treeNode = QueryFactory.buildQuery(session, AttributeTreeNode.class,
							 new Object[] {"attribute",srcAttribute}, //$NON-NLS-1$
							 new Object[] {"uuid", UuidUtils.stringToUuid(recordAttribute.getTreeValue().getUuid())}).uniqueResult(); //$NON-NLS-1$
					
					if (treeNode == null){
						treeNode = QueryFactory.buildQuery(session, AttributeTreeNode.class,
								 new Object[] {"attribute",srcAttribute}, //$NON-NLS-1$
								 new Object[] {"hkey", recordAttribute.getTreeValue().getName()}).uniqueResult(); //$NON-NLS-1$
					}
					if (treeNode == null){
						warnings.add(MessageFormat.format(Messages.RecordXmlImporter_TreeNodeNotFound, srcAttribute.getName(), recordAttribute.getTreeValue().getName()));
						continue;
					}
					newValue.setAttributeTreeNode(treeNode);					
					break;
					
				case LINE:
				case POLYGON:
					GeometrySource src = GeometrySource.UNKNOWN;
					try {
						src = GeometrySource.valueOf(recordAttribute.getStringValue());	
					}catch (Exception ex) {
						warnings.add(MessageFormat.format("Could not parse the value {0} into a valid geometry source for the geometry attribute {1}. The source will be set to unknown.", recordAttribute.getStringValue(), recordAttribute.getAttribute().getName()));
					}
				
					try {
						byte[] geom = Base64.getDecoder().decode(recordAttribute.getGeomValue());
						Geometry g = (new WKBReader()).read(geom);
						GeometryAttributeValue value = new GeometryAttributeValue(g, src);
						newValue.setGeometry(value);	
					}catch (Exception ex) {
						warnings.add(MessageFormat.format("Could not parse a valid geometry from the value associated with geometry attribute {0}. The attribute value will not be imported.", recordAttribute.getAttribute().getName()));
					}
					
					
			}
			
			newValues.add(newValue);
			
		}
		return newValues;		
	}
}
