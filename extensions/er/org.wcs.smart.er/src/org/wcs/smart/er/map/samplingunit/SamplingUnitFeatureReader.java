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
package org.wcs.smart.er.map.samplingunit;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Feature reading from sampling unit georesource.
 * 
 * @author Emily
 *
 */
public class SamplingUnitFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	
	private Session session;
	private Iterator<Object> iterator;
	
	private boolean isRecce;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	@SuppressWarnings("unchecked")
	public SamplingUnitFeatureReader(SurveyDesign sd, SimpleFeatureType ftype) {
		this.ftype = ftype;
		this.session = HibernateManager.openSession();
		
		isRecce = false;
		if (ftype.getTypeName().equals(SamplingUnitType.PLOT.name())){
			Criteria c = session.createCriteria(SamplingUnit.class)
					.add(Restrictions.eq("surveyDesign", sd)) //$NON-NLS-1$
					.add(Restrictions.eq("type", SamplingUnit.SamplingUnitType.PLOT)); //$NON-NLS-1$
			iterator = c.list().iterator();
		}else if (ftype.getTypeName().equals(SamplingUnitType.TRANSECT.name())){
			Criteria c = session.createCriteria(SamplingUnit.class)
					.add(Restrictions.eq("surveyDesign", sd)) //$NON-NLS-1$
					.add(Restrictions.eq("type", SamplingUnit.SamplingUnitType.TRANSECT)); //$NON-NLS-1$
			iterator = c.list().iterator();
		}else if (ftype.getTypeName().equals(SamplingUnitType.RECON.name())){
			Criteria c = session.createCriteria(MissionTrack.class, "mt") //$NON-NLS-1$
					.createAlias("mt.mission", "m") //$NON-NLS-1$ //$NON-NLS-2$
					.createAlias("m.survey", "s") //$NON-NLS-1$ //$NON-NLS-2$
					.add(Restrictions.eq("s.surveyDesign", sd)) //$NON-NLS-1$
					.add(Restrictions.eq("type", TrackType.RECON)); //$NON-NLS-1$
			iterator = c.list().iterator();		
			isRecce = true;	
		}
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		if (session.isOpen()){
			session.close();
		}
	}

	/**
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/**
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		return iterator.hasNext();
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		if (isRecce){
			MissionTrack su = (MissionTrack) iterator.next();
			return createFeature(ftype, su);
		}else{
			SamplingUnit su = (SamplingUnit) iterator.next();
			return createFeature(ftype, su);
		}
	}
	
	public static SimpleFeature createFeature(SimpleFeatureType ftype, MissionTrack su){
		Object[] data = new Object[6];
		data[0] = su.getId() + "." + SmartUtils.encodeHex(su.getUuid()); //$NON-NLS-1$ 
		data[1] = su.getId(); 
		data[2] = su.getMission().getId();
		data[3] = su.getMission().getSurvey().getId();
		data[4] = su.getDate();
		data[5] = su.getLineString();
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);	
	}
	
	public static SimpleFeature createFeature(SimpleFeatureType ftype, SamplingUnit su){
		Object[] data = new Object[su.getSurveyDesign().getSamplingUnitAttributes().size() + 3];
		data[0] = su.getId() + "." + SmartUtils.encodeHex(su.getUuid()); //$NON-NLS-1$ 
		data[1] = su.getId(); 
		int i = 2;
		for (SurveyDesignSamplingUnitAttribute att : su.getSurveyDesign().getSamplingUnitAttributes()){
			for (SamplingUnitAttributeValue v : su.getAttributes()){
				if (att.getSamplingUnitAttribute().equals(v.getSamplingUnitAttribute())){
					data[i] = v.getValueAsString();
					break;
				}
			}
			i++;
		}
		data[i] = su.getGeometry();
		return SimpleFeatureBuilder.build(ftype, data, (String)data[0]);	
	}
}
