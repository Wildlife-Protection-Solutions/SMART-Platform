/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.i2.udig.entity;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.udig.LocationLayerType;

/**
 * Geotools data store for entity locations
 * 
 * @author Emily
 */
public class IntelEntityDataSource extends ContentDataStore{

	private UUID entityUuid;
	private Map<String,Attribute> attributeMap;
	private String entityId;
	
	public IntelEntityDataSource(UUID entityUuid){
		this.entityUuid = entityUuid;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new IntelEntityFeatureSource(entry, entityUuid);
	}

	public void updateEntityId(String entityId) {
		this.entityId = entityId;
	}
	
	@Override
	protected List<Name> createTypeNames() throws IOException {
		attributeMap =  new HashMap<>();
		
		List<Name> names = new ArrayList<Name>();
		for (LocationLayerType layertype : LocationLayerType.values()){
			if (layertype == LocationLayerType.ATTRIBUTE) continue;
			if (layertype == LocationLayerType.DM_OBS) continue;
			names.add(generateName(layertype, entityUuid));
		}
		
		try (Session session = HibernateManager.openSession()){		
			Query<Long> q = session.createQuery("SELECT count(*) FROM IntelEntity e join e.entityType t join t.attributes ta join ta.id.attribute a WHERE a.type = :type and e.uuid = :uuid", Long.class); //$NON-NLS-1$
			q.setParameter("type", IntelAttribute.AttributeType.POSITION); //$NON-NLS-1$
			q.setParameter("uuid", entityUuid); //$NON-NLS-1$
			Long cnt = q.uniqueResult();
			boolean hasPosition = cnt > 0;	
			
			if (hasPosition) {
				names.add(generateName(LocationLayerType.ATTRIBUTE, entityUuid));
			}

			IntelEntity entity = session.get(IntelEntity.class, entityUuid);
			entityId = entity.getIdAttributeAsText();
			if (entity.getDmAttributeListItem() != null) {
				names.add(generateName(LocationLayerType.DM_OBS, entityUuid));
			}
			
			//find all geometry based data model
			List<Attribute> attributes = DataModelManager.INSTANCE.getGeometryAttributes(session);
			for (Attribute attribute : attributes) {
				String type = attribute.getType().name() + "." + attribute.getKeyId(); //$NON-NLS-1$
				names.add(new NameImpl(type)); 
				attributeMap.put(type, attribute);
			}	
			
			if (entity.getEntityType().getDmAttribute() != null) {
				List<CategoryAttribute> options = session.createQuery("FROM CategoryAttribute WHERE id.attribute = :attribute", CategoryAttribute.class) //$NON-NLS-1$
				.setParameter("attribute", entity.getEntityType().getDmAttribute()) //$NON-NLS-1$
				.list();
				
				List<Attribute> dmAttributes = new ArrayList<>();
				for (CategoryAttribute ca : options) {
					Category root = ca.getCategory();
					List<Category> toProcess = new ArrayList<>();
					toProcess.add(root);
					while(!toProcess.isEmpty()) {
						Category c = toProcess.remove(0);
						if (c.getAttributes() != null) {
							for (CategoryAttribute item : c.getAttributes()) {
								if (item.getAttribute().getType().isGeometry()) {
									dmAttributes.add(item.getAttribute());
								}
							}
						}
						if (c.getChildren() != null) toProcess.addAll(c.getChildren());
					}
				}
				
				//need one layer for each dm attribute
				for (Attribute dmAttribute : dmAttributes) {
					String type = "WP_" + dmAttribute.getType().name() + "." + dmAttribute.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
					names.add(new NameImpl(type)); 
					attributeMap.put(type, dmAttribute);
				}	 
			}
		}
		return names;
	}
	
	
	public static Name generateName(LocationLayerType type, UUID entityUuid){
		return new NameImpl(type.name());
	}

	public static Filter createDateTimeFilter(LocalDateTime startDate, LocalDateTime endDate){
		if (startDate == null || endDate == null) return Filter.INCLUDE;
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		return ff.between(ff.property("date"), ff.literal(startDate), ff.literal(endDate)); //$NON-NLS-1$
	}
	
	public static boolean isLineAttribute(String name) {
		return (name.startsWith(Attribute.AttributeType.LINE.name() + ".")); //$NON-NLS-1$
	}
	public static boolean isPolgyonAttribute(String name) {
		return (name.startsWith(Attribute.AttributeType.POLYGON.name() + ".")); //$NON-NLS-1$
	}
	
	/**
	 * these are geometry attributes associated with record locations
	 * @param name
	 * @return
	 */
	public static boolean isGeometryAttribute(String name) {
		return isLineAttribute(name) || isPolgyonAttribute(name);
	}

	public static boolean isObservationLineAttribute(String name) {
		return (name.startsWith("WP_" + Attribute.AttributeType.LINE.name() + ".")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	public static boolean isObservationPolgyonAttribute(String name) {
		return (name.startsWith("WP_" + Attribute.AttributeType.POLYGON.name() + ".")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	/**
	 * these are geometry attributes associated with waypoints (patrol, incident, mission etc)
	 * @param name
	 * @return
	 */
	public static boolean isObservationGeometryAttribute(String name) {
		return isObservationLineAttribute(name) || isObservationPolgyonAttribute(name);
	}
	
	public String getName(String typeName) {
		if (typeName.equals(LocationLayerType.ATTRIBUTE.name())) return MessageFormat.format("{0}: Entity Attributes", entityId);
		if (typeName.equals(LocationLayerType.DM_OBS.name())) return MessageFormat.format("{0}: Observations", entityId);
		if (typeName.equals(LocationLayerType.POINT.name())) return MessageFormat.format("{0}: Record Locations (Point)", entityId);
		if (typeName.equals(LocationLayerType.POLYGON.name())) return MessageFormat.format("{0}: Record Locations (Polygon)", entityId);
		
		if (isGeometryAttribute(typeName)) {
			return MessageFormat.format("{0}: Record Location Observation Attribute - {1}", entityId, attributeMap.get(typeName).getName());
		}
		if (isObservationGeometryAttribute(typeName)){
			return MessageFormat.format("{0}: Observation Attribute - {1}", entityId, attributeMap.get(typeName).getName());
		}
		
		return null;
	}
	
	public Attribute getAttribute(String typeName) {
		return attributeMap.get(typeName);
	}
}
