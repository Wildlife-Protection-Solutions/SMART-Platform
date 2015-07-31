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
package org.wcs.smart.entity.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.ccca.EntityTypeCcaaManager;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Feature Reader for reading fixed entity type entity features.
 * @author Emily
 * @since 1.0.0
 */
public class FixedEntityDataSourceFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	
	private Iterator<?> fIterator;
	private EntityType entityType;
	private static GeometryFactory gf = new GeometryFactory();
	
	private Session s;
	
	public FixedEntityDataSourceFeatureReader(EntityType entityType, SimpleFeatureType ftype) {
		this.ftype = ftype;
		this.entityType = entityType;
	
		s = HibernateManager.openSession();
		if (SmartDB.isMultipleAnalysis()){
			fIterator = EntityTypeCcaaManager.getInstance().getEntities(entityType.getKeyId(), s).iterator();
		}else{
			entityType = (EntityType) s.merge(entityType);
			List<Entity> pnts = new ArrayList<Entity>();
			if (entityType.getEntities() != null){
				pnts.addAll(entityType.getEntities());
			}
			fIterator = pnts.iterator();
		}
	}
	

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		s.close();
		
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
		return fIterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		return getFeature((Entity)this.fIterator.next());
	}
	
	
	private SimpleFeature getFeature(Entity entity){
		Object data[] = new Object[entityType.getAttributes().size() + 4];
		
		data[0] = entity.getId() + "." + UuidUtils.uuidToString(entity.getUuid()); //$NON-NLS-1$ 
		data[1] = entity.getId();
		data[2] = entity.getStatus().getGuiName();
		
		for (int i = 0; i < entityType.getAttributes().size();i++){
			EntityAttribute ea = entityType.getAttributes().get(i);
			EntityAttributeValue value = entity.findAttribute(ea.getKeyId());
			if (value == null){
				data[i+3] = null;
			}else{
				if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
					data[i+3] = value.getNumberValue() < 0.5 ? 0 : 1;
				}else{
					data[i+3] = value.getValueAsString();
				}
			}
		}
		data[data.length - 1] = gf.createPoint(new Coordinate(entity.getX(), entity.getY()));
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);
	}
	
	}
