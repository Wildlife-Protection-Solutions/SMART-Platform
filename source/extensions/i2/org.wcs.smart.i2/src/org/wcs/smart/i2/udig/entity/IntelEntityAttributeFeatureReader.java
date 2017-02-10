package org.wcs.smart.i2.udig.entity;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class IntelEntityAttributeFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<IntelEntityAttributeValue> fIterator= null;
	
	public IntelEntityAttributeFeatureReader(UUID entityUuid, SimpleFeatureType ftype) {
		this.ftype = ftype;
		
		Session s = HibernateManager.openSession();
		try{
			
			Query q = s.createQuery("FROM IntelEntityAttributeValue WHERE id.entity.uuid = :entityUuid and id.attribute.type = :type");
			q.setParameter("entityUuid", entityUuid);
			q.setParameter("type", IntelAttribute.AttributeType.POSITION);
			
			List<IntelEntityAttributeValue> value = q.list();
			
			for (Iterator<IntelEntityAttributeValue> iterator = value.iterator(); iterator.hasNext();) {
				IntelEntityAttributeValue intelEntityAttributeValue = (IntelEntityAttributeValue) iterator.next();
				intelEntityAttributeValue.getAttribute().getName();
				if (intelEntityAttributeValue.getAttributeValue() == null) iterator.remove();
				
			}
			fIterator = value.iterator();
		}finally{
			s.close();
		}		
	}


	/* (non-Javadoc)
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		
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
		return getEntityAttributeAsFeature(fIterator.next(), ftype);
	}
	
	private static SimpleFeature getEntityAttributeAsFeature(IntelEntityAttributeValue location, SimpleFeatureType ftype){
		Object data[] = new Object[3];		
		data[0] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(location.getNumberValue(), location.getNumberValue2()));
		data[1] = ftype.getName() + "." + UuidUtils.uuidToString(location.getAttribute().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		data[2] = location.getAttribute().getName();
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
}