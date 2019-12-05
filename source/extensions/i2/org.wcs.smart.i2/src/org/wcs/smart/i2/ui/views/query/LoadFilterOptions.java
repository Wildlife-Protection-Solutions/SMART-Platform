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
package org.wcs.smart.i2.ui.views.query;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.model.OtherAttributeGroup;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.Resources;

/**
 * Job for loading roots for filter tree
 * 
 * @author Emily
 *
 */
public class LoadFilterOptions extends Job {

	private FilterTreeContentProvider viewer;
	private AbstractIntelQuery query;
	
	public LoadFilterOptions(FilterTreeContentProvider viewer, AbstractIntelQuery query) {
		super(Messages.LoadFilterOptions_JobName);
		this.viewer= viewer;
		this.query = query;
	}


	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<FilterTreeItem> roots = new ArrayList<FilterTreeItem>();
		try(Session s = HibernateManager.openSession()){
			
			if (query.getTypeKey().equals(IntelEntityRecordQuery.KEY)) {
				roots.add(loadEntity(s));
				roots.add(loadAttributes(s));
				roots.add(loadRecordAttributes(s));				
				BasicTreeFilterItem opRoot = new BasicTreeFilterItem(Messages.LoadFilterOptions_EntityObservationsNode);
				opRoot.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.DATA_MODEL_ICON));
				roots.add(opRoot);
				opRoot.addChild(loadDataModel(s));
				opRoot.addChild(loadAreas(s));
			}else if (query.getTypeKey().equals(IntelRecordQuery.KEY)) {
				roots.add(loadRecordAttributes(s));	
			}else {
				roots.add(loadEntity(s));
				roots.add(loadAttributes(s));
				roots.add(loadDataModel(s));
				roots.add(loadAreas(s));
			}
			roots.add(loadOperators());
		}
		
		Display.getDefault().syncExec(()->{
			viewer.setItems(roots);			
		});
		return Status.OK_STATUS;
	}

	private FilterTreeItem loadOperators(){
		BasicTreeFilterItem opRoot = new BasicTreeFilterItem(Messages.LoadFilterOptions_OperatorsLabel);
		opRoot.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_OPERATOR));
		opRoot.addChild(new OperatorTreeFilterItem(Operator.NOT));
		opRoot.addChild(new OperatorTreeFilterItem(Operator.BRACKETS));
		return opRoot;
	}
	
	private BasicTreeFilterItem loadAreas(Session session){
		BasicTreeFilterItem locationFilters = new BasicTreeFilterItem(Messages.LoadFilterOptions_LocationFilterLabel);
		locationFilters.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_AREA));
		
		Area.AreaType[] types = new Area.AreaType[]{
			AreaType.CA,
			AreaType.BA,
			AreaType.ADMIN,
			AreaType.PATRL,
			AreaType.MNGT
		};
		for (Area.AreaType type : types){
			AreaTypeTreeFilterItem typeNode = new AreaTypeTreeFilterItem(type);
			typeNode.setImageDescriptor(locationFilters.getImage());
			locationFilters.addChild(typeNode);
		}
		return locationFilters;
	}
	
	private BasicTreeFilterItem loadDataModel(Session session){
		
		BasicTreeFilterItem dataModelItem = new BasicTreeFilterItem(Messages.LoadFilterOptions_DataModelFilterLabel);
		dataModelItem.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.DATA_MODEL_ICON));
		
		BasicTreeFilterItem categoryItem = new BasicTreeFilterItem(Messages.LoadFilterOptions_CategoriesFilterLabel);
		categoryItem.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.CATEGORY_ICON));
		dataModelItem.addChild(categoryItem);
		
		List<Category> categories = new ArrayList<>(InternalQueryManager.INSTANCE.getQueryItemProvider().getRootCategories(session));
		categories.sort((a,b)->((Integer)a.getCategoryOrder()).compareTo(b.getCategoryOrder()));
		for (Category category : categories){
			DataModelTreeFilterItem item = new DataModelTreeFilterItem(category);
			categoryItem.addChild(item);
		}
		
		BasicTreeFilterItem attributesItem = new BasicTreeFilterItem(Messages.LoadFilterOptions_DmAttributesFilterLabel);
		attributesItem.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ATTRIBUTE_NUMBER_ICON));
		dataModelItem.addChild(attributesItem);
		List<Attribute> attributes= new ArrayList<>(InternalQueryManager.INSTANCE.getQueryItemProvider().getDmAttributes(session));
		
		attributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for (Attribute a : attributes){
			DataModelTreeFilterItem item = new DataModelTreeFilterItem(a);
			attributesItem.addChild(item);
		}
		
		return dataModelItem;
	}

	
	private FilterTreeItem loadAttributes(Session session){
		AttributeHeaderFilterItem attributeRoots = new AttributeHeaderFilterItem(Messages.LoadFilterOptions_EntityAttributeFilterLabel, false);
		
		HashMap<String, IntelAttribute> attributes = new HashMap<>();
		Set<String> donotuse = new HashSet<>();
		for (IntelProfile ip : ProfilesManager.INSTANCE.getActiveProfiles()) {
			if (!IntelSecurityManager.INSTANCE.canViewQuery(ip)) continue;
			ip = session.get(IntelProfile.class, ip.getUuid());
			for (IntelEntityType et : ip.getEntityTypes()) {
				for (IntelEntityTypeAttribute e : et.getAttributes()) {
					if (donotuse.contains(e.getAttribute().getKeyId())) continue;					
					if (!attributes.containsKey(e.getAttribute().getKeyId())) {
						attributes.put(e.getAttribute().getKeyId(), e.getAttribute());
					}else {
						//if the attribute types are not the same we may want to remove the attribute
						//and make sure we don't re-add it
						if (!e.getAttribute().getType().equals(attributes.get(e.getAttribute().getKeyId()).getType())) {
							attributes.remove(e.getAttribute().getKeyId());
							donotuse.add(e.getAttribute().getKeyId());
						}
					}
				}
			}
		}
		List<IntelAttribute> all = new ArrayList<>(attributes.values());
		all.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for (IntelAttribute a : all){
			//only include attribute associated with entity type that is valid
			AttributeTreeFilterItem item = new AttributeTreeFilterItem(a, true, false);
			attributeRoots.addChild(item);
		}
		
		SystemAttributeFilterItem dateCreated = new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.ENTITY_DATE_CREATED);
		SystemAttributeFilterItem dateModified = new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.ENTITY_DATE_MODIFIED);
		
		attributeRoots.addChild(dateCreated);
		attributeRoots.addChild(dateModified);
		
		return attributeRoots;
	}
	
	private FilterTreeItem loadEntity(Session session){
		BasicTreeFilterItem entityTypeRoot = new BasicTreeFilterItem(Messages.LoadFilterOptions_EntityTypeFilterLabel);
		entityTypeRoot.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ENTITY));

		List<IntelEntityType> types =
			InternalQueryManager.INSTANCE.getQueryItemProvider().getEntityTypes(ProfilesManager.INSTANCE.getActiveProfileIds(), session);
				
		types.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for(IntelEntityType t : types){
			EntityTreeFilterItem entityNode = new EntityTreeFilterItem(t, false);
			entityTypeRoot.addChild(entityNode);
			final byte[] icon = t.getIcon();
			if (icon != null){
				entityNode.setImageDescriptor(new ImageDescriptor() {
					@Override
					public ImageData getImageData() {
						try(ByteArrayInputStream in = new ByteArrayInputStream(icon)){
							BufferedImage image = ImageIO.read(in);
							if (image != null){
								return AWTSWTImageUtils.convertToSWTImage(image).getImageData();
							}
						}catch (Exception ex){
							
						}
						return null;
					}
				});
				
			}
			
			EntityTreeFilterItem anyNode = new EntityTreeFilterItem(t);
			entityNode.addChild(anyNode);
						
			EntityTypeTreeFilterItem entitiesNode = new EntityTypeTreeFilterItem(t);
			entityNode.addChild(entitiesNode);
			
			AttributeHeaderFilterItem attributesNode = new AttributeHeaderFilterItem(Messages.LoadFilterOptions_AttributeFilterLabel, false);
			entityNode.addChild(attributesNode);

			Map<IntelEntityTypeAttributeGroup, List<IntelEntityTypeAttribute>> attributeMapping = new HashMap<>();
			List<IntelEntityTypeAttribute> typeAttributes = InternalQueryManager.INSTANCE.getQueryItemProvider().getEntityTypeAttributes(t, session);
			for (IntelEntityTypeAttribute a : typeAttributes){
				if (attributeMapping.get(a.getAttributeGroup()) == null){
					attributeMapping.put(a.getAttributeGroup(), new ArrayList<>());
				}
				attributeMapping.get(a.getAttributeGroup()).add(a);
			}
			
			List<IntelEntityTypeAttribute> eattributes = InternalQueryManager.INSTANCE.getQueryItemProvider().getEntityTypeAttributes(t, session);
			Stream<IntelEntityTypeAttributeGroup> groups = eattributes.stream()
				.map(IntelEntityTypeAttribute::getAttributeGroup)
				.distinct().sorted(Comparator.nullsLast((a,b)-> ((Integer)a.getOrder()).compareTo(b.getOrder())));
			
			groups.forEach(g->{
				String name = null;
				if (g == null){
					name = OtherAttributeGroup.INSTANCE.getName();
				}else{
					name = g.getName();
				}
				AttributeHeaderFilterItem attributeGroupNode =  new AttributeHeaderFilterItem(name, true);
				attributesNode.addChild(attributeGroupNode);
				
				attributeMapping.get(g).forEach(attribute->{
					AttributeTreeFilterItem attributeNode = new AttributeTreeFilterItem(attribute);
					attributeGroupNode.addChild(attributeNode);
				});
				
			});
			
		}
		return entityTypeRoot;	
	}
	
	private FilterTreeItem loadRecordAttributes(Session session) {
		BasicTreeFilterItem recordSourceRoot = new BasicTreeFilterItem(Messages.LoadFilterOptions_RecordAttributesNode);
		recordSourceRoot.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD));
		
		recordSourceRoot.addChild(new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.RECORD_DATE));
		recordSourceRoot.addChild(new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.RECORD_STATUS));
		recordSourceRoot.addChild(new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.RECORD_SOURCE));
		recordSourceRoot.addChild(new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED));
		recordSourceRoot.addChild(new SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED));
		
		
		Set<UUID> profiles = ProfilesManager.INSTANCE.getActiveProfiles().stream()
				.filter(f->IntelSecurityManager.INSTANCE.canViewQuery(f))
				.map(f->f.getUuid()).collect(Collectors.toSet());
				
		List<IntelRecordSource> sources = InternalQueryManager.INSTANCE.getQueryItemProvider()
				.getRecordSources(profiles, session);
		
		for (IntelRecordSource type : sources) {
			BasicTreeFilterItem sourceRoot = new BasicTreeFilterItem(type.getName());
			Image img = Resources.INSTANCE.getImage(type);
			if (img != null) {
				sourceRoot.setImageDescriptor(ImageDescriptor.createFromImage(img));
			}
			recordSourceRoot.addChild(sourceRoot);
			
			
			List<IntelRecordSourceAttribute> thisattributes = new ArrayList<>(InternalQueryManager.INSTANCE.getQueryItemProvider().getRecordSourceAttributes(type, session));
			for (IntelRecordSourceAttribute  a : thisattributes) {			
				sourceRoot.addChild(new AttributeTreeFilterItem(a));
			}
		}
		return recordSourceRoot;
	}
}
