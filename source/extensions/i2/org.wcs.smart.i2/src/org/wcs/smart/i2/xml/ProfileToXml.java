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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
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
import org.wcs.smart.i2.xml.model.AttributeType;
import org.wcs.smart.i2.xml.model.EntityType;
import org.wcs.smart.i2.xml.model.EntityTypeAttribute;
import org.wcs.smart.i2.xml.model.EntityTypeAttributeGroup;
import org.wcs.smart.i2.xml.model.ObjectFactory;
import org.wcs.smart.i2.xml.model.Profile;
import org.wcs.smart.i2.xml.model.RecordSource;
import org.wcs.smart.i2.xml.model.RecordSourceAttribute;
import org.wcs.smart.i2.xml.model.RelationshipGroup;
import org.wcs.smart.i2.xml.model.RelationshipType;
import org.wcs.smart.i2.xml.model.RelationshipTypeAttribute;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Converts various intelligence data components to xml file.
 * 
 * @author Emily
 *
 */
public class ProfileToXml {

	public static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.i2.xml.model"; //$NON-NLS-1$
	
	public static final String XML_DATA_FILENAME = "intelligencedata.xml"; //$NON-NLS-1$
	
	private Session session;
		
	public ProfileToXml(Session session) {
		this.session = session;
	}
	
	public void export(Path outputFile, IntelProfile profile, IProgressMonitor monitor) throws Exception{
		
		SubMonitor progress = SubMonitor.convert(monitor,Messages.IntelDataToXml_ExportTaskName, 2);
		Path tempDir = Files.createTempDirectory("smart." + System.nanoTime()); //$NON-NLS-1$
		
		try {
			IntelProfile p = session.get(IntelProfile.class, profile.getUuid());
			toXml(p, tempDir, progress.split(1));
			
			progress.subTask(Messages.ProfileToXml_zippingTaskName);
			List<Path> toInclude = Files.list(tempDir).collect(Collectors.toList());
			ZipUtil.createZip(toInclude, outputFile, progress.split(1));
		}finally {
			//clean up
			SmartUtils.deleteDirectory(tempDir);
		}
	}
		
	private void toXml(IntelProfile profile, Path outputDir, IProgressMonitor monitor) throws Exception {
	
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelDataToXml_ExportTask2, 8);
		
		Set<IntelAttribute> iattributes = new HashSet<>();
		Set<IntelRelationshipType> irelationshiptypes = new HashSet<>();
		Set<IntelRelationshipGroup> igroups = new HashSet<>();
		Set<Path> filesToInclude = new HashSet<>();
		
		
		progress.subTask(Messages.IntelDataToXml_CollectionDataTask);
		progress.split(1);
		Path recordTemplate = null;
		
		recordTemplate = IntelReportManager.INSTANCE.getRecordTemplate(SmartDB.getCurrentConservationArea());
		if (recordTemplate != null && Files.exists(recordTemplate)) {
			filesToInclude.add(recordTemplate);
		}
		
		for (IntelProfileEntityType t : profile.getEntityTypes()) {
			for (IntelEntityTypeAttribute eta : t.getEntityType().getAttributes()) {
				if (eta.getAttribute() != null) iattributes.add(eta.getAttribute());
			}
		}
		for (IntelProfileRecordSource s : profile.getRecordSources()) {
			for (IntelRecordSourceAttribute a : s.getRecordSource().getAttributes()) {
				if (a.getAttribute() != null) iattributes.add(a.getAttribute());
			}
		}
		
		irelationshiptypes.addAll(session.createQuery("FROM IntelRelationshipType WHERE sourceProfile = :src or targetProfile = :trg", IntelRelationshipType.class) //$NON-NLS-1$
				.setParameter("src", profile) //$NON-NLS-1$
				.setParameter("trg", profile) //$NON-NLS-1$
				.list());
		
		irelationshiptypes.forEach(ir->{
			if (ir.getRelationshipGroup() != null) igroups.add(ir.getRelationshipGroup());
			ir.getAttributes().forEach(a->iattributes.add(a.getAttribute()));
		});
		
		progress.subTask(Messages.IntelDataToXml_attributesTask);
		progress.split(1);
		List<Attribute> xmlAttributes = convertAttributes(iattributes);
		
		progress.subTask(Messages.IntelDataToXml_recordSourceTask);
		progress.split(1);
		List<RecordSource> xmlSources = convertRecordSource(profile.getRecordSources());
		
		progress.subTask(Messages.IntelDataToXml_relationshipGroupTask);
		progress.split(1);
		List<RelationshipGroup> xmlGroups = convertRelationshipGroups(igroups);
		
