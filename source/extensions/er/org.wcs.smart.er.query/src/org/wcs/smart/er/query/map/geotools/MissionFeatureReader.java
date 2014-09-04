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
package org.wcs.smart.er.query.map.geotools;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.FeatureReader;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.query.engine.ISurveyQueryMissionResult;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;

/**
 * Feature reading for mission tracks associated with observation
 * query.
 * 
 * @author Emily
 *
 */
public class MissionFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<byte[]> fIterator;
		
	private Session session;
	
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public MissionFeatureReader(SimpleQuery query,
			SimpleFeatureType ftype) {
		
		this.ftype = ftype;
		this.fIterator = null;

		session = HibernateManager.openSession();
		
		Object cachedResults;
		try {
			cachedResults = query.getCachedResults(new NullProgressMonitor());
			if (cachedResults instanceof ISurveyQueryMissionResult){
				fIterator = ((ISurveyQueryMissionResult) cachedResults).getMissionUuids().iterator();
			}
		} catch (Exception e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		session.close();
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
		if (fIterator == null){
			return false;
		}
		return fIterator.hasNext();
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		byte[] next = (byte[]) this.fIterator.next();
		Mission mission = (Mission) session.load(Mission.class, next);
		SimpleFeature f = SurveyResultItemFeature.createObservationFeature(mission, ftype);
		return f;
	}

}
