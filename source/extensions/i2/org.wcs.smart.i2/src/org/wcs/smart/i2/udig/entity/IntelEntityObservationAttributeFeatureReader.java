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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Finds all geometry attributes associated with the specific attribute that are
 * recorded as waypoint observations from patrols/incidents/missions etc.
 * 
 * The cavet here is that the observation also has to be associated to the entity
 * via the data model attribute link
 * 
 * @author Emily
 */
public class IntelEntityObservationAttributeFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private ScrollableResults<WaypointObservationAttribute> fIterator = null;
	private boolean isArea = false;
	private Session session;
	
	public IntelEntityObservationAttributeFeatureReader(UUID entityUuid,
			String typeName,
			SimpleFeatureType ftype, LocalDateTime[] dFilter) {
		this.ftype = ftype;
		session = HibernateManager.openSession();
		
		
		IntelEntity entity = session.get(IntelEntity.class, entityUuid);
		if (entity.getDmAttributeListItem() == null) {
			//nothing to do 
			return;
		}
		
		
		if (IntelEntityDataSource.isObservationPolgyonAttribute(typeName)) {
			isArea = true;
		}
		
		String attributeKey = typeName.split("\\.")[1]; //$NON-NLS-1$
		AttributeListItem li = entity.getDmAttributeListItem();

		String query1 = "SELECT observation FROM WaypointObservationAttribute WHERE attributeListItem = :entity "; //$NON-NLS-1$
		String query2 = "FROM WaypointObservationAttribute WHERE attribute.keyId = :keyId and attribute.conservationArea = :ca and observation IN (" + query1 + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		
		
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			query2 += "and observation.observationGroup.waypoint.dateTime between :d1 and :d2"; //$NON-NLS-1$
		}
		Query<WaypointObservationAttribute> hquery = session.createQuery(query2, WaypointObservationAttribute.class)
				.setParameter("entity", li) //$NON-NLS-1$
				.setParameter("keyId", attributeKey) //$NON-NLS-1$
				.setParameter("ca", entity.getConservationArea()); //$NON-NLS-1$
				
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			hquery.setParameter("d1", dFilter[0]); //$NON-NLS-1$
			hquery.setParameter("d2", dFilter[1]); //$NON-NLS-1$
		}

		fIterator = hquery.unwrap(Query.class).setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
	}
	
	public IntelEntityObservationAttributeFeatureReader(UUID entityUuid, String typeName, SimpleFeatureType ftype) {
		this(entityUuid, typeName, ftype, null);
	}
	

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		session.close();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		if (fIterator == null) return false;
		return fIterator.next();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {		
		return getObservationAttributeAsGeometry(ftype, isArea, fIterator.get());
	}
	
	public static SimpleFeature getObservationAttributeAsGeometry(
			SimpleFeatureType ftype, boolean hasArea, WaypointObservationAttribute value) {
		
		Object data[] = new Object[ftype.getAttributeCount()];
		int i = 0;
		data[i++] = value.getGeometry().getGeometry();
		data[i++] = value.getUuid().toString();
		data[i++] = value.getObservation().getUuid().toString();
		data[i++] = value.getObservation().getObservationGroup().getWaypoint().getUuid().toString();
		data[i++] = value.getAttribute().getName();
		data[i++] = value.getAttribute().getKeyId();
		data[i++] = value.getGeometry().getPerimeter();
		if (hasArea)  data[i++] = value.getGeometry().getArea();
		data[i++] = value.getGeometry().getSource().getLabel(Locale.getDefault());
		data[i++] = value.getObservation().getObservationGroup().getWaypoint().getId();
		data[i++] = value.getObservation().getObservationGroup().getWaypoint().getDateTime();
		data[i++] = value.getObservation().getCategory().getName();
		data[i++] = value.getObservation().getCategory().getHkey();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}