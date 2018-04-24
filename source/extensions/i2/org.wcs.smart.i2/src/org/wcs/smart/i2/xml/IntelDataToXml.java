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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
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
import org.wcs.smart.i2.xml.model.AttributeType;
import org.wcs.smart.i2.xml.model.EntityType;
import org.wcs.smart.i2.xml.model.EntityTypeAttribute;
import org.wcs.smart.i2.xml.model.EntityTypeAttributeGroup;
import org.wcs.smart.i2.xml.model.IntelligenceData;
import org.wcs.smart.i2.xml.model.ObjectFactory;
import org.wcs.smart.i2.xml.model.RecordSource;
import org.wcs.smart.i2.xml.model.RecordSourceAttribute;
import org.wcs.smart.i2.xml.model.RelationshipGroup;
import org.wcs.smart.i2.xml.model.RelationshipType;
import org.wcs.smart.i2.xml.model.RelationshipTypeAttribute;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Converts various intelligence data components to xml file.
 * 
 * @author Emily
 *
 */
public class IntelDataToXml {

	public static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.i2.xml.model"; //$NON-NLS-1$
	
	public static final String XML_DATA_FILENAME = "intelligencedata.xml"; //$NON-NLS-1$
	
	private Session session;
	
	private IntelligenceData data;
	private Set<Path> filesToInclude;
	
	public IntelDataToXml(Session session) {
		this.session = session;
	}
	
