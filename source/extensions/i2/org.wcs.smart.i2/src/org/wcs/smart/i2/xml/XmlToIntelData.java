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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.xml.model.Attribute;
import org.wcs.smart.i2.xml.model.AttributeListItem;
import org.wcs.smart.i2.xml.model.EntityType;
import org.wcs.smart.i2.xml.model.EntityTypeAttribute;
import org.wcs.smart.i2.xml.model.EntityTypeAttributeGroup;
import org.wcs.smart.i2.xml.model.IntelligenceData;
import org.wcs.smart.i2.xml.model.NamedItem;
import org.wcs.smart.i2.xml.model.RecordSource;
import org.wcs.smart.i2.xml.model.RecordSourceAttribute;
import org.wcs.smart.i2.xml.model.RelationshipGroup;
import org.wcs.smart.i2.xml.model.RelationshipType;
import org.wcs.smart.i2.xml.model.RelationshipTypeAttribute;
import org.wcs.smart.util.ZipUtil;

import com.ibm.icu.text.MessageFormat;

/**
 * Converts xml data to intelligence model objects
 * 
 * @author Emily
 *
 */
public class XmlToIntelData {

	private ConservationArea ca;
	private List<String> warnings;
	private Path rootPath;
	private Session session;
	
	public XmlToIntelData(ConservationArea ca) {
		this.ca = ca;
	}
	
