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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.xml.record.AttachmentType;
import org.wcs.smart.i2.xml.record.LabelUuid;
import org.wcs.smart.i2.xml.record.LocationType;
import org.wcs.smart.i2.xml.record.ObservationAttributeType;
import org.wcs.smart.i2.xml.record.ObservationType;
import org.wcs.smart.i2.xml.record.RecordAttributeType;
import org.wcs.smart.i2.xml.record.RecordType;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

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

	private SimpleDateFormat dateFormater = new SimpleDateFormat(RecordXmlExporter.DATE_FORMAT_STR);
	
	private Session session;
	//records and entity locations to save
	private List<IntelRecord> records;
	private HashMap<IntelRecord, List<IntelEntityLocation>> entityLocations;
	//list of warnings
	private HashMap<Path, List<String>> allWarnings;
	//temporary directories to cleanup after attachments imported
	private List<Path> tempDirs;
	
	public RecordXmlImporter(Session session){
		this.session = session;
		records = new ArrayList<IntelRecord>();
		entityLocations = new HashMap<>();
		allWarnings = new HashMap<Path, List<String>>();
		tempDirs = new ArrayList<Path>();
	}
	
	
	public boolean finish(IEventBroker broker){
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
		
		ArrayList<IntelRecord> loaded = new ArrayList<IntelRecord>();
		
		StringBuilder error = new StringBuilder();
		for (IntelRecord r : records){
			session.beginTransaction();
			try{
				List<IntelEntityLocation> locations = entityLocations.get(r);
				if (r.getAttachments() != null){
					for (IntelRecordAttachment recordAttachment : r.getAttachments()){
//						File baseFolder = new File(recordAttachment.getAttachment().getDatastoreFolderPath(session));
//						String fileName = recordAttachment.getAttachment().getFilename();
//						
//						int cnt = 1;
//						File attachmentFile = new File(baseFolder, fileName);
//						while(attachmentFile.exists()){
//							fileName 
//						}
//						recordAttachment.getAttachment().computeFileLocation(session);
						session.saveOrUpdate(recordAttachment.getAttachment());
					}
				}
				
				session.saveOrUpdate(r);
				if (locations != null){
					for (IntelEntityLocation l : locations) session.saveOrUpdate(l);
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
		}catch (Exception ex){
			error.append(Messages.RecordXmlImporter_EventError + ex.getMessage());
			Intelligence2PlugIn.log(ex.getMessage(), ex);
		}
		cleanUp();
		
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
	
	private void cleanUp(){
		for (Path p : tempDirs){
			try{
				FileUtils.deleteDirectory(p.toFile());
			}catch (Exception ex){
				//TODO:
				ex.printStackTrace();
			}
		}		
	}
	
	
	public void importRecord(Path zipFile, IProgressMonitor monitor) {
		monitor.beginTask(MessageFormat.format(Messages.RecordXmlImporter_TaskName, zipFile.toString()), 1);
		
		List<String> warnings = allWarnings.get(zipFile);
		if (warnings == null){
			warnings = new ArrayList<String>();
			allWarnings.put(zipFile, warnings);
		}
		
		Path tempDir = null;
		try{
			RecordType type = null;
			
			//unzip file
			tempDir = Files.createTempDirectory("smart"); //$NON-NLS-1$
			tempDirs.add(tempDir);
			ZipUtil.unzipFolder(zipFile.toFile(), tempDir.toFile());
			
			//search temp dir for xml file
			Path xmlFile = null;
			try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(tempDir)) {
	            for (Path path : directoryStream) {
	            	if (!Files.isDirectory(path) && path.toString().endsWith(".xml")){ //$NON-NLS-1$
	            		xmlFile = path;
	            		break;
	            	}
	            }
	        } catch (IOException ex) {}
			
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
			List<IntelEntityLocation> entitylocations = new ArrayList<>();
			IntelRecord newRecord = new IntelRecord();
			newRecord.setConservationArea(SmartDB.getCurrentConservationArea());
			newRecord.setTitle(type.getTitle().trim());
			if (type.getScratchpad() != null) newRecord.setComment(type.getScratchpad());
			if (type.getNarrative() != null) newRecord.setDescription(type.getNarrative());

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
					IntelEntity entity = parseEntity(e, session, warnings);
					if (entity != null) {
						IntelEntityRecord r = new IntelEntityRecord();
						r.setEntity(entity);
						r.setRecord(newRecord);
						newRecord.getEntities().add(r);
					}
				}
			}

			if (type.getAttachments() != null && !type.getAttachments().isEmpty()) {
				// TODO: entity links?
				newRecord.setAttachments(new ArrayList<IntelRecordAttachment>());
				for (AttachmentType xmlAttachment : type.getAttachments()) {
					Path importFile = xmlFile.getParent().resolve(RecordXmlExporter.ATTACHMENT_DIR).resolve(xmlAttachment.getFilename());
					IntelAttachment newAttachment = new IntelAttachment();
					newAttachment.setCopyFromLocation(importFile.toFile());
					newAttachment.setFilename(xmlAttachment.getFilename());
					newAttachment.setConservationArea(SmartDB.getCurrentConservationArea());
					newAttachment.setDateCreated(new Date());
					newAttachment.setCreatedBy(SmartDB.getCurrentEmployee());
					
					IntelRecordAttachment attach = new IntelRecordAttachment();
					attach.setAttachment(newAttachment);
					attach.setRecord(newRecord);
					newRecord.getAttachments().add(attach);
				}
			}
			
			if (type.getLocations() != null && !type.getLocations().isEmpty()) {
				newRecord.setLocations(new ArrayList<IntelLocation>());

				for (LocationType xmlLocation : type.getLocations()) {

					IntelLocation newLocation = new IntelLocation();
					if (xmlLocation.getComment() != null) newLocation.setComment(xmlLocation.getComment());
					if (xmlLocation.getId() != null) newLocation.setId(xmlLocation.getId());
					newLocation.setDateTime(dateFormater.parse(xmlLocation.getDatetime()));
					newLocation.setConservationArea(SmartDB.getCurrentConservationArea());
					newLocation.setGeom(xmlLocation.getGeometry());
					newLocation.setRecord(newRecord);
					newRecord.getLocations().add(newLocation);

					if (xmlLocation.getEntities() != null && !xmlLocation.getEntities().isEmpty()) {

						for (LabelUuid xmlEntity : xmlLocation.getEntities()) {
							IntelEntity entity = parseEntity(xmlEntity, session, warnings);
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
		
		}finally{
			monitor.done();
		}
	}
	
	private List<IntelRecordAttributeValue> parseRecordSourceAttributes(List<RecordAttributeType> attributes, IntelRecord record, Session session, List<String> warnings){
		if (attributes == null || attributes.isEmpty()) return null;
		List<IntelRecordAttributeValue> newValues = new ArrayList<IntelRecordAttributeValue>();
		
		for (RecordAttributeType recordAttribute : attributes){
			UUID attributeUuid = UuidUtils.stringToUuid(recordAttribute.getRecordAttribute().getUuid());
			
			IntelRecordSourceAttribute srcAttribute = null;
			for (IntelRecordSourceAttribute a : record.getRecordSource().getAttributes()){
				if (a.getUuid().equals(attributeUuid)){
					srcAttribute = a;
					break;
				}
			}

			if (srcAttribute == null){
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_SourceAttributenotFound, recordAttribute.getRecordAttribute().getName(), record.getRecordSource().getName()));
				continue;
			}
			
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
					if (!srcAttribute.getIsMultiple() && listItems.size() > 1){
						listItems = Collections.singletonList(listItems.get(0));
					}
					newValue.setAttributeListItems(listItems);
					
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
					UUID item = parseEntity(list,srcAttribute.getEntityType(), session, warnings);
					if (item != null){
						IntelRecordAttributeValueList newItem = new IntelRecordAttributeValueList();
						newItem.getId().setElementUuid(item);
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
	
	
	@SuppressWarnings("unchecked")
	private IntelRecordSource parseRecordSource(LabelUuid source, Session session, List<String> warnings){
		if (source == null) return null;
		
		//search uuids
		IntelRecordSource src = (IntelRecordSource) session.get(IntelRecordSource.class, UuidUtils.stringToUuid(source.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
			return src;
		}
		
		//search keys
		List<IntelRecordSource> sources = session.createCriteria(IntelRecordSource.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list();
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
	
	private UUID parseEntity(LabelUuid entityListItem, IntelEntityType type, Session session, List<String> warnings){
		if (entityListItem == null) return null;
		
		//search uuids
		IntelEntity src = (IntelEntity) session.get(IntelEntity.class, UuidUtils.stringToUuid(entityListItem.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea()) && src.getEntityType().equals(type)){
			return src.getUuid();
		}
		
		//search ids??
		//TODO:
		
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_EntityOfTypeNotFound, entityListItem.getName(), type.getName()));
		return null;
	}
	
	private IntelEntity parseEntity(LabelUuid entityListItem, Session session, List<String> warnings){
		if (entityListItem == null) return null;
		
		//search uuids
		IntelEntity src = (IntelEntity) session.get(IntelEntity.class, UuidUtils.stringToUuid(entityListItem.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea()) ){
			return src;
		}
		
		//search ids??
		//TODO:
		
		//add to warnings;
		warnings.add(MessageFormat.format(Messages.RecordXmlImporter_EntityNotFound, entityListItem.getName()));
		return null;
	}
	
	
	private Category parseCategory(LabelUuid categoryItem, Session session, List<String> warnings){
		if (categoryItem == null) return null;
		
		//search uuids
		Category src = (Category) session.get(Category.class, UuidUtils.stringToUuid(categoryItem.getUuid()));
		if (src != null && src.getConservationArea().equals(SmartDB.getCurrentConservationArea()) ){
			return src;
		}
		
		Category c = (Category) session.createCriteria(Category.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("hkey", categoryItem.getName())) //$NON-NLS-1$
				.uniqueResult();
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
			
			CategoryAttribute srcAttribute = null;
			List<CategoryAttribute> allAttributes = new ArrayList<CategoryAttribute>();
			newObservation.getCategory().getAllCategoryAttribute(allAttributes, null);
			for (CategoryAttribute a : allAttributes){
				if (a.getAttribute().getUuid().equals(attributeUuid)){
					srcAttribute = a;
					break;
				}
			}
			if (srcAttribute == null){
				for (CategoryAttribute a : allAttributes){
					if (a.getAttribute().getKeyId().equals(recordAttribute.getAttribute().getName())){
						srcAttribute = a;
						break;
					}
				}	
			}
			if (srcAttribute == null || !srcAttribute.getAttribute().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
				warnings.add(MessageFormat.format(Messages.RecordXmlImporter_CategoryAttributeNotFound, newObservation.getCategory().getName(), recordAttribute.getAttribute().getName()));
				continue;
			}

			IntelObservationAttribute newValue = new IntelObservationAttribute();
			newValue.setAttribute(srcAttribute.getAttribute());
			newValue.setObservation(newObservation);
			
			switch(srcAttribute.getAttribute().getType()){
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
					if (recordAttribute.getListValue() == null) continue;
					
					//search uuid
					for (AttributeListItem listItem : srcAttribute.getAttribute().getAttributeList()){
						if (listItem.getUuid().equals(recordAttribute.getListValue().getUuid())){
							newValue.setAttributeListItem(listItem);
							break;
						}
					}
					if (newValue.getAttributeListItem() == null){
						//search key
						for (AttributeListItem listItem : srcAttribute.getAttribute().getAttributeList()){
							if (listItem.getKeyId().equals(recordAttribute.getListValue().getName())){
								newValue.setAttributeListItem(listItem);
								break;
							}
						}
					}
					if (newValue.getAttributeListItem() == null){
						warnings.add(MessageFormat.format(Messages.RecordXmlImporter_AttributeListNotFound, srcAttribute.getAttribute().getName(), recordAttribute.getListValue().getName()));
						continue;
					}
					
					break;
				case TREE:
					if (recordAttribute.getTreeValue() == null) continue;
					
					AttributeTreeNode treeNode = (AttributeTreeNode)session.createCriteria(AttributeTreeNode.class)
						.add(Restrictions.eq("attribute", srcAttribute.getAttribute())) //$NON-NLS-1$
						.add(Restrictions.eq("uuid", UuidUtils.stringToUuid(recordAttribute.getTreeValue().getUuid()))) //$NON-NLS-1$
						.uniqueResult();
					if (treeNode == null){
						treeNode = (AttributeTreeNode)session.createCriteria(AttributeTreeNode.class)
								.add(Restrictions.eq("attribute", srcAttribute.getAttribute())) //$NON-NLS-1$
								.add(Restrictions.eq("hkey", recordAttribute.getTreeValue().getName())) //$NON-NLS-1$
								.uniqueResult();
					}
					if (treeNode == null){
						warnings.add(MessageFormat.format(Messages.RecordXmlImporter_TreeNodeNotFound, srcAttribute.getAttribute().getName(), recordAttribute.getTreeValue().getName()));
						continue;
					}
					newValue.setAttributeTreeNode(treeNode);					
					break;
			}
			
			newValues.add(newValue);
			
		}
		return newValues;		
	}
}
