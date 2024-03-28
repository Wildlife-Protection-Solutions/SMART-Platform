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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelObservationAttribute;

/**
 * Finds all geometry attributes associated with the specific attribute that are
 * recorded as record location observation
 * 
 * @author Emily
 */
public class IntelEntityRecordObservationAttributeFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private ScrollableResults<IntelObservationAttribute> fIterator = null;
	private boolean isArea = false;
	private Session session;
	
	public IntelEntityRecordObservationAttributeFeatureReader(UUID entityUuid,
			String typeName,
			SimpleFeatureType ftype, LocalDateTime[] dFilter) {
		this.ftype = ftype;
		session = HibernateManager.openSession();
		
		if (IntelEntityDataSource.isPolgyonAttribute(typeName)) {
			isArea = true;
		}
		
		String attributeKey = typeName.split("\\.")[1]; //$NON-NLS-1$
		

		String query = "FROM IntelObservationAttribute WHERE attribute.keyId = :attributeKey AND geom is not NULL AND observation.location in (SELECT id.location FROM IntelEntityLocation WHERE id.entity.uuid = :uuid) "; //$NON-NLS-1$
		
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			query += "and observation.location.dateTime between :d1 and :d2"; //$NON-NLS-1$
		}
		Query<IntelObservationAttribute> hquery = session.createQuery(query, IntelObservationAttribute.class)
				.setParameter("attributeKey", attributeKey) //$NON-NLS-1$
				.setParameter("uuid", entityUuid); //$NON-NLS-1$
				
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			hquery.setParameter("d1", dFilter[0]); //$NON-NLS-1$
			hquery.setParameter("d2", dFilter[1]); //$NON-NLS-1$
		}

		fIterator = hquery.unwrap(Query.class).setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
	}
	
	public IntelEntityRecordObservationAttributeFeatureReader(UUID entityUuid, String typeName, SimpleFeatureType ftype) {
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
			SimpleFeatureType ftype, boolean hasArea, IntelObservationAttribute value) {
		
		Object data[] = new Object[ftype.getAttributeCount()];
		int i = 0;
		data[i++] = value.getGeometry().getGeometry();
		data[i++] = value.getUuid().toString();
		data[i++] = value.getObservation().getUuid().toString();
		data[i++] = value.getObservation().getLocation().getUuid().toString();
		data[i++] = value.getAttribute().getName();
		data[i++] = value.getAttribute().getKeyId();
		data[i++] = value.getGeometry().getPerimeter();
		if (hasArea)  data[i++] = value.getGeometry().getArea();
		data[i++] = value.getGeometry().getSource().getLabel(Locale.getDefault());
		data[i++] = value.getObservation().getLocation().getId();
		data[i++] = value.getObservation().getLocation().getDateTime();
		data[i++] = value.getObservation().getCategory().getName();
		data[i++] = value.getObservation().getCategory().getHkey();
		
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}