	public void importXmlData(Path zipFile, IProgressMonitor monitor) throws IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.XmlToIntelData_conversiontask, 10);
		warnings = new ArrayList<>();
		rootPath = Files.createTempDirectory("smart." + System.nanoTime()); //$NON-NLS-1$
		try {
			try {
				ZipUtil.unzipFolder(zipFile.toFile(), rootPath.toFile());
			} catch (Exception e) {
				throw new IOException(e);
			}
			
			Path xmlFile = rootPath.resolve(IntelDataToXml.XML_DATA_FILENAME);
			IntelligenceData data = null;
			try {
				progress.split(1);
				progress.subTask(Messages.XmlToIntelData_readingfileTask);
				data = readXmlFile(xmlFile);
			}catch (Exception ex) {
				throw new IOException(ex);
			}
			
			try (Session session = HibernateManager.openSession()){
				toIntelData(data, session, progress.split(9));
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
	
	private IntelligenceData readXmlFile(Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(IntelDataToXml.METADATA_CLASSES_PACKAGE);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<IntelligenceData> o = (JAXBElement<IntelligenceData>) unmarshaller.unmarshal(xmlFile.toFile());
		return o.getValue();
	}
	
	private void toIntelData(IntelligenceData data, Session session, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 7);
		
		this.session = session;

		//process attributes
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_attributesTask);
		List<IntelAttribute> attributes = processAttributes(data.getAttributes());
		
		//attribute mappings
		HashMap<String, IntelAttribute> attributeMapping = new HashMap<>();
		List<IntelAttribute> existingAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, "conservationArea", ca).list(); //$NON-NLS-1$
		existingAttributes.forEach(e->attributeMapping.put(e.getKeyId(), e));
		attributes.forEach(e->attributeMapping.put(e.getKeyId(), e));
		
		//process entities
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_entitytypesTask);
		List<IntelEntityType> entities = proecessEntityTypes(data.getEntities(), attributeMapping);
		
		//entity mappings
		HashMap<String, IntelEntityType> entityMappings = new HashMap<>();
		List<IntelEntityType> existingEntities = QueryFactory.buildQuery(session, IntelEntityType.class, "conservationArea", ca).list(); //$NON-NLS-1$
		existingEntities.forEach(e->entityMappings.put(e.getKeyId(), e));
		entities.forEach(e->entityMappings.put(e.getKeyId(), e));
		
		//process & save record sources
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_recourdsourceTask);
		List<IntelRecordSource> recordSources = processRecordSources(data.getRecordSource(), attributeMapping, entityMappings);

		//processing relationship groups
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_relationshipgroupsTask);
		List<IntelRelationshipGroup> relationshipGroups = processRelationshipGroups(data.getRelationshipGroups());
				
		//relationship group mappings
		HashMap<String, IntelRelationshipGroup> relationshipGroupMappings = new HashMap<>();
		List<IntelRelationshipGroup> existingGroups = QueryFactory.buildQuery(session, IntelRelationshipGroup.class, "conservationArea", ca).list(); //$NON-NLS-1$
		existingGroups.forEach(e->relationshipGroupMappings.put(e.getKeyId(), e));
		relationshipGroups.forEach(e->relationshipGroupMappings.put(e.getKeyId(), e));
		
		//process & save relationship type
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_relationshiptypesTask);
		List<IntelRelationshipType> relationshipTypes = processRelationshipTypes(data.getRelationships(), attributeMapping, entityMappings, relationshipGroupMappings);
		
		//validate warnings with user
		if (!warnings.isEmpty()) {
			boolean[] ret = new boolean[] {false};
			Display.getDefault().syncExec(()->{
				WarningDialog warningDialog = new WarningDialog(Display.getDefault().getActiveShell(), Messages.XmlToIntelData_WarningsTitle, Messages.XmlToIntelData_WarningsMsg, warnings, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
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
		session.beginTransaction();
		try {
			attributes.forEach(a->session.save(a));
			entities.forEach(a->session.save(a));
			entityMappings.values().forEach(et->{
				et.getAttributes().forEach(eta->{
					session.save(eta);
					if (eta.getAttributeGroup() != null) session.save(eta.getAttributeGroup());	
				});
			});
			recordSources.forEach(a->session.save(a));
			relationshipGroups.forEach(a->session.save(a));
			relationshipTypes.forEach(a->session.save(a));
			session.getTransaction().commit();
		}catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		List<String> copyErrors = new ArrayList<String>();
		entities.forEach(e->{
			if (e.getBirtTemplate() != null) {
				Path src = rootPath.resolve(e.getBirtTemplate());
				Path trg = IntelReportManager.INSTANCE.getEntityTemplate(e);
				if(Files.exists(src)) {
					try {
						if (!Files.exists(trg.getParent())) {
							Files.createDirectories(trg.getParent());
						}
						Files.copy(src, trg, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e1) {
						copyErrors.add(MessageFormat.format(Messages.XmlToIntelData_EntityCopyError, e.getName()));
						Intelligence2PlugIn.log(e1.getMessage(), e1);
					}
				}
			}
		});
		if (data.getRecordTemplate() != null) {
			Path trg = IntelReportManager.INSTANCE.getRecordTemplate(ca);
			Path src = rootPath.resolve(trg.getFileName());
			try {
				if (!Files.exists(trg.getParent())) {
					Files.createDirectories(trg.getParent());
				}
				Files.copy(src, trg, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				copyErrors.add(Messages.XmlToIntelData_SourceCopyError);
				Intelligence2PlugIn.log(e1.getMessage(), e1);
			}
		}
		
		if (!copyErrors.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.XmlToIntelData_BirtCopyErrors);
			sb.append("\n"); //$NON-NLS-1$
			for (String x : copyErrors) {
				sb.append(x);
				sb.append("\n"); //$NON-NLS-1$
			}
			Display.getDefault().syncExec(()->{
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.XmlToIntelData_CopyErrorTitle, sb.toString());
			});	
		}
		progress.done();
		
		StringBuilder sb = new StringBuilder();
		if (attributes.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToIntelData_AtributeStatus, attributes.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (entities.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToIntelData_EntityTypesStatus, entities.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (relationshipGroups.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToIntelData_RelationshipGroupStatus, relationshipGroups.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (relationshipTypes.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToIntelData_RelationshipTypeStatus, relationshipTypes.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (recordSources.size() > 0) {
			sb.append(MessageFormat.format(Messages.XmlToIntelData_RecordSourceStatus, recordSources.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (sb.length() == 0) {
			sb.append(Messages.XmlToIntelData_NothingImorted2);
		}else {
			sb.insert(0, Messages.XmlToIntelData_DataImportedMsg);
		}
		
		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.XmlToIntelData_NothingImported, sb.toString());
		});

	}
	
	
	private List<IntelRelationshipType> processRelationshipTypes(List<RelationshipType> xmlTypes, HashMap<String, IntelAttribute> attributes, HashMap<String, IntelEntityType> entities, HashMap<String, IntelRelationshipGroup> groups){
		List<IntelRelationshipType> newSources = new ArrayList<>();
		
		for (RelationshipType xmlSource : xmlTypes) {
			IntelRelationshipType src = new IntelRelationshipType();
			src.setConservationArea(ca);
			src.setIcon(xmlSource.getIcon());
			src.setKeyId(xmlSource.getKey());
			updateNames(src, xmlSource.getNames());
			
			if (xmlSource.getGroupKey() != null) {
				IntelRelationshipGroup group = groups.get(xmlSource.getGroupKey());
				if (group == null) {
					warnings.add(MessageFormat.format(Messages.XmlToIntelData_RelationshipGroupNotFound, src.getKeyId(), src.getName()));
				}else {
					src.setRelationshipGroup(group);
				}
			}
			
			if (xmlSource.getSrcTypeKey() != null) {
				IntelEntityType srcType = entities.get(xmlSource.getSrcTypeKey());
				if (srcType == null) {
					warnings.add(MessageFormat.format(Messages.XmlToIntelData_RelationshipSourceEntityNotFound, xmlSource.getSrcTypeKey(), src.getName()));
				}else {
					src.setSourceEntityType(srcType);
				}
			}
			if (xmlSource.getTargetTypeKey() != null) {
				IntelEntityType trgType = entities.get(xmlSource.getTargetTypeKey());
				if (trgType == null) {
					warnings.add(MessageFormat.format(Messages.XmlToIntelData_RelationshipTargetEntityNotFound, xmlSource.getSrcTypeKey(), src.getName()));
				}else {
					src.setTargetEntityType(trgType);
				}
			}
			
			src.setAttributes(new ArrayList<>());
			
			for (RelationshipTypeAttribute xmlAttribute : xmlSource.getAttributes()) {
				IntelRelationshipTypeAttribute newAttribute = new IntelRelationshipTypeAttribute();
				
				IntelAttribute attribute = attributes.get(xmlAttribute.getAttributeKey());
				if (attribute == null) {					
					//skip
					warnings.add(MessageFormat.format(Messages.XmlToIntelData_RelationshipTypeAttributeExists, xmlAttribute.getAttributeKey(), src.getName(), src.getKeyId()));
					continue;	
				}
				newAttribute.setAttribute(attribute);
				newAttribute.setOrder(xmlAttribute.getOrder());
				newAttribute.setRelationshipType(src);
				src.getAttributes().add(newAttribute);
			}
			
			newSources.add(src);
		}
		
		//validate 
		List<IntelRelationshipType> toAdd = new ArrayList<>();
		List<IntelRelationshipType> existingSources = QueryFactory.buildQuery(session, IntelRelationshipType.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (IntelRelationshipType newSource : newSources) {
			IntelRelationshipType found = null;
			for (IntelRelationshipType existingSource : existingSources) {
				if (existingSource.getKeyId().equals(newSource.getKeyId())) {
					found = existingSource;
					break;
				}
			}
			if (found == null) {
				// we need to add this attribute
				toAdd.add(newSource);
			} else {
				warnings.add(MessageFormat.format(
						Messages.XmlToIntelData_RelationshipTypeExists,
						found.getName(), found.getKeyId()));
			}
		}
		return toAdd;
	}
	
	
	private List<IntelRelationshipGroup> processRelationshipGroups(List<RelationshipGroup> xmlGroups){
		List<IntelRelationshipGroup> newGroups = new ArrayList<>();
		
		for (RelationshipGroup xmlGroup : xmlGroups) {
			IntelRelationshipGroup group = new IntelRelationshipGroup();
			group.setConservationArea(ca);
			group.setKeyId(xmlGroup.getKey());
			group.setRelationshipTypes(new ArrayList<>());
			updateNames(group, xmlGroup.getNames());
			
			newGroups.add(group);
		}
		
		//validate 
		List<IntelRelationshipGroup> toAdd = new ArrayList<>();
		List<IntelRelationshipGroup> existingSources = QueryFactory.buildQuery(session, IntelRelationshipGroup.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (IntelRelationshipGroup newSource : newGroups) {
			IntelRelationshipGroup found = null;
			for (IntelRelationshipGroup existingSource : existingSources) {
				if (existingSource.getKeyId().equals(newSource.getKeyId())) {
					found = existingSource;
					break;
				}
			}
			if (found == null) {
				// we need to add this attribute
				toAdd.add(newSource);
			}
		}
		return toAdd;
	}
	
	private List<IntelEntityType> proecessEntityTypes(List<EntityType> xmlTypes, HashMap<String, IntelAttribute> attributes){
		List<IntelEntityType> newSources = new ArrayList<>();
		
		for (EntityType xmlSource : xmlTypes) {
			IntelEntityType src = new IntelEntityType();
			src.setConservationArea(ca);
			src.setIcon(xmlSource.getIcon());
			src.setKeyId(xmlSource.getKey());
			updateNames(src, xmlSource.getNames());
			
			IntelAttribute idAttribute = attributes.get(xmlSource.getIdAttribute());
			if (idAttribute == null) {
				//ERROR
				warnings.add(MessageFormat.format(Messages.XmlToIntelData_EntityTypeIdAttributeNotFound, xmlSource.getIdAttribute(), src.getName(), src.getKeyId()));
				continue;
			}
			src.setIdAttribute(idAttribute);
			src.setBirtTemplate(xmlSource.getReportTemplate());
			
			HashMap<String, IntelEntityTypeAttributeGroup> groups = new HashMap<>();
			for (EntityTypeAttributeGroup xmlGroup : xmlSource.getGroups()) {
				IntelEntityTypeAttributeGroup group = new IntelEntityTypeAttributeGroup();
				group.setEntityType(src);
				group.setOrder(xmlGroup.getOrder());
				updateNames(group, xmlGroup.getNames());
				groups.put(xmlGroup.getId(), group);
			}
			
			src.setAttributes(new ArrayList<>());
			for (EntityTypeAttribute xmlAttribute : xmlSource.getAttributes()) {
				IntelEntityTypeAttribute newAttribute = new IntelEntityTypeAttribute();
				if (xmlAttribute.getGroupKey() != null && groups.containsKey(xmlAttribute.getGroupKey())) {
					newAttribute.setAttributeGroup(groups.get(xmlAttribute.getGroupKey()));	
				}
				IntelAttribute attribute = attributes.get(xmlAttribute.getAttributeKey());
				if (attribute == null) {
					//skip
					warnings.add(MessageFormat.format(Messages.XmlToIntelData_EntityTypeAttributeNotFound, xmlSource.getIdAttribute(), src.getName(), src.getKeyId()));
					continue;
				}
				newAttribute.setAttribute(attribute);
				newAttribute.setEntityType(src);
				newAttribute.setOrder(xmlAttribute.getOrder());
				src.getAttributes().add(newAttribute);
			}
			newSources.add(src);
		}
		
		//validate 
		List<IntelEntityType> toAdd = new ArrayList<>();
		List<IntelEntityType> existingSources = QueryFactory.buildQuery(session, IntelEntityType.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (IntelEntityType newSource : newSources) {
			IntelEntityType found = null;
			for (IntelEntityType existingSource : existingSources) {
				if (existingSource.getKeyId().equals(newSource.getKeyId())) {
					found = existingSource;
					break;
				}
			}
			if (found == null) {
				// we need to add this attribute
				toAdd.add(newSource);
			} else {
				warnings.add(MessageFormat.format(
						Messages.XmlToIntelData_EntityTypeExists,
						found.getName(), found.getKeyId()));
			}
		}
		return toAdd;
	}

	
	
	private List<IntelRecordSource> processRecordSources(List<RecordSource> xmlSources, HashMap<String, IntelAttribute> attributes, HashMap<String, IntelEntityType> entityTypes){
		List<IntelRecordSource> newSources = new ArrayList<>();
		
		for (RecordSource xmlSource : xmlSources) {
			IntelRecordSource src = new IntelRecordSource();
			src.setConservationArea(ca);
			src.setIcon(xmlSource.getIcon());
			src.setKeyId(xmlSource.getKey());
			updateNames(src, xmlSource.getNames());
			if (xmlSource.getAttributes() != null) {
				src.setAttributes(new ArrayList<>());
				for (RecordSourceAttribute xmlSrcAttribute : xmlSource.getAttributes()) {
					IntelRecordSourceAttribute attribute = new IntelRecordSourceAttribute();
					
					if (xmlSrcAttribute.getAttributeKey() != null) {
						IntelAttribute srcAttribute = attributes.get(xmlSrcAttribute.getAttributeKey());
						if (srcAttribute == null) {
							warnings.add(MessageFormat.format(Messages.XmlToIntelData_RecordSourceAttributeNotFound,
									xmlSrcAttribute.getAttributeKey(), src.getName()
									));
							continue;
						}
						attribute.setAttribute(srcAttribute);
					}else if(xmlSrcAttribute.getEntityTypeKey() != null) {
						IntelEntityType srcType = entityTypes.get(xmlSrcAttribute.getEntityTypeKey());
						if (srcType == null) {
							warnings.add(MessageFormat.format(Messages.XmlToIntelData_RecordSourceEntityTypeNotFound,
									xmlSrcAttribute.getEntityTypeKey(), src.getName()
									));

							continue;
						}
						attribute.setEntityType(srcType);
					}
					if (attribute.getAttribute() == null && attribute.getEntityType() == null) {
						//ERROR
						warnings.add(MessageFormat.format(Messages.XmlToIntelData_RecordSourceAttributeNoValidReference,
								xmlSrcAttribute.getAttributeKey() == null ? xmlSrcAttribute.getAttributeKey() : xmlSrcAttribute.getEntityTypeKey()
								));
						continue;
					}
					attribute.setIsMultiple(xmlSrcAttribute.isIsMulit());
					attribute.setOrder(xmlSrcAttribute.getOrder());
					attribute.setSource(src);
					if (!xmlSrcAttribute.getNames().isEmpty()) {
						updateNames(attribute, xmlSrcAttribute.getNames());
					}
					src.getAttributes().add(attribute);
				}
			}
			newSources.add(src);
		}
		
		//validate 
		List<IntelRecordSource> toAdd = new ArrayList<>();
		List<IntelRecordSource> existingSources = QueryFactory.buildQuery(session, IntelRecordSource.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (IntelRecordSource newSource : newSources) {
			IntelRecordSource found = null;
			for (IntelRecordSource existingSource : existingSources) {
				if (existingSource.getKeyId().equals(newSource.getKeyId())) {
					found = existingSource; 
					break;
				}
			}
			if (found == null) {
				//we need to add this attribute
				toAdd.add(newSource);
			}else {
				warnings.add(MessageFormat.format(Messages.XmlToIntelData_RecordSourceExists, found.getName(), found.getKeyId())); 
			}
		}
		return toAdd;
	}
	
	private List<IntelAttribute> processAttributes(List<Attribute> xmlAttributes) {
		List<IntelAttribute> newAttributes = new ArrayList<IntelAttribute>();
		
		for (Attribute xmlAttribute : xmlAttributes) {
			IntelAttribute a = new IntelAttribute();
			a.setConservationArea(ca);
			a.setKeyId(xmlAttribute.getKey());
			a.setType(IntelAttribute.AttributeType.valueOf(xmlAttribute.getType().name()));
			
			updateNames(a, xmlAttribute.getNames());
			
			if (a.getType() == AttributeType.LIST && xmlAttribute.getListValues() != null) {
				a.setAttributeList(new ArrayList<>());
				for (AttributeListItem xmlListItem : xmlAttribute.getListValues()) {
					IntelAttributeListItem li = new IntelAttributeListItem();
					li.setAttribute(a);
					li.setKeyId(xmlListItem.getKey());
					updateNames(li, xmlListItem.getNames());		
					
					a.getAttributeList().add(li);
				}
			}
			newAttributes.add(a);
		}
		
		//validate 
		List<IntelAttribute> toAdd = new ArrayList<>();
		List<IntelAttribute> existingAttributes = QueryFactory.buildQuery(session, IntelAttribute.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (IntelAttribute newAttribute : newAttributes) {
			IntelAttribute found = null;
			for (IntelAttribute existingAttribute : existingAttributes) {
				if (existingAttribute.getKeyId().equals(newAttribute.getKeyId())) {
					found = existingAttribute; 
					break;
				}
			}
			if (found == null) {
				//we need to add this attribute
				toAdd.add(newAttribute);
			}else {
				if (!found.getType().equals(newAttribute.getType())){
					//different attribute types; this is a warning but not an error
					warnings.add(MessageFormat.format(Messages.XmlToIntelData_AttributeExistsDifferentType, found.getName(), found.getKeyId(), found.getType().getGuiName(Locale.getDefault()), newAttribute.getType().getGuiName(Locale.getDefault()))); 
				}
			}
		}
		
		return toAdd;
	}
	
	private void updateNames(org.wcs.smart.ca.NamedItem item, Collection<NamedItem> names) {
		String defaultValue = null;
		String blankName = Messages.XmlToIntelData_AttributeDefaultName;
		if (!names.isEmpty()) blankName = names.iterator().next().getValue();
		
		for (NamedItem ni : names) {
			Language l = findLanguage(ni.getLanguageCode());
			if (ni.isIsDefault()) defaultValue = ni.getValue();
			if (l != null) {
				item.updateName(l, ni.getValue());
			}
		}
		//ensure we have a default name
		if (item.findNameNull(ca.getDefaultLanguage())== null) {
			if (defaultValue != null) {
				item.updateName(ca.getDefaultLanguage(), defaultValue);
			}else {
				item.updateName(ca.getDefaultLanguage(), blankName);
			}
		}
		item.setName(item.getDefaultName());
	}
	private Language findLanguage(String code) {
		for (Language l : ca.getLanguages()) {
			if (l.getCode().equals(code)) return l;
		}
		return null;
	}
}
