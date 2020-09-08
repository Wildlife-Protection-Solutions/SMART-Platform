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
package org.wcs.smart.asset.data.inout;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.asset.data.inout.xml.AssetData;
import org.wcs.smart.asset.data.inout.xml.Attribute;
import org.wcs.smart.asset.data.inout.xml.AttributeListItem;
import org.wcs.smart.asset.data.inout.xml.AttributeMapping;
import org.wcs.smart.asset.data.inout.xml.AttributeType;
import org.wcs.smart.asset.data.inout.xml.MetadataMapping;
import org.wcs.smart.asset.data.inout.xml.ObjectFactory;
import org.wcs.smart.asset.data.inout.xml.StationAttributes;
import org.wcs.smart.asset.data.inout.xml.StationLocationAttributes;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Converts asset model data to xml file
 * 
 * @author Emily
 *
 */
public class AssetModelDataToXml {

	/**
	 * Converts the provided asset configurations to xml file.
	 * 
	 * @param outputFile
	 * @param attributes
	 * @param assetTypes
	 * @param metadataMappings
	 * @param includeModuleSettings
	 * @param includeStationAttributes
	 * @param includeLocationAttributes
	 * @throws JAXBException
	 */
	public void export(Path outputFile, List<UUID> attributes, List<UUID> assetTypes, List<UUID> metadataMappings, 
			boolean includeModuleSettings, boolean includeStationAttributes, boolean includeLocationAttributes, IProgressMonitor monitor) throws JAXBException {
		
		SubMonitor progress = SubMonitor.convert(monitor, 7);
		
		AssetData xmlData = new AssetData();
		
		try(Session session = HibernateManager.openSession()){
			progress.split(1);
			if (attributes != null && !attributes.isEmpty()) {
				exportAttributes(attributes, session, xmlData);
			}
			progress.split(1);
			if (assetTypes != null && !assetTypes.isEmpty()) {
				exportAssetTypes(assetTypes, session, xmlData);
			}
			progress.split(1);
			if (metadataMappings != null && !metadataMappings.isEmpty()) {
				exportMetadataMappings(metadataMappings, session, xmlData);
			}
			progress.split(1);
			if (includeModuleSettings) {
				exportModuleSettings(xmlData, session);
			}
			progress.split(1);
			if (includeStationAttributes) {
				exportStationAttributes(xmlData, session);
			}
			progress.split(1);
			if (includeLocationAttributes) {
				exportStationLocationAttributes(xmlData, session);
			}
		}
		
		//write xmlData to file
		progress.split(1);
		writeToFile(xmlData, outputFile);
		progress.done();
	}
	
	private void writeToFile(AssetData data, Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<AssetData> element = objFactor.createData(data);
		marshaller.marshal(element, xmlFile.toFile());
	}
	
