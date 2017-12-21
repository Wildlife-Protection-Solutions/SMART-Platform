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
package org.wcs.smart.asset;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.query.Query;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.QueryFactory;


/**
 * Clones intelligence template details
 * 
 * @author Emily
 *
 */
public class ConservationAreaCloner implements IConservationAreaTemplateCloner{

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		
		SubMonitor progress = SubMonitor.convert(monitor, "Copying Asset module data", 6);
		
		progress.subTask("copying asset attributes");
		cloneAttributes(engine);
		progress.worked(1);
		
		progress.subTask("Clone asset types");
		cloneAssetType(engine);
		progress.worked(1);
		
		progress.subTask("Clone metadata mappings");
		cloneMetadataMappings(engine);
		progress.worked(1);
		
		progress.subTask("Clone asset module settings");
		cloneModuleSettings(engine);
		progress.worked(1);
		
		progress.subTask("Clone station and location attributes");
		cloneStationAttributeSettings(engine);
		cloneStationLocationAttributeSettings(engine);
		progress.worked(1);		
	}

	private void cloneMetadataMappings(ConservationAreaClonerEngine engine) throws Exception{
		List<AssetMetadataMapping> mappings = QueryFactory.buildQuery(engine.getSession(), AssetMetadataMapping.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (AssetMetadataMapping mapping : mappings){
			AssetMetadataMapping clone = new AssetMetadataMapping();
			clone.setConservationArea(engine.getNewCa());
			clone.setMappedAssetProperty(mapping.getMappedAssetProperty());
			if (mapping.getMappedAttribute() != null) {
				Attribute a = findNewAttribute(mapping.getMappedAttribute(), engine);
				if (a == null) throw new Exception("Cloned datamodel attribute not found");
				clone.setMappedAttribute(a);
			}
			
			if (mapping.getMappedCategory() != null) {
				Category a = findNewCategory(mapping.getMappedCategory(), engine);
				if (a == null) throw new Exception("Cloned datamodel category not found");
				clone.setMappedCategory(a);
			}
			
			if (mapping.getMappedListItem() != null) {
				AttributeListItem a = findNewAttributeListItem(mapping.getMappedListItem(), engine);
				if (a == null) throw new Exception("Cloned datamodel attribute list item not found");
				clone.setMappedListItem(a);
			}
			
			if (mapping.getMappedTreeNode() != null) {
				AttributeTreeNode a = findNewAttributeTreeNode(mapping.getMappedTreeNode(), engine);
				if (a == null) throw new Exception("Cloned datamodel attribute tree node not found");
				clone.setMappedTreeNode(a);
			}
			
			clone.setMetadataKey(mapping.getMetadataKey());
			clone.setMetadataType(mapping.getMetadataType());
			clone.setSearchOrder(mapping.getSearchOrder());
			
			engine.getSession().save(clone);
	
		}
		engine.getSession().flush();
	}
	
	private void cloneStationAttributeSettings(ConservationAreaClonerEngine engine) throws Exception{
		List<AssetStationAttribute> settings = QueryFactory.buildQuery(engine.getSession(), AssetStationAttribute.class, "attribute.conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (AssetStationAttribute setting : settings){
			AssetStationAttribute clone = new AssetStationAttribute();
			
			
			AssetAttribute a = (AssetAttribute) engine.getNewConservationItem(setting.getAttribute());
			if (a == null) throw new Exception("Station attribute not found");
			clone.setAttribute(a);
			clone.setOrder(setting.getOrder());
			engine.getSession().save(clone);
	
		}
		engine.getSession().flush();
	}
	
	private void cloneStationLocationAttributeSettings(ConservationAreaClonerEngine engine) throws Exception{
		List<AssetStationLocationAttribute> settings = QueryFactory.buildQuery(engine.getSession(), AssetStationLocationAttribute.class, "attribute.conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (AssetStationLocationAttribute setting : settings){
			AssetStationLocationAttribute clone = new AssetStationLocationAttribute();
			
			
			AssetAttribute a = (AssetAttribute) engine.getNewConservationItem(setting.getAttribute());
			if (a == null) throw new Exception("Station location attribute not found");
			clone.setAttribute(a);
			clone.setOrder(setting.getOrder());
			engine.getSession().save(clone);
	
		}
		engine.getSession().flush();
	}
	
	private void cloneModuleSettings(ConservationAreaClonerEngine engine){
		List<AssetModuleSettings> settings = QueryFactory.buildQuery(engine.getSession(), AssetModuleSettings.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (AssetModuleSettings setting : settings){
			AssetModuleSettings clone = new AssetModuleSettings();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(setting.getKeyId());
			clone.setValue(setting.getValue());
			engine.getSession().save(clone);
	
		}
		engine.getSession().flush();
	}
	
	private void cloneAttributes(ConservationAreaClonerEngine engine){
		List<AssetAttribute> attributes = QueryFactory.buildQuery(engine.getSession(), AssetAttribute.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (AssetAttribute ia : attributes){
			AssetAttribute clone = new AssetAttribute();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(ia.getKeyId());
			engine.copyLabels(ia, clone);
			clone.setType(ia.getType());
			
			if (ia.getAttributeList() != null){
				clone.setAttributeList(new ArrayList<AssetAttributeListItem>());
				for (AssetAttributeListItem i : ia.getAttributeList()){
					AssetAttributeListItem clonei = new AssetAttributeListItem();
					clonei.setAttribute(clone);
					clonei.setKeyId(i.getKeyId());
					engine.copyLabels(i, clonei);
					clone.getAttributeList().add(clonei);
				}
			}
			engine.addConservationItemMapping(ia, clone);
			engine.getSession().save(clone);
			engine.getSession().flush();
		}	
	}
	
	private void cloneAssetType(ConservationAreaClonerEngine engine) throws Exception{
		List<AssetType> assets = QueryFactory.buildQuery(engine.getSession(), AssetType.class, "conservationArea", engine.getTemplateCa()).list(); //$NON-NLS-1$
		
		for (AssetType asset : assets){
			AssetType clone = new AssetType();
			clone.setConservationArea(engine.getNewCa());
			clone.setKeyId(asset.getKeyId());
			engine.copyLabels(asset, clone);
			
			clone.setIcon(asset.getIcon());
			clone.setIncidentCutoff(asset.getIncidentCutoff());
			
			clone.setAssetAttributes(new ArrayList<>());
			clone.setAssetDeploymentAttributes(new ArrayList<>());
			
			
			for (AssetTypeAttribute attribute : asset.getAssetAttributes()) {
				AssetTypeAttribute cloneAttribute = new AssetTypeAttribute();
				cloneAttribute.setAssetType(clone);
				cloneAttribute.setOrder(attribute.getOrder());
				
				AssetAttribute a = (AssetAttribute) engine.getNewConservationItem(attribute.getAttribute());
				if (a == null) throw new Exception("Asset type attribute not found");
				cloneAttribute.setAttribute(a);
				clone.getAssetAttributes().add(cloneAttribute);
			}
			
			for (AssetTypeDeploymentAttribute attribute : asset.getAssetDeploymentAttributes()) {
				AssetTypeDeploymentAttribute cloneAttribute = new AssetTypeDeploymentAttribute();
				cloneAttribute.setAssetType(clone);
				cloneAttribute.setOrder(attribute.getOrder());
				
				AssetAttribute a = (AssetAttribute) engine.getNewConservationItem(attribute.getAttribute());
				if (a == null) throw new Exception("Asset type deployment attribute not found");
				cloneAttribute.setAttribute(a);
				clone.getAssetDeploymentAttributes().add(cloneAttribute);
			}
			
			engine.getSession().save(clone);
		}	
		engine.getSession().flush();
	}
	
	private Category findNewCategory(Category oldCategory, ConservationAreaClonerEngine engine) throws Exception{
		List<Category> categories = QueryFactory.buildQuery(engine.getSession(), Category.class,
				new Object[] {"conservationArea", engine.getNewCa()}, //$NON-NLS-1$
				new Object[] {"hkey", oldCategory.getHkey()}).list(); //$NON-NLS-1$
		if (categories.size() == 1){
			return (Category) categories.get(0);
		}
		return null;
	}
	
	private Attribute findNewAttribute(Attribute oldAttribute, ConservationAreaClonerEngine engine) throws Exception{
		List<Attribute> attributes = QueryFactory.buildQuery(engine.getSession(), Attribute.class,
				new Object[] {"conservationArea", engine.getNewCa()}, //$NON-NLS-1$
				new Object[] {"keyId", oldAttribute.getKeyId()}).list(); //$NON-NLS-1$
		if (attributes.size() == 1){
			return (Attribute) attributes.get(0);
		}
		return null;
	}
	
	private AttributeListItem findNewAttributeListItem(AttributeListItem oldListItem, ConservationAreaClonerEngine engine) throws Exception{
		Query<AttributeListItem> q = engine.getSession().createQuery("From AttributeListItem a WHERE a.attribute.conservationArea = :ca and a.keyId = :key and a.attribute.keyId = :attributeKey", AttributeListItem.class); //$NON-NLS-1$
		q.setParameter("ca", engine.getNewCa()); //$NON-NLS-1$
		q.setParameter("key", oldListItem.getKeyId()); //$NON-NLS-1$
		q.setParameter("attributeKey", oldListItem.getAttribute().getKeyId()); //$NON-NLS-1$
		List<?> items = q.list();
		if (items.size() == 1){
			return (AttributeListItem) items.get(0);
		}
		//this may be an entity list item that is not cloned
		return null;
		
		
	}
	
	private AttributeTreeNode findNewAttributeTreeNode(AttributeTreeNode oldTreeNode, ConservationAreaClonerEngine engine) throws Exception{
		Query<AttributeTreeNode> q = engine.getSession().createQuery("From AttributeTreeNode a WHERE a.attribute.conservationArea = :ca and a.hkey = :key and a.attribute.keyId = :attributeKey", AttributeTreeNode.class); //$NON-NLS-1$
		q.setParameter("ca", engine.getNewCa()); //$NON-NLS-1$
		q.setParameter("key", oldTreeNode.getHkey()); //$NON-NLS-1$
		q.setParameter("attributeKey", oldTreeNode.getAttribute().getKeyId()); //$NON-NLS-1$
		List<?> items = q.list();
		if (items.size() == 1){
			return (AttributeTreeNode) items.get(0);
		}
		return null;
	}
}