		progress.subTask(Messages.IntelDataToXml_relationshipTypeTask);
		progress.split(1);
		List<RelationshipType> xmlRelationships = convertRelationshipType(irelationshiptypes);
		
		progress.subTask(Messages.IntelDataToXml_entityTypeTask);
		progress.split(1);
		List<EntityType> xmlEntities = convertEntityType(profile.getEntityTypes(), filesToInclude);
		
		progress.subTask(Messages.IntelDataToXml_conversionTask);
		Profile data = new Profile();
		data.setKey(profile.getKeyId());
		data.setColor(Integer.toHexString(profile.getColor()).substring(2));
		data.getNames().addAll(convertNamedItem(profile));

		
		data.getAttributes().addAll(xmlAttributes);
		data.getEntities().addAll(xmlEntities);
		data.getRecordSource().addAll(xmlSources);
		data.getRelationshipGroups().addAll(xmlGroups);
		data.getRelationships().addAll(xmlRelationships);
		
		if (recordTemplate != null) {
			data.setRecordTemplate(recordTemplate.getFileName().toString());
		}
		progress.worked(1);
		
		
		SubMonitor mfiles = progress.newChild(filesToInclude.size() + 1);
		Path xmlFile = outputDir.resolve(profile.getKeyId() +".xml"); //$NON-NLS-1$
		writeToFile(data, xmlFile);
		mfiles.worked(1);
		if (!filesToInclude.isEmpty()) {
			Path fileDir = outputDir.resolve(profile.getKeyId());
			Files.createDirectory(fileDir);
			
			for (Path src : filesToInclude) {
				Path trg = fileDir.resolve(src.getFileName());
				Files.copy(src, trg);
				mfiles.worked(1);
			}
		}
		

	}
	
	private void writeToFile(Profile data, Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<Profile> element = objFactor.createData(data);
		marshaller.marshal(element, xmlFile.toFile());
	}
	
	private List<Attribute> convertAttributes(Set<IntelAttribute> iattributes){
		List<Attribute> xmlAttributes = new ArrayList<>();
		for (IntelAttribute a : iattributes) {
			Attribute xmlAttribute = new Attribute();
			xmlAttribute.setKey(a.getKeyId());
			xmlAttribute.setType(AttributeType.valueOf(a.getType().name()));
			xmlAttribute.getNames().addAll(convertNamedItem(a));
			
			if (a.getType() == IntelAttribute.AttributeType.LIST && a.getAttributeList() != null) {
				for (IntelAttributeListItem li : a.getAttributeList()) {
					AttributeListItem xmlListItem = new AttributeListItem();
					xmlListItem.setKey(li.getKeyId());
					xmlListItem.setListOrder(li.getOrder());
					xmlListItem.getNames().addAll(convertNamedItem(li));
					xmlAttribute.getListValues().add(xmlListItem);
				}
			}	
			xmlAttributes.add(xmlAttribute);
		}
		return xmlAttributes;
	}
	
	private List<RecordSource> convertRecordSource(Set<IntelProfileRecordSource> isources){
		List<RecordSource> xmlSources = new ArrayList<>();
		for (IntelProfileRecordSource as : isources) {
			IntelRecordSource a = as.getRecordSource();
			
			RecordSource xmlSource = new RecordSource();
			
			xmlSource.setKey(a.getKeyId());
			xmlSource.getNames().addAll(convertNamedItem(a));
			xmlSource.setIcon(a.getIcon());
			
			if (a.getAttributes() != null) {
				for (IntelRecordSourceAttribute srcAttribute : a.getAttributes()) {
					RecordSourceAttribute xmlAttribute = new RecordSourceAttribute();
					xmlAttribute.setIsDuplicateCheck(srcAttribute.getDuplicateCheck());
					xmlAttribute.setKey(srcAttribute.getKeyId());
					xmlAttribute.setIsMulit(srcAttribute.getIsMultiple());
					xmlAttribute.setOrder(srcAttribute.getOrder());
					xmlAttribute.getNames().addAll(convertNamedItem(srcAttribute));
					if (srcAttribute.getAttribute() != null) {
						xmlAttribute.setAttributeKey(srcAttribute.getAttribute().getKeyId());	
					}
					if (srcAttribute.getEntityType() != null) {
						xmlAttribute.setEntityTypeKey(srcAttribute.getEntityType().getKeyId());
					}
					
					xmlSource.getAttributes().add(xmlAttribute);
				}
			}	
			xmlSources.add(xmlSource);
		}
		return xmlSources;
	}
	
	
	private List<RelationshipGroup> convertRelationshipGroups(Set<IntelRelationshipGroup> igroups){
		List<RelationshipGroup> xmlGroups = new ArrayList<>();
		for (IntelRelationshipGroup g : igroups) {
			RelationshipGroup xmlGroup = new RelationshipGroup();
			xmlGroup.setKey(g.getKeyId());
			xmlGroup.getNames().addAll(convertNamedItem(g));
			xmlGroups.add(xmlGroup);
		}
		return xmlGroups;
	}
	
	private List<RelationshipType> convertRelationshipType(Set<IntelRelationshipType> rtypes){
		List<RelationshipType> xmlTypes = new ArrayList<>();
		for (IntelRelationshipType r : rtypes) {
			RelationshipType xmlType = new RelationshipType();
			
			xmlType.setKey(r.getKeyId());
			xmlType.getNames().addAll(convertNamedItem(r));
			xmlType.setIcon(r.getIcon());
			
			xmlType.setSrcProfileKey(r.getSourceProfile().getKeyId());
			xmlType.setTargetProfileKey(r.getTargetProfile().getKeyId());
			
			if (r.getRelationshipGroup() != null) xmlType.setGroupKey(r.getRelationshipGroup().getKeyId());
			if (r.getSourceEntityType() != null) xmlType.setSrcTypeKey(r.getSourceEntityType().getKeyId());
			if (r.getTargetEntityType() != null) xmlType.setTargetTypeKey(r.getTargetEntityType().getKeyId());
			
			if (r.getAttributes() != null) {
				for (IntelRelationshipTypeAttribute a : r.getAttributes()) {
					RelationshipTypeAttribute xmlAttribute = new RelationshipTypeAttribute();
					xmlAttribute.setAttributeKey(a.getAttribute().getKeyId());
					xmlAttribute.setOrder(a.getOrder());
					
					xmlType.getAttributes().add(xmlAttribute);
				}
			}
			xmlTypes.add(xmlType);
		}
		return xmlTypes;
	}
	
	private List<EntityType> convertEntityType(Set<IntelProfileEntityType> etypes, Collection<Path> filesToInclude){
		List<EntityType> xmlTypes = new ArrayList<>();
		for (IntelProfileEntityType et : etypes) {
			
			IntelEntityType e = et.getEntityType();
			
			EntityType xmlType = new EntityType();
			
			xmlType.setKey(e.getKeyId());
			xmlType.getNames().addAll(convertNamedItem(e));
			xmlType.setIcon(e.getIcon());
			xmlType.setIdAttribute(e.getIdAttribute().getKeyId());
			
			if (e.getBirtTemplate() != null) {
				Path birtreport = IntelReportManager.INSTANCE.getEntityTemplate(e);
				if (Files.exists(birtreport)) {
					xmlType.setReportTemplate(e.getBirtTemplate());
					filesToInclude.add(birtreport);
				}
			}
			
			if (e.getAttributes() != null) {
				Set<IntelEntityTypeAttributeGroup> groups = new HashSet<>();
				for (IntelEntityTypeAttribute a : e.getAttributes()) {
					if (a.getAttributeGroup() != null) groups.add(a.getAttributeGroup());
					
					EntityTypeAttribute xmlAttribute = new EntityTypeAttribute();
					xmlAttribute.setIsDuplicateCheck(a.getDuplicateCheck());
					xmlAttribute.setAttributeKey(a.getAttribute().getKeyId());
					xmlAttribute.setOrder(a.getOrder());
					if (a.getAttributeGroup() != null) xmlAttribute.setGroupKey(UuidUtils.uuidToString(a.getAttributeGroup().getUuid()));
					xmlType.getAttributes().add(xmlAttribute);
				}
				
				for(IntelEntityTypeAttributeGroup g : groups) {
					EntityTypeAttributeGroup xmlGroup = new EntityTypeAttributeGroup();
					xmlGroup.getNames().addAll(convertNamedItem(g));
					xmlGroup.setOrder(g.getOrder());				
					xmlGroup.setId(UuidUtils.uuidToString(g.getUuid()));
					xmlType.getGroups().add(xmlGroup);
				}
				
			}
			
			xmlTypes.add(xmlType);
		}
		return xmlTypes;
	}
	
	private Set<org.wcs.smart.i2.xml.model.NamedItem> convertNamedItem(NamedItem item) {
		Set<org.wcs.smart.i2.xml.model.NamedItem> items = new HashSet<>();
		for (Label l : item.getNames()) {
			org.wcs.smart.i2.xml.model.NamedItem xmlItem = new org.wcs.smart.i2.xml.model.NamedItem();
			xmlItem.setIsDefault(l.getLanguage().isDefault());
			xmlItem.setValue(l.getValue());
			xmlItem.setLanguageCode(l.getLanguage().getCode());
			items.add(xmlItem);
		}
		return items;
	}
}