	private void exportStationAttributes(AssetData xmlData, Session session) {
		List<AssetStationAttribute> attributes = QueryFactory.buildQuery(session, AssetStationAttribute.class, new Object[] {"attribute.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		
		if (attributes.isEmpty()) return;
		
		StationAttributes xmlAttributes = new StationAttributes();
		xmlData.setStationAttributes(xmlAttributes);
		
		for (AssetStationAttribute attribute : attributes) {
			AttributeMapping xmlMapping = new AttributeMapping();
			xmlMapping.setAttributeKey(attribute.getAttribute().getKeyId());
			xmlMapping.setAttributeType(attribute.getAttribute().getType().name());
			xmlMapping.setOrder(attribute.getOrder());
			
			xmlAttributes.getAttributes().add(xmlMapping);
		}
	}
	
	private void exportStationLocationAttributes(AssetData xmlData, Session session) {
		List<AssetStationLocationAttribute> attributes = QueryFactory.buildQuery(session, AssetStationLocationAttribute.class, new Object[] {"attribute.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		
		if (attributes.isEmpty()) return;
		
		StationLocationAttributes xmlAttributes = new StationLocationAttributes();
		xmlData.setStationLocationAttributes(xmlAttributes);
		
		for (AssetStationLocationAttribute attribute : attributes) {
			AttributeMapping xmlMapping = new AttributeMapping();
			xmlMapping.setAttributeKey(attribute.getAttribute().getKeyId());
			xmlMapping.setAttributeType(attribute.getAttribute().getType().name());
			xmlMapping.setOrder(attribute.getOrder());
			
			xmlAttributes.getAttributes().add(xmlMapping);
		}
	}
	
	private void exportModuleSettings(AssetData xmlData, Session session) {
		List<AssetModuleSettings> settings = QueryFactory.buildQuery(session, AssetModuleSettings.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		
		for (AssetModuleSettings setting : settings) {
			org.wcs.smart.asset.data.inout.xml.AssetModuleSettings xmlSetting = new org.wcs.smart.asset.data.inout.xml.AssetModuleSettings();
			xmlSetting.setKey(setting.getKeyId());
			xmlSetting.setValue(setting.getValue());
			
			xmlData.getModuleSettings().add(xmlSetting);
		}
	}
	
	private void exportMetadataMappings(List<UUID> metadataMappings, Session session, AssetData xmlData) {
		for (UUID mappingUuid : metadataMappings) {
			
			AssetMetadataMapping mapping = session.get(AssetMetadataMapping.class, mappingUuid);
			
			MetadataMapping xmlMapping = new MetadataMapping();
			
			if (mapping.getMappedAssetProperty() != null) xmlMapping.setAssetProperty(mapping.getMappedAssetProperty().name());
			if (mapping.getMappedAttribute() != null) xmlMapping.setAttributeKey(mapping.getMappedAttribute().getKeyId());
			if (mapping.getMappedListItem() != null) xmlMapping.setAttributeListItemKey(mapping.getMappedListItem().getKeyId());
			if (mapping.getMappedTreeNode() != null) xmlMapping.setAttributeTreeNodeKey(mapping.getMappedTreeNode().getHkey());
			if (mapping.getMappedCategory() != null) xmlMapping.setCategoryKey(mapping.getMappedCategory().getHkey());
			xmlMapping.setMappingString(mapping.getMetadataKey());
			xmlMapping.setOrder(mapping.getSearchOrder());
			xmlMapping.setState(mapping.getState().name());
			xmlMapping.setType(mapping.getMetadataType().name());
			
			xmlData.getMetadataMappings().add(xmlMapping);
		}		
	}
	private void exportAssetTypes(List<UUID> assetTypes, Session session, AssetData xmlData) {
		
		for (UUID assetTypeUuid : assetTypes) {
			AssetType assetType = session.get(AssetType.class, assetTypeUuid);
			
			org.wcs.smart.asset.data.inout.xml.AssetType xmlAssetType = new org.wcs.smart.asset.data.inout.xml.AssetType();
			xmlAssetType.setKey(assetType.getKeyId());
			xmlAssetType.setIncidentCutoff(assetType.getIncidentCutoff());
			xmlAssetType.setIcon(assetType.getIcon());
			
			xmlAssetType.getNames().addAll(convertNamedItem(assetType));
			
			for (AssetTypeAttribute assetAttribute : assetType.getAssetAttributes()) {
				
				AttributeMapping attributeMapping = new AttributeMapping();
				attributeMapping.setAttributeKey(assetAttribute.getAttribute().getKeyId());
				attributeMapping.setAttributeType(assetAttribute.getAttribute().getType().name());
				attributeMapping.setOrder(assetAttribute.getOrder());
				
				xmlAssetType.getAttributes().add(attributeMapping);
			}
			
			
			for (AssetTypeDeploymentAttribute assetAttribute : assetType.getAssetDeploymentAttributes()) {
				
				AttributeMapping attributeMapping = new AttributeMapping();
				attributeMapping.setAttributeKey(assetAttribute.getAttribute().getKeyId());
				attributeMapping.setAttributeType(assetAttribute.getAttribute().getType().name());
				attributeMapping.setOrder(assetAttribute.getOrder());
				
				xmlAssetType.getDeploymentAttributes().add(attributeMapping);
			}
			
			xmlData.getAssetTypes().add(xmlAssetType);
		}		
	}

	private void exportAttributes(List<UUID> attributes, Session session, AssetData xmlData) {
		
		for (UUID attributeUuid : attributes) {
			AssetAttribute attribute = session.get(AssetAttribute.class, attributeUuid);
			
			Attribute xmlAttribute = new Attribute();
			xmlAttribute.setKey(attribute.getKeyId());
			xmlAttribute.setType(AttributeType.valueOf(attribute.getType().name()));
			xmlAttribute.getNames().addAll(convertNamedItem(attribute));
			
			if (attribute.getType() == org.wcs.smart.asset.model.AssetAttribute.AttributeType.LIST) {
				for (AssetAttributeListItem listItem : attribute.getAttributeList()) {
					AttributeListItem xmlListItem = new AttributeListItem();
					xmlListItem.setKey(listItem.getKeyId());
					xmlListItem.getNames().addAll(convertNamedItem(listItem));
					xmlAttribute.getListValues().add(xmlListItem);
				}
			}
			xmlData.getAttributes().add(xmlAttribute);
		}		
	}
	
	
	private Set<org.wcs.smart.asset.data.inout.xml.NamedItem> convertNamedItem(NamedItem item) {
		Set<org.wcs.smart.asset.data.inout.xml.NamedItem> items = new HashSet<>();
		for (Label l : item.getNames()) {
			org.wcs.smart.asset.data.inout.xml.NamedItem xmlItem = new org.wcs.smart.asset.data.inout.xml.NamedItem();
			xmlItem.setIsDefault(l.getLanguage().isDefault());
			xmlItem.setValue(l.getValue());
			xmlItem.setLanguageCode(l.getLanguage().getCode());
			items.add(xmlItem);
		}
		return items;
	}
}
