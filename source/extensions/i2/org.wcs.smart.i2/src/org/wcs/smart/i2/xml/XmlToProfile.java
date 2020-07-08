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

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelPermission;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
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
import org.wcs.smart.i2.xml.model.NamedItem;
import org.wcs.smart.i2.xml.model.Profile;
import org.wcs.smart.i2.xml.model.RecordSource;
import org.wcs.smart.i2.xml.model.RecordSourceAttribute;
import org.wcs.smart.i2.xml.model.RelationshipGroup;
import org.wcs.smart.i2.xml.model.RelationshipType;
import org.wcs.smart.i2.xml.model.RelationshipTypeAttribute;
import org.wcs.smart.util.ZipUtil;
/**
 * Converts xml data to intelligence model objects
 * 
 * @author Emily
 *
 */
public class XmlToProfile {

	private ConservationArea ca;
	private List<String> warnings;
	private Path rootPath;
	private Session session;
	
	public XmlToProfile(ConservationArea ca) {
		this.ca = ca;
	}
	
	public IntelProfile importXmlData(Path zipFile, IProgressMonitor monitor, IEventBroker eventBroker) throws IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.XmlToIntelData_conversiontask, 10);
		warnings = new ArrayList<>();
		rootPath = Files.createTempDirectory("smart." + System.nanoTime()); //$NON-NLS-1$
		try {
			try {
				ZipUtil.unzipFolder(zipFile, rootPath);
			} catch (Exception e) {
				throw new IOException(e);
			}
			
			//lets find all xml files
			
			Path xmlFile = null;
			List<Path> files = Files.list(rootPath).collect(Collectors.toList());
			for (Path f : files) {
				if (f.getFileName().toString().endsWith(".xml")) { //$NON-NLS-1$
					xmlFile = f;
				}
			}
			
			try (Session session = HibernateManager.openSession()){
				Profile xml = readXmlFile(xmlFile);
				return toIntelData(xml, rootPath, session, eventBroker, progress.split(1));
			
			}catch (Exception ex) {
				Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
			}

		}finally{
			//clean up
			try {
				FileUtils.deleteDirectory(rootPath.toFile());
			}catch (Exception ex) {
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
		return null;
	}
	
	public Profile readXmlFile(Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ProfileToXml.METADATA_CLASSES_PACKAGE);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<Profile> o = (JAXBElement<Profile>) unmarshaller.unmarshal(xmlFile.toFile());
		return o.getValue();
	}
	
	public Profile readXmlFile(InputStream xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ProfileToXml.METADATA_CLASSES_PACKAGE);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<Profile> o = (JAXBElement<Profile>) unmarshaller.unmarshal(xmlFile);
		return o.getValue();
	}
	
	private IntelProfile toIntelData(Profile data, Path filesDir, Session session,  IEventBroker eventBroker, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, 7);
		
		this.session = session;
		
		IntelProfile existing = QueryFactory.buildQuery(session, IntelProfile.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"keyId", data.getKey()}).uniqueResult(); //$NON-NLS-1$
		if (existing != null) {
			throw new Exception (MessageFormat.format(Messages.XmlToProfile_KeyExists, data.getKey()));
		}

		IntelProfile profile = new IntelProfile();
		profile.setKeyId(data.getKey());
		profile.setColorObj(Color.decode("#"+data.getColor())); //$NON-NLS-1$
		profile.setConservationArea(ca);
		profile.setRecordSources(new HashSet<>());
		profile.setEntityTypes(new HashSet<>());
		updateNames(profile, data.getNames());
		
		
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
		
		for (IntelEntityType e : entities) {
			IntelProfileEntityType map = new IntelProfileEntityType();
			map.setEntityType(e);
			map.setProfile(profile);
			
			profile.getEntityTypes().add(map);
			e.getProfiles().add(map);
		}
		
		//entity mappings
		HashMap<String, IntelEntityType> entityMappings = new HashMap<>();
		List<IntelEntityType> existingEntities = QueryFactory.buildQuery(session, IntelEntityType.class, "conservationArea", ca).list(); //$NON-NLS-1$
		existingEntities.forEach(e->entityMappings.put(e.getKeyId(), e));
		entities.forEach(e->entityMappings.put(e.getKeyId(), e));
		
		//process & save record sources
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_recourdsourceTask);
		List<IntelRecordSource> recordSources = processRecordSources(data.getRecordSource(), attributeMapping, entityMappings);
		for (IntelRecordSource s : recordSources) {
			IntelProfileRecordSource ps = new IntelProfileRecordSource();
			ps.getId().setProfile(profile);
			ps.getId().setRecordSource(s);
			
			s.getProfiles().add(ps);
			profile.getRecordSources().add(ps);
		}
		
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
		List<IntelRelationshipType> relationshipTypes = processRelationshipTypes(data.getRelationships(), attributeMapping, entityMappings, relationshipGroupMappings, profile);
		

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
			if (!ret[0] ) return null;
		}
		
		//save changes
		progress.split(1);
		progress.subTask(Messages.XmlToIntelData_SaveTask);
		session.beginTransaction();
		try {
			
			List<IntelProfileEntityType> types = new ArrayList<>(profile.getEntityTypes());
			profile.getEntityTypes().clear();
			session.save(profile);
			for (IntelProfileEntityType ipe : types) {
				for (IntelEntityTypeAttribute att: ipe.getEntityType().getAttributes()) {
					session.saveOrUpdate(att.getAttribute());
				}
			}
			for (IntelProfileEntityType ipe : types) {
				session.saveOrUpdate(ipe.getEntityType());
			}
			profile.getEntityTypes().addAll(types);
			session.flush();		
			
			IntelPermission ip = new IntelPermission();
			ip.setEmployee(SmartDB.getCurrentEmployee());
			ip.setPermission(IntelPermission.ADMIN);
			ip.setProfile(profile);
					
			session.save(ip);
			
			attributes.forEach(a->session.save(a));
			
			for (IntelEntityType a : entities) {
				if (a.getUuid() == null) session.save(a);
			}	
			entityMappings.values().forEach(et->{
			
			et.getAttributes().forEach(eta->{
					session.save(eta);
					if (eta.getAttributeGroup() != null) session.save(eta.getAttributeGroup());	
				});
			});
			for (IntelRecordSource a : recordSources) {
				if (a.getUuid() == null) session.save(a);
			}
			
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
				Path src = rootPath.resolve(data.getKey()).resolve(e.getBirtTemplate());
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
				if (Files.exists(src)) Files.copy(src, trg, StandardCopyOption.REPLACE_EXISTING);
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
		if (eventBroker == null) return profile;
		
		if (entities.size() > 0) { eventBroker.post(IntelEvents.ENTITY_TYPE_NEW, entities); }
		if (relationshipTypes.size() > 0) { eventBroker.post(IntelEvents.RELATION_TYPE_NEW, relationshipTypes); }
		if (recordSources.size() > 0) { eventBroker.post(IntelEvents.RECORD_SOURCE_ALL, recordSources); }
		
		return profile;
		
	}
	
	
	private List<IntelRelationshipType> processRelationshipTypes(List<RelationshipType> xmlTypes, 
			HashMap<String, IntelAttribute> attributes, HashMap<String, IntelEntityType> entities, HashMap<String, IntelRelationshipGroup> groups,
			IntelProfile profile){
		
		List<IntelRelationshipType> newSources = new ArrayList<>();
		
		for (RelationshipType xmlSource : xmlTypes) {
			IntelRelationshipType src = new IntelRelationshipType();
			src.setConservationArea(ca);
			src.setIcon(xmlSource.getIcon());
			src.setKeyId(xmlSource.getKey());
			updateNames(src, xmlSource.getNames());
			
			String srcProfileKey = xmlSource.getSrcProfileKey();
			String targetProfileKey = xmlSource.getTargetProfileKey();
			
			IntelProfile srcProfile = null;
			if (profile.getKeyId().equalsIgnoreCase(srcProfileKey)) {
				srcProfile = profile;
			}else {
				srcProfile = session.createQuery("FROM IntelProfile WHERE conservationArea = :ca AND keyId = :keyid", IntelProfile.class) //$NON-NLS-1$
					.setParameter("ca", ca) //$NON-NLS-1$
					.setParameter("keyid", srcProfileKey).uniqueResult(); //$NON-NLS-1$
			}
			if (srcProfile == null) {
				warnings.add(MessageFormat.format(Messages.XmlToProfile_ProfileNotFound, srcProfileKey, xmlSource.getKey()));
				continue;
			}
			
			IntelProfile targetProfile = null;
			if (profile.getKeyId().equalsIgnoreCase(targetProfileKey)) {
				targetProfile = profile;
			}else {
				targetProfile = session.createQuery("FROM IntelProfile WHERE conservationArea = :ca AND keyId = :keyid", IntelProfile.class) //$NON-NLS-1$
					.setParameter("ca", ca) //$NON-NLS-1$
					.setParameter("keyid", targetProfileKey).uniqueResult(); //$NON-NLS-1$
			}
			if (targetProfile == null) {
				warnings.add(MessageFormat.format(Messages.XmlToProfile_ProfileNotFoundRelationships, targetProfileKey, xmlSource.getKey()));
				continue;
			}
				
			src.setSourceProfile(srcProfile);
			src.setTargetProfile(targetProfile);
			
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
				toAdd.add(found);
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
			}else {
				toAdd.add(found);
			}
		}
		return toAdd;
	}
	
	private List<IntelEntityType> proecessEntityTypes(List<EntityType> xmlTypes, HashMap<String, IntelAttribute> attributes) throws Exception{
		List<IntelEntityType> newSources = new ArrayList<>();
		
		for (EntityType xmlSource : xmlTypes) {
			IntelEntityType src = new IntelEntityType();
			src.setConservationArea(ca);
			src.setIcon(xmlSource.getIcon());
			src.setKeyId(xmlSource.getKey());
			src.setProfiles(new HashSet<>());
			updateNames(src, xmlSource.getNames());
			
			IntelAttribute idAttribute = attributes.get(xmlSource.getIdAttribute());
			if (idAttribute == null) {
				throw new Exception(MessageFormat.format(Messages.XmlToIntelData_EntityTypeIdAttributeNotFound, xmlSource.getIdAttribute(), src.getName(), src.getKeyId()));
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
				if (!areSame(newSource, found)) {
					throw new Exception(MessageFormat.format(
						Messages.XmlToProfile_EntityTypeExists,
						found.getName(), found.getKeyId()));
				}
				toAdd.add(found);
			}
		}
		return toAdd;
	}

	
	
	private List<IntelRecordSource> processRecordSources(List<RecordSource> xmlSources, HashMap<String, IntelAttribute> attributes, HashMap<String, IntelEntityType> entityTypes) throws Exception{
		List<IntelRecordSource> newSources = new ArrayList<>();
		
		for (RecordSource xmlSource : xmlSources) {
			IntelRecordSource src = new IntelRecordSource();
			src.setConservationArea(ca);
			src.setIcon(xmlSource.getIcon());
			src.setKeyId(xmlSource.getKey());
			src.setProfiles(new HashSet<>());
			updateNames(src, xmlSource.getNames());
			
			if (xmlSource.getAttributes() != null) {
				src.setAttributes(new ArrayList<>());
				for (RecordSourceAttribute xmlSrcAttribute : xmlSource.getAttributes()) {
					IntelRecordSourceAttribute attribute = new IntelRecordSourceAttribute();
					attribute.setKeyId(xmlSrcAttribute.getKey());
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
				if (!areSame(newSource, found)) {
					throw new Exception(MessageFormat.format(Messages.XmlToProfile_RecordSourceExists, found.getKeyId()));
				}
				toAdd.add(found);
			}
		}
		return toAdd;
	}
	
	/*
	 * Validates that the two sources are similar.  This means the keys are the same
	 * and all attributes in the input record source exist in the existing record 
	 * source and the multiplicy value is the same.  Does not validate the attributes or
	 * entity types for similarity.
	 */
	private boolean areSame(IntelRecordSource in, IntelRecordSource existing) {
		if (!in.getKeyId().equalsIgnoreCase(existing.getKeyId())) return false;
		
		//validate that every attribute in the input exists in the existing value
		//we don't create if the existing one has more
		for (IntelRecordSourceAttribute inatt : in.getAttributes()) {
			boolean found = false;
			for (IntelRecordSourceAttribute eatt : existing.getAttributes()) {
				if (inatt.getKeyId().equalsIgnoreCase(eatt.getKeyId())) {
					if (inatt.getAttribute() != null && eatt.getAttribute() != null && inatt.getAttribute().getKeyId().equalsIgnoreCase(eatt.getAttribute().getKeyId())) {
						if (eatt.getIsMultiple() != inatt.getIsMultiple()) {
							return false;
						}
						found = true;
						break;
					}
					if (inatt.getEntityType() != null && eatt.getEntityType() != null && inatt.getEntityType().getKeyId().equalsIgnoreCase(eatt.getEntityType().getKeyId())) {
						if (eatt.getIsMultiple() != inatt.getIsMultiple()) {
							return false;
						}
						found = true;
						break;
					}
					return false;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}
	
	
	/*
	 * Validates that the two sources are similar.  This means the keys are the same
	 * and all attributes in the input record source exist in the existing record 
	 * source and the multiplicy value is the same.  Does not validate the attributes or
	 * entity types for similarity.
	 */
	private boolean areSame(IntelEntityType in, IntelEntityType existing) {
		if (!in.getKeyId().equalsIgnoreCase(existing.getKeyId())) return false;
		
		//validate that every attribute in the input exists in the existing value
		//we don't create if the existing one has more
		
		if (!in.getIdAttribute().getKeyId().equalsIgnoreCase(existing.getIdAttribute().getKeyId())) return false;
		for (IntelEntityTypeAttribute inatt : in.getAttributes()) {
			boolean found = false;
			for (IntelEntityTypeAttribute eatt : existing.getAttributes()) {
				if (inatt.getAttribute().getKeyId().equalsIgnoreCase(eatt.getAttribute().getKeyId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}
	
	
	private List<IntelAttribute> processAttributes(List<Attribute> xmlAttributes) throws Exception {
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
					li.setOrder(xmlListItem.getListOrder());
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
					throw new Exception(MessageFormat.format(Messages.XmlToProfile_AttributeExists, found.getKeyId())); 
				}
				
				if (found.getType() == AttributeType.LIST) {
					//validate that everything in the import list is in the existing list
					for (IntelAttributeListItem li : newAttribute.getAttributeList()) {
						boolean lifound = false;
						for (IntelAttributeListItem li2 : found.getAttributeList()) {
							if (li.getKeyId().equalsIgnoreCase(li2.getKeyId())) {
								lifound = true;
								break;
							}
						}
						if (!lifound) {
							throw new Exception(MessageFormat.format(Messages.XmlToProfile_ListItemExists, newAttribute.getKeyId(), li.getKeyId()));
						}
					}
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
