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
package org.wcs.smart.geotools.data.smart;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Smart area feature reader
 * @author Emily
 * @since 1.0.0
 */
public class SmartFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private IDatabaseConnectionProvider provider = null;
	private Session session = null;
	private ScrollableResults itemCursor = null;
	private UUID ca;
	private boolean createTransaction = false;
	private AreaType atype;
	
	public SmartFeatureReader(UUID ca,
			AreaType type,SimpleFeatureType ftype, 
			IDatabaseConnectionProvider dbProvider) {
		this.ca = ca;
		this.provider = dbProvider;
		this.ftype = ftype;
		this.atype = type;
	}
	

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		if (itemCursor != null) itemCursor.close();
		if (session != null){
			if (createTransaction){
				//we created this session/transaction so we need to cleanup
				//otherwise somebody else created it and we are just using it; so
				//donot cleanup
				session.getTransaction().rollback();
			}
			provider.finishSession(session);
		}
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
		if (itemCursor == null){
			this.session = provider.openSession();
			if (!session.getTransaction().isActive()){
				this.session.beginTransaction();
				createTransaction = true;
			}
			
			itemCursor = session.createCriteria(Area.class)
					.add(Restrictions.eq("conservationArea.uuid", ca)) //$NON-NLS-1$
					.add(Restrictions.eq("type", atype)).setReadOnly(true) //$NON-NLS-1$
					.setCacheable(false).scroll(ScrollMode.FORWARD_ONLY); 
			
		}
		return itemCursor.next();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException,
			NoSuchElementException {
		//String spec = "geom:MultiPolygon:srid=4326,uuid:String,id:String,key:String";
		Area a = (Area)itemCursor.get(0);
		String fid = ftype.getTypeName() + "." + a.getKeyId(); //$NON-NLS-1$
		Object values[] = new Object[5];
		values[0] = a.getGeometry();
		values[1] = fid;
		values[2] = a.getName();
		values[3] = a.getKeyId();
		values[4] = UuidUtils.uuidToString(a.getUuid());
		
		return SimpleFeatureBuilder.build(ftype, values, fid);
	}
}
