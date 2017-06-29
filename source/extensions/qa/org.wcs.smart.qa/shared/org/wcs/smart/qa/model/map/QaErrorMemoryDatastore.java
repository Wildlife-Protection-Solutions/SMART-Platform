/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.model.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.wcs.smart.qa.model.QaError;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

/**
 * In memory data store for results of QA Validation
 * @author Emily
 *
 */
public class QaErrorMemoryDatastore extends ContentDataStore{

	private List<SimpleFeatureType> schemas;
	private Collection<QaError> errors;
	
	/**
	 * Determines if the error is valid for the given feature type.  This is determined
	 * based on the geometry type of the error object
	 * @param error
	 * @param ftype
	 * @return
	 */
	public static boolean isValid(QaError error, SimpleFeatureType ftype){
		if (error.getGeometryObject() == null) return false;
		if (ftype.getTypeName().equals(FeatureFactory.QA_ERROR_PNT_TYPE_NAME))
			return error.getGeometryObject() instanceof Point;
		if (ftype.getTypeName().equals(FeatureFactory.QA_ERROR_LINE_TYPE_NAME))
			return error.getGeometryObject() instanceof LineString || error.getGeometryObject() instanceof MultiLineString;
		return false;
	}
	
	public QaErrorMemoryDatastore(Collection<QaError> errors){
		this.errors = errors;
	}
	
	public Collection<QaError> getErrors(){
		return this.errors;
	}
	
	
	public SimpleFeatureType getFeatureType(String typeName){
		for (SimpleFeatureType t : schemas){
			if (t.getTypeName().equals(typeName)) return t;
		}
		return null;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new QaErrorFeatureSource(entry, this);
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		schemas = new ArrayList<>();
		try{
			schemas.add(FeatureFactory.createPointQaErrorFeatureType());
			schemas.add(FeatureFactory.createLineQaErrorFeatureType());
		}catch (Exception ex){
			throw new IOException(ex);
		}
		
		List<Name> names = new ArrayList<Name>();
		for (SimpleFeatureType schema : schemas){
			names.add(schema.getName());
		}
		return names;
	}

}

