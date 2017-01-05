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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.OtherAttributeGroup;
import org.wcs.smart.i2.query.Operator;

/**
 * Job for loading roots for filter tree
 * 
 * @author Emily
 *
 */
public class LoadFilterOptions extends Job {

	private TreeViewer viewer;
	
	public LoadFilterOptions(TreeViewer viewer) {
		super("Loading query filters....");
		this.viewer= viewer;
	}


	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<FilterTreeItem> roots = new ArrayList<FilterTreeItem>();
		Session s = HibernateManager.openSession();
		try{
			roots.add(loadEntity(s));
			roots.add(loadAttributes(s));
			roots.add(loadDataModel(s));
			roots.add(loadAreas(s));
			roots.add(loadOperators());
		}finally{
			s.close();
		}
		
		Display.getDefault().syncExec(()->{
			viewer.setInput(roots);
		});
		return Status.OK_STATUS;
	}

	private FilterTreeItem loadOperators(){
		BasicTreeFilterItem opRoot = new BasicTreeFilterItem("Operators");
		Operator[] ops = new Operator[]{
				Operator.NOT,
				Operator.BRACKETS
		};
		for (Operator o : ops){
			OperatorTreeFilterItem item = new OperatorTreeFilterItem(o);
			opRoot.addChild(item);
		}
		return opRoot;
	}
	
	private FilterTreeItem loadAreas(Session session){
		BasicTreeFilterItem locationFilters = new BasicTreeFilterItem("Location Filters");
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
	
	private FilterTreeItem loadDataModel(Session session){
		
		BasicTreeFilterItem dataModelItem = new BasicTreeFilterItem("Data Model");
		dataModelItem.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.DATA_MODEL_ICON));
		
		BasicTreeFilterItem categoryItem = new BasicTreeFilterItem("Categories");
		categoryItem.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.CATEGORY_ICON));
		dataModelItem.addChild(categoryItem);
		
		List<Category> categories = session.createCriteria(Category.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
			.add(Restrictions.isNull("parent"))
			.addOrder(Order.asc("categoryOrder"))
			.list();
		for (Category c : categories){
			DataModelTreeFilterItem item = new DataModelTreeFilterItem(c);
			categoryItem.addChild(item);
		}
		
		BasicTreeFilterItem attributesItem = new BasicTreeFilterItem("Attributes");
		attributesItem.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ATTRIBUTE_NUMBER_ICON));
		dataModelItem.addChild(attributesItem);
		List<Attribute> attributes= 
				session.createCriteria(Attribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		attributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for (Attribute a : attributes){
			DataModelTreeFilterItem item = new DataModelTreeFilterItem(a);
			attributesItem.addChild(item);
		}
		
		return dataModelItem;
	}
	
	private FilterTreeItem loadAttributes(Session session){
		AttributeHeaderFilterItem attributeRoots = new AttributeHeaderFilterItem("Entity Attributes", false);
		
		List<IntelAttribute> attributes= 
				session.createCriteria(IntelAttribute.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		attributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for (IntelAttribute a : attributes){
			AttributeTreeFilterItem item = new AttributeTreeFilterItem(a);
			attributeRoots.addChild(item);
		}
		
		return attributeRoots;
	}
	
		
	private FilterTreeItem loadEntity(Session session){
		BasicTreeFilterItem entityTypeRoot = new BasicTreeFilterItem("Entity Types");
		entityTypeRoot.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ENTITY));
		List<IntelEntityType> types = 
				session.createCriteria(IntelEntityType.class).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
		types.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		for(IntelEntityType t : types){
			BasicTreeFilterItem entityNode = new BasicTreeFilterItem(t.getName());
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
			
			AttributeHeaderFilterItem attributesNode = new AttributeHeaderFilterItem("Attributes", false);
			entityNode.addChild(attributesNode);

			Map<IntelEntityTypeAttributeGroup, List<IntelEntityTypeAttribute>> attributeMapping = new HashMap<>();
			for (IntelEntityTypeAttribute a : t.getAttributes()){
				if (attributeMapping.get(a.getAttributeGroup()) == null){
					attributeMapping.put(a.getAttributeGroup(), new ArrayList<>());
				}
				attributeMapping.get(a.getAttributeGroup()).add(a);
			}
			
			Stream<IntelEntityTypeAttributeGroup> groups = t.getAttributes().stream()
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
}
