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

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.data.inout.xml.AssetData;
import org.wcs.smart.asset.data.inout.xml.Attribute;
import org.wcs.smart.asset.data.inout.xml.AttributeListItem;
import org.wcs.smart.asset.data.inout.xml.AttributeMapping;
import org.wcs.smart.asset.data.inout.xml.MetadataMapping;
import org.wcs.smart.asset.data.inout.xml.NamedItem;
import org.wcs.smart.asset.data.inout.xml.ObjectFactory;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetMetadataMapping.State;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;



/**
 * Converts xml data to asset model elements
 * 
 * @author Emily
 *
 */
public class AssetXmlToAssetData {

	private ConservationArea ca;
	private List<String> warnings;
	private Session session;
	
	public AssetXmlToAssetData(ConservationArea ca) {
		this.ca = ca;
	}
	
	public void importXmlData(Path xmlFile, IProgressMonitor monitor) throws IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.AssetXmlToAssetData_ImportTaskName, 10);
		warnings = new ArrayList<>();
		
		AssetData data = null;
		try {
			progress.split(1);
			progress.subTask(Messages.AssetXmlToAssetData_ReadingSubTaskName);
			data = readXmlFile(xmlFile);
		}catch (Exception ex) {
			throw new IOException(ex);
		}
			
		try (Session session = HibernateManager.openSession()){
			toAssetData(data, session, progress.split(9));
		}
		
	}
	
	private AssetData readXmlFile(Path xmlFile) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Unmarshaller unmarshaller = context.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<AssetData> o = (JAXBElement<AssetData>) unmarshaller.unmarshal(xmlFile.toFile());
		return o.getValue();
	}
	
	private void toAssetData(AssetData data, Session session, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 7);
		
		this.session = session;

		//process attributes
		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_ImportAttributesSubTask);
		List<AssetAttribute> attributes = processAttributes(data.getAttributes());
		
		//attribute mappings include newly imported and existing
		HashMap<String, AssetAttribute> attributeMapping = new HashMap<>();
		List<AssetAttribute> existingAttributes = QueryFactory.buildQuery(session, AssetAttribute.class, "conservationArea", ca).list(); //$NON-NLS-1$
		existingAttributes.forEach(e->attributeMapping.put(e.getKeyId(), e));
		attributes.forEach(e->attributeMapping.put(e.getKeyId(), e));
		
		//process asset types
		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_ImporttypesSubTask);
		List<AssetType> assetTypes = processAssetTypes(data, attributeMapping);

		//process metadata mappings
		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_ImportMappingsSubTask);
		List<AssetMetadataMapping> metadataMappings = processMetadataMappings(data, session);
		
		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_ImportSettingsSubTask);
		List<AssetModuleSettings> settings = processModuleSettings(data, session);
		
		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_ImportStationAttributeSubTask);
		List<AssetStationAttribute> stations = processStationAttributes(data, attributeMapping, session);

		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_ImportStationLocationAttributeSubTask);
		List<AssetStationLocationAttribute> locations = processStationLocationAttributes(data, attributeMapping, session);

		//validate warnings with user
		if (!warnings.isEmpty()) {
			boolean[] ret = new boolean[] {false};
			Display.getDefault().syncExec(()->{
				WarningDialog warningDialog = new WarningDialog(Display.getDefault().getActiveShell(), Messages.AssetXmlToAssetData_WarningsTitle, Messages.AssetXmlToAssetData_WarningsMessage, warnings, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
				if (warningDialog.open() == 0) {
					ret[0] = true;
				}
			});	
			//cancel
			if (!ret[0] ) return;
		}
		
		//save changes
		progress.split(1);
		progress.subTask(Messages.AssetXmlToAssetData_SavingTask);
		session.beginTransaction();
		try {
			attributes.forEach(a->session.saveOrUpdate(a));
			assetTypes.forEach(a->session.saveOrUpdate(a));
			metadataMappings.forEach(a->session.saveOrUpdate(a));
			
			settings.forEach(a->session.saveOrUpdate(a));
			stations.forEach(a->session.saveOrUpdate(a));
			locations.forEach(a->session.saveOrUpdate(a));
			
			session.getTransaction().commit();
		}catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		
		progress.done();
		
		StringBuilder sb = new StringBuilder();
		if (attributes.size() > 0) {
			sb.append(MessageFormat.format(Messages.AssetXmlToAssetData_numattributes, attributes.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (assetTypes.size() > 0) {
			sb.append(MessageFormat.format(Messages.AssetXmlToAssetData_numtypes, assetTypes.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (metadataMappings.size() > 0) {
			sb.append(MessageFormat.format(Messages.AssetXmlToAssetData_nummappings, metadataMappings.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (settings.size() > 0) {
			sb.append(MessageFormat.format(Messages.AssetXmlToAssetData_numsettings, settings.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (stations.size() > 0) {
			sb.append(MessageFormat.format(Messages.AssetXmlToAssetData_numstationattributes, stations.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		if (locations.size() > 0) {
			sb.append(MessageFormat.format(Messages.AssetXmlToAssetData_numlocationattribute, locations.size()));
			sb.append("\n"); //$NON-NLS-1$
		}
		
		if (sb.length() == 0) {
			sb.append(Messages.AssetXmlToAssetData_NoData);
		}else {
			sb.insert(0, Messages.AssetXmlToAssetData_ImportedMsg);
		}
		
		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.AssetXmlToAssetData_ImportedTitls, sb.toString());
		});

	}

	private List<AssetStationLocationAttribute> processStationLocationAttributes(AssetData xmlData, HashMap<String, AssetAttribute> attributeMappings, Session session){
		List<AssetStationLocationAttribute> stationAttributes = new ArrayList<>();
		
		if (xmlData.getStationLocationAttributes() != null && xmlData.getStationLocationAttributes().getAttributes() != null) {
			for (AttributeMapping xmlMapping : xmlData.getStationLocationAttributes().getAttributes()) {
				AssetStationLocationAttribute newAttribute = new AssetStationLocationAttribute();
				newAttribute.setOrder(xmlMapping.getOrder());
				
				AssetAttribute assetAttribute = attributeMappings.get(xmlMapping.getAttributeKey().toLowerCase());
				if (assetAttribute == null) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AttributeNotFound, xmlMapping.getAttributeKey()));
					continue;
				}else if (!assetAttribute.getType().name().equalsIgnoreCase(xmlMapping.getAttributeType())) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AssettypeDoesNotMatch, xmlMapping.getAttributeType(), assetAttribute.getType().name(), assetAttribute.getName()));
					continue;
				}
				newAttribute.setAttribute(assetAttribute);
				stationAttributes.add(newAttribute);
			}
		}

		//validate
		List<AssetStationLocationAttribute> toAdd = new ArrayList<>();
		List<AssetStationLocationAttribute> existingStationAttributes = QueryFactory.buildQuery(session, AssetStationLocationAttribute.class, "attribute.conservationArea", ca).list(); //$NON-NLS-1$
		for (AssetStationLocationAttribute newAttribute : stationAttributes) {
			AssetStationLocationAttribute found = null;
			for (AssetStationLocationAttribute existingStationAttribute: existingStationAttributes) {
				if (existingStationAttribute.getAttribute().equals(newAttribute.getAttribute())) {
					found = existingStationAttribute; 
					break;
				}
			}
			if (found == null) {
				toAdd.add(newAttribute);
			}else {
				warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AttributeAlreadyExists, found.getAttribute().getName())); 
			}
		}
		return toAdd;
		
	}
	
	private List<AssetStationAttribute> processStationAttributes(AssetData xmlData, HashMap<String, AssetAttribute> attributeMappings, Session session){
		List<AssetStationAttribute> stationAttributes = new ArrayList<>();
		
		if (xmlData.getStationAttributes() != null  && xmlData.getStationAttributes().getAttributes() != null) {
			for (AttributeMapping xmlMapping : xmlData.getStationAttributes().getAttributes()) {
				AssetStationAttribute newAttribute = new AssetStationAttribute();
				newAttribute.setOrder(xmlMapping.getOrder());
				
				AssetAttribute assetAttribute = attributeMappings.get(xmlMapping.getAttributeKey().toLowerCase());
				if (assetAttribute == null) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AttributeNotFoundStation, xmlMapping.getAttributeKey()));
					continue;
				}else if (!assetAttribute.getType().name().equalsIgnoreCase(xmlMapping.getAttributeType())) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_TypeDoesNotMatchStation, xmlMapping.getAttributeType(), assetAttribute.getType().name(), assetAttribute.getName()));
					continue;
				}
				newAttribute.setAttribute(assetAttribute);
				stationAttributes.add(newAttribute);
			}
		}

		//validate
		List<AssetStationAttribute> toAdd = new ArrayList<>();
		List<AssetStationAttribute> existingStationAttributes = QueryFactory.buildQuery(session, AssetStationAttribute.class, "attribute.conservationArea", ca).list(); //$NON-NLS-1$
		for (AssetStationAttribute newAttribute : stationAttributes) {
			AssetStationAttribute found = null;
			for (AssetStationAttribute existingStationAttribute: existingStationAttributes) {
				if (existingStationAttribute.getAttribute().equals(newAttribute.getAttribute())) {
					found = existingStationAttribute; 
					break;
				}
			}
			if (found == null) {
				toAdd.add(newAttribute);
			}else {
				warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AttributeExistsStation, found.getAttribute().getName())); 
			}
		}
		return toAdd;
		
	}
	
	private List<AssetModuleSettings> processModuleSettings(AssetData xmlData, Session session) {
		List<AssetModuleSettings> newSettings = new ArrayList<>();
		for (org.wcs.smart.asset.data.inout.xml.AssetModuleSettings xmlSetting : xmlData.getModuleSettings()) {
			AssetModuleSettings newSetting = new AssetModuleSettings();
			newSetting.setConservationArea(ca);
			newSetting.setKeyId(xmlSetting.getKey().toLowerCase());
			newSetting.setValue(xmlSetting.getValue());
			newSettings.add(newSetting);
		}
		
		//validate 
		List<AssetModuleSettings> toAdd = new ArrayList<>();
		List<AssetModuleSettings> existingSettings = QueryFactory.buildQuery(session, AssetModuleSettings.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (AssetModuleSettings newSetting : newSettings) {
			AssetModuleSettings found = null;
			for (AssetModuleSettings existingSetting: existingSettings) {
				if (existingSetting.getKeyId().equals(newSetting.getKeyId())) {
					found = existingSetting; 
					break;
				}
			}
			if (found == null) {
				toAdd.add(newSetting);
			}else {
				warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_SettingsExist, found.getKeyId())); 
			}
		}
		return toAdd;
		
	}
	
	
	private List<AssetMetadataMapping> processMetadataMappings(AssetData xmlData, Session session){
		List<AssetMetadataMapping> newMappings = new ArrayList<>();
		for (MetadataMapping xmlMapping : xmlData.getMetadataMappings()) {
			
			AssetMetadataMapping newMapping = new AssetMetadataMapping();
			newMapping.setConservationArea(ca);
			newMapping.setMetadataKey(xmlMapping.getMappingString());
			newMapping.setSearchOrder(xmlMapping.getOrder());
			
			newMapping.setState(State.ENABLED);
			try {
				newMapping.setState(State.valueOf(xmlMapping.getState()));
			}catch (Exception ex) {
				//eat this
			}
			
			if (xmlMapping.getType() != null && !xmlMapping.getType().isEmpty()) {
				try {
					AssetMetadataMapping.MetadataType prop = AssetMetadataMapping.MetadataType.valueOf(xmlMapping.getType().toUpperCase(Locale.ROOT));
					newMapping.setMetadataType(prop);
				}catch (Exception ex) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MappingNoValid, xmlMapping.getAssetProperty()));
					continue;
				}
			}
			
			if (xmlMapping.getAssetProperty() != null && !xmlMapping.getAssetProperty().isEmpty()) {
				try {
					AssetMetadataMapping.AssetProperty prop = AssetMetadataMapping.AssetProperty.valueOf(xmlMapping.getAssetProperty().toUpperCase(Locale.ROOT));
					newMapping.setMappedAssetProperty(prop);
				}catch (Exception ex) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_InvalidMappingAssetProperty, xmlMapping.getAssetProperty()));
					continue;
				}
			}
			
			if (xmlMapping.getCategoryKey() != null && !xmlMapping.getCategoryKey().isEmpty()) {
				String hkey = xmlMapping.getCategoryKey();
				List<Category> categories = QueryFactory.buildQuery(session, Category.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"hkey", hkey}).list(); //$NON-NLS-1$
				if (categories.isEmpty()) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_CategoryNotFound, xmlMapping.getCategoryKey()));
					continue;
				}else if (categories.size() > 1) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MultipleMappingsFound, xmlMapping.getCategoryKey()));
					continue;
				}else {
					newMapping.setMappedCategory(categories.get(0));
				}
			}
			
			if (xmlMapping.getAttributeKey() != null && !xmlMapping.getAttributeKey().isEmpty()) {
				String key = xmlMapping.getAttributeKey();
				List<org.wcs.smart.ca.datamodel.Attribute> attributes = QueryFactory.buildQuery(session, org.wcs.smart.ca.datamodel.Attribute.class, 
						new Object[] {"conservationArea", ca}, //$NON-NLS-1$
						new Object[] {"keyId", key}).list(); //$NON-NLS-1$
				if (attributes.isEmpty()) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MappingAttributeNotFound, xmlMapping.getAttributeKey()));
					continue;
				}else if (attributes.size() > 1) {
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MappingMultipleAttributesFound, xmlMapping.getAttributeKey()));
					continue;
				}else {
					newMapping.setMappedAttribute(attributes.get(0));
				}
				
				
				if (xmlMapping.getAttributeListItemKey() != null && !xmlMapping.getAttributeListItemKey().isEmpty() && newMapping.getMappedAttribute().getType() ==  org.wcs.smart.ca.datamodel.Attribute.AttributeType.LIST) {
					for (org.wcs.smart.ca.datamodel.AttributeListItem li : newMapping.getMappedAttribute().getAttributeList()) {
						if (li.getKeyId().equalsIgnoreCase(xmlMapping.getAttributeListItemKey())) {
							newMapping.setMappedListItem(li);
							break;
						}
					}
					if (newMapping.getMappedListItem() == null) {
						warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MappingListItemNotFound, xmlMapping.getAttributeListItemKey(), newMapping.getMappedAttribute().getName()));
						continue;
					}
				}
				
				if (xmlMapping.getAttributeTreeNodeKey() != null && !xmlMapping.getAttributeTreeNodeKey().isEmpty() && newMapping.getMappedAttribute().getType() ==  org.wcs.smart.ca.datamodel.Attribute.AttributeType.TREE) {
					List<AttributeTreeNode> toTest = new ArrayList<>();
					toTest.addAll(newMapping.getMappedAttribute().getTree());
					while(!toTest.isEmpty()) {
						AttributeTreeNode test = toTest.remove(0);
						if (test.getHkey().equalsIgnoreCase(xmlMapping.getAttributeTreeNodeKey())) {
							newMapping.setMappedTreeNode(test);
							break;
						}
						if (test.getChildren() != null) toTest.addAll(test.getChildren());
					}
					
					if (newMapping.getMappedTreeNode() == null) {
						warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MappingTreeNodeNotFound, xmlMapping.getAttributeTreeNodeKey(), newMapping.getMappedAttribute().getName()));
						continue;
					}
				}
			}
			
			if (newMapping.getMetadataType() == null) {
				warnings.add(Messages.AssetXmlToAssetData_MappingTypeNotProvided);
				continue;
			}
			if (newMapping.getMappedAssetProperty() == null && newMapping.getMappedCategory() == null && newMapping.getMappedAttribute() == null) {
				warnings.add(Messages.AssetXmlToAssetData_MappingInvalidType);
				continue;
			}
			newMappings.add(newMapping);
		}
		
		//validate 
		List<AssetMetadataMapping> toAdd = new ArrayList<>();
		List<AssetMetadataMapping> existingTypes = QueryFactory.buildQuery(session, AssetMetadataMapping.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (AssetMetadataMapping newMapping : newMappings) {
			AssetMetadataMapping found = null;
			for (AssetMetadataMapping existingMapping : existingTypes) {
				
				if (equals(existingMapping, newMapping)) {
					found = existingMapping;
					break;
				}
			}
			if (found == null) {
				//we need to add this attribute
				toAdd.add(newMapping);
			}else {
				warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_MappingExists, found.getMetadataKey())); 
			}
		}
		return toAdd;
		
	}
	
	private boolean equals(AssetMetadataMapping m1, AssetMetadataMapping m2) {
		if (!m1.getMetadataType().equals(m2.getMetadataType())) return false;
		if (!m1.getMetadataKey().equalsIgnoreCase(m2.getMetadataKey())) return false;
		if (!Objects.equals(m1.getMappedAssetProperty(), m2.getMappedAssetProperty())) return false;
		if (!Objects.equals(m1.getMappedAttribute(), m2.getMappedAttribute())) return false;
		if (!Objects.equals(m1.getMappedCategory(), m2.getMappedCategory())) return false;
		if (!Objects.equals(m1.getMappedListItem(), m2.getMappedListItem())) return false;
		if (!Objects.equals(m1.getMappedTreeNode(), m2.getMappedTreeNode())) return false;
		return true;
	}
	private List<AssetType> processAssetTypes(AssetData xmlData, HashMap<String, AssetAttribute> attributes){
		List<AssetType> newTypes = new ArrayList<>();
		
		for (org.wcs.smart.asset.data.inout.xml.AssetType xmlType : xmlData.getAssetTypes()) {
			
			AssetType assetType = new AssetType();
			
			assetType.setConservationArea(ca);
			assetType.setIcon(xmlType.getIcon());
			assetType.setKeyId(xmlType.getKey().toLowerCase());
			updateNames(assetType, xmlType.getNames());
			assetType.setIncidentCutoff(xmlType.getIncidentCutoff());
			assetType.setAssetAttributes(new ArrayList<>());
			assetType.setAssetDeploymentAttributes(new ArrayList<>());
			
			if (xmlType.getAttributes() != null) {
				for (AttributeMapping m : xmlType.getAttributes()) {
					
					AssetTypeAttribute typeAttribute = new AssetTypeAttribute();
					typeAttribute.setAssetType(assetType);
					typeAttribute.setOrder(m.getOrder());
					
					AssetAttribute targetAttribute = attributes.get(m.getAttributeKey());
					if (targetAttribute == null) {
						warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AssetTypeAttributeNotFound, assetType.getName(), m.getAttributeType(), m.getAttributeKey()));
						
					}else {
						try {
							AssetAttribute.AttributeType type = AssetAttribute.AttributeType.valueOf(m.getAttributeType());
							if (!type.equals(targetAttribute.getType())) {
								warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AssetTypeAssetTypeDoesnotMatch, assetType.getName(), m.getAttributeKey(), m.getAttributeType(), targetAttribute.getType().name()));
								targetAttribute = null;
							}
						}catch (Exception ex) {
							warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AssetTypeTypeNotFound, assetType.getName(), m.getAttributeType(), m.getAttributeKey()));
							targetAttribute = null;	
						}
					}
					if (targetAttribute != null) {
						typeAttribute.setAttribute(targetAttribute);
						assetType.getAssetAttributes().add(typeAttribute);
					}
					
				}
			
				if (xmlType.getDeploymentAttributes() != null) {
					for (AttributeMapping m : xmlType.getDeploymentAttributes()) {
						
						AssetTypeDeploymentAttribute typeAttribute = new AssetTypeDeploymentAttribute();
						typeAttribute.setAssetType(assetType);
						typeAttribute.setOrder(m.getOrder());
						
						AssetAttribute targetAttribute = attributes.get(m.getAttributeKey());
						if (targetAttribute == null) {
							warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AssetTypeDeployAttributeNotFound, assetType.getName(), m.getAttributeType(), m.getAttributeKey()));
							
						}else {
							try {
								AssetAttribute.AttributeType type = AssetAttribute.AttributeType.valueOf(m.getAttributeType());
								if (!type.equals(targetAttribute.getType())) {
									warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_WarningsTitleDeployAssetTypeDoesntMatch, assetType.getName(), m.getAttributeKey(), m.getAttributeType(), targetAttribute.getType().name()));
									targetAttribute = null;
								}
							}catch (Exception ex) {
								warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_WarningsTitleAssettypeNotFound, assetType.getName(), m.getAttributeType(), m.getAttributeKey()));
								targetAttribute = null;	
							}
						}
						if (targetAttribute != null) {
							typeAttribute.setAttribute(targetAttribute);
							assetType.getAssetDeploymentAttributes().add(typeAttribute);
						}
						
					}
				}
			}
			newTypes.add(assetType);
		}
		
		//validate 
		List<AssetType> toAdd = new ArrayList<>();
		List<AssetType> existingTypes = QueryFactory.buildQuery(session, AssetType.class, "conservationArea", ca).list(); //$NON-NLS-1$
		for (AssetType newSource : newTypes) {
			AssetType found = null;
			for (AssetType existingSource : existingTypes) {
				if (existingSource.getKeyId().equals(newSource.getKeyId())) {
					found = existingSource; 
					break;
				}
			}
			if (found == null) {
				//we need to add this attribute
				toAdd.add(newSource);
			}else {
				warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_WarningsTitleMultipleAssetTypes, found.getName(), found.getKeyId())); 
			}
		}
		return toAdd;
	}
	
	private List<AssetAttribute> processAttributes(List<Attribute> xmlAttributes) {
		List<AssetAttribute> newAttributes = new ArrayList<AssetAttribute>();
		
		for (Attribute xmlAttribute : xmlAttributes) {
			AssetAttribute a = new AssetAttribute();
			a.setConservationArea(ca);
			a.setKeyId(xmlAttribute.getKey().toLowerCase());
			a.setType(AssetAttribute.AttributeType.valueOf(xmlAttribute.getType().name()));
			
			updateNames(a, xmlAttribute.getNames());
			
			if (a.getType() == AssetAttribute.AttributeType.LIST && xmlAttribute.getListValues() != null) {
				a.setAttributeList(new ArrayList<>());
				for (AttributeListItem xmlListItem : xmlAttribute.getListValues()) {
					AssetAttributeListItem li = new AssetAttributeListItem();
					li.setAttribute(a);
					li.setKeyId(xmlListItem.getKey());
					updateNames(li, xmlListItem.getNames());		
					
					a.getAttributeList().add(li);
				}
			}
			newAttributes.add(a);
		}
		
		//validate 
		List<AssetAttribute> toAdd = new ArrayList<>();
		List<AssetAttribute> existingAttributes = QueryFactory.buildQuery(session, AssetAttribute.class, "conservationArea", ca).list(); //$NON-NLS-1$
		
		for (AssetAttribute newAttribute : newAttributes) {
			AssetAttribute found = null;
			for (AssetAttribute existingAttribute : existingAttributes) {
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
					warnings.add(MessageFormat.format(Messages.AssetXmlToAssetData_AttributetypeNotMatching, found.getName(), found.getKeyId(), found.getType().getGuiName(Locale.getDefault()), newAttribute.getType().getGuiName(Locale.getDefault()))); 
				}
			}
		}
		
		return toAdd;
	}
	
	private void updateNames(org.wcs.smart.ca.NamedItem item, Collection<NamedItem> names) {
		String defaultValue = null;
		String blankName = Messages.AssetXmlToAssetData_NoNameLabel;
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