	public void export(Path outputFile, List<UUID> attributes, List<UUID> sources, List<UUID> relationshipTypes, List<UUID> entityTypes, boolean exportRecordTemplate, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor,Messages.IntelDataToXml_ExportTaskName, 4);
		toXml(attributes, sources, relationshipTypes, entityTypes, exportRecordTemplate, progress.split(3));
		if (data  != null) {
			writeData(outputFile, progress.split(1));
		}else {
			throw new IOException(Messages.IntelDataToXml_ConvservationFailedMsg);
		}
		
	}
	
	private void writeData(Path outputFile, IProgressMonitor monitor) throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		
		monitor.subTask(Messages.IntelDataToXml_ExportTask);
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
	
	private void toXml(List<UUID> attributes, List<UUID> sources, List<UUID> relationshipTypes, List<UUID> entityTypes, boolean exportRecordTemplate, IProgressMonitor monitor) {
	
		SubMonitor progress = SubMonitor.convert(monitor, Messages.IntelDataToXml_ExportTask2, 7);
		
		Set<IntelRecordSource> isources = new HashSet<>();
		Set<IntelAttribute> iattributes = new HashSet<>();
		Set<IntelEntityType> ientitytypes = new HashSet<>();
		Set<IntelRelationshipType> irelationshiptypes = new HashSet<>();
		Set<IntelRelationshipGroup> igroups = new HashSet<>();
		filesToInclude = new HashSet<>();
		
		progress.subTask(Messages.IntelDataToXml_CollectionDataTask);
		progress.split(1);
		Path recordTemplate = null;
		if (exportRecordTemplate) {
			recordTemplate = IntelReportManager.INSTANCE.getRecordTemplate(SmartDB.getCurrentConservationArea());
			if (recordTemplate != null && Files.exists(recordTemplate)) {
				filesToInclude.add(recordTemplate);
			}
		}
		
		if (attributes != null) {
			attributes.forEach(a->{
				IntelAttribute ia = session.get(IntelAttribute.class, a);
				if (ia != null) iattributes.add(ia);
			});
		}
		
		
		if (sources != null) {
			sources.forEach(s->{
				IntelRecordSource src = session.get(IntelRecordSource.class, s);
				if (src != null) {
					isources.add(src);
					if (src.getAttributes() != null) {
						src.getAttributes().forEach(a->{
							if (a.getAttribute() != null) {
								iattributes.add(a.getAttribute());
							//}else if (a.getEntityType() != null) {
							//	ientitytypes.add(a.getEntityType());
							}
						});
					}
				}
			});
		}
		

		if (relationshipTypes != null) {
			relationshipTypes.forEach(r->{
				IntelRelationshipType rtype = session.get(IntelRelationshipType.class, r);
				if (rtype != null) {
					irelationshiptypes.add(rtype);
					/*
					if (rtype.getSourceEntityType() != null) ientitytypes.add(rtype.getSourceEntityType());
					if (rtype.getTargetEntityType() != null) ientitytypes.add(rtype.getTargetEntityType());
					*/
				}
			});
		}

		if (entityTypes != null) {
			entityTypes.forEach(e->{
				IntelEntityType etype = session.get(IntelEntityType.class, e);
				if (etype != null) {
					ientitytypes.add(etype);
				}
			});
		}
		
		//TODO include relationships for entities
		/*
		Collection<IntelEntityType> toSearch = ientitytypes;
		while(!toSearch.isEmpty()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<IntelRelationshipType> c = cb.createQuery(IntelRelationshipType.class);
			Root<IntelRelationshipType> from = c.from(IntelRelationshipType.class);
			c.where( cb.or(from.get("sourceEntityType").in(entityTypes), //$NON-NLS-1$
				 from.get("targetEntityType").in(entityTypes))); //$NON-NLS-1$
		
			List<IntelRelationshipType> rtypes = session.createQuery(c).list();
			toSearch = new ArrayList<>();
			for (IntelRelationshipType t : rtypes) {
				if (irelationshiptypes.contains(t)) continue;
				
				irelationshiptypes.add(t);
				if (t.getSourceEntityType() != null && !ientitytypes.contains(t.getSourceEntityType())) {
					ientitytypes.add(t.getSourceEntityType());
					toSearch.add(t.getSourceEntityType());
				}
				if (t.getTargetEntityType() != null && !ientitytypes.contains(t.getTargetEntityType())) {
					ientitytypes.add(t.getSourceEntityType());
					toSearch.add(t.getSourceEntityType());
				}
			}
		}
		*/
		
		irelationshiptypes.forEach(r->{
			if (r.getAttributes() != null) {
				r.getAttributes().forEach(ra -> iattributes.add(ra.getAttribute()));
			}
			if (r.getRelationshipGroup() != null) igroups.add(r.getRelationshipGroup());
		});
		ientitytypes.forEach(e->{
			if (e.getAttributes() != null) {
				e.getAttributes().forEach(ea -> iattributes.add(ea.getAttribute()));
			}
		});
		
		
		progress.subTask(Messages.IntelDataToXml_attributesTask);
		progress.split(1);
		List<Attribute> xmlAttributes = convertAttributes(iattributes);
		
		progress.subTask(Messages.IntelDataToXml_recordSourceTask);
		progress.split(1);
		List<RecordSource> xmlSources = convertRecordSource(isources);
		
		progress.subTask(Messages.IntelDataToXml_relationshipGroupTask);
		progress.split(1);
		List<RelationshipGroup> xmlGroups = convertRelationshipGroups(igroups);
		
		progress.subTask(Messages.IntelDataToXml_relationshipTypeTask);
		progress.split(1);
		List<RelationshipType> xmlRelationships = convertRelationshipType(irelationshiptypes);
		
		progress.subTask(Messages.IntelDataToXml_entityTypeTask);
		progress.split(1);
		List<EntityType> xmlEntities = convertEntityType(ientitytypes, filesToInclude);
		
		progress.subTask(Messages.IntelDataToXml_conversionTask);
		data = new IntelligenceData();
		data.getAttributes().addAll(xmlAttributes);
		data.getEntities().addAll(xmlEntities);
		data.getRecordSource().addAll(xmlSources);
		data.getRelationshipGroups().addAll(xmlGroups);
		data.getRelationships().addAll(xmlRelationships);
		
		if (recordTemplate != null) {
			data.setRecordTemplate(recordTemplate.getFileName().toString());
		}
		progress.worked(1);
	}
	
	private void writeToFile(IntelligenceData data, Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<IntelligenceData> element = objFactor.createData(data);
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
	
	private List<RecordSource> convertRecordSource(Set<IntelRecordSource> isources){
		List<RecordSource> xmlSources = new ArrayList<>();
		for (IntelRecordSource a : isources) {
			RecordSource xmlSource = new RecordSource();
			
			xmlSource.setKey(a.getKeyId());
			xmlSource.getNames().addAll(convertNamedItem(a));
			xmlSource.setIcon(a.getIcon());
			
			if (a.getAttributes() != null) {
				for (IntelRecordSourceAttribute srcAttribute : a.getAttributes()) {
					RecordSourceAttribute xmlAttribute = new RecordSourceAttribute();
					
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
	
	private List<EntityType> convertEntityType(Set<IntelEntityType> etypes, Collection<Path> filesToInclude){
		List<EntityType> xmlTypes = new ArrayList<>();
		for (IntelEntityType e : etypes) {
			
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
