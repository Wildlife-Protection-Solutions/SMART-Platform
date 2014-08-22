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
package org.wcs.smart.er.ui.samplingunit.load;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ng.ShapefileDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;

/**
 * Shapefile sampling unit importer.
 * 
 * @author Emily
 *
 */
public class ShpSamplingUnitImporter implements ISamplingUnitImporter{

	@Override
	public String[] getFieldNames(File f, Map<String, Object> options)
			throws Exception {
		ShapefileDataStore store = new ShapefileDataStore(f.toURI().toURL());
		SimpleFeatureType type = store.getSchema();
		
		String[] attributes = new String[type.getAttributeCount()];
		int i = 0;
		for (AttributeDescriptor d : type.getAttributeDescriptors()){
			attributes[i++] = d.getLocalName();
		}
		return attributes;
	}

	@Override
	public List<SamplingUnit> importFile(File f, HashMap<Object, Object> options)
			throws Exception {

		List<SamplingUnit> units = new ArrayList<SamplingUnit>();
		ShapefileDataStore store = new ShapefileDataStore(f.toURI().toURL());
		
		SamplingUnit.SamplingUnitType type = (SamplingUnitType) options.get(TYPE_KEY);
		if (type == null){
			throw new Exception("Sampling unit type cannot be determined.");
		}
		
		Double bufferValue = (Double) options.get(BUFFER_KEY);
		
		
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
		try{
			SimpleFeature sf = reader.next();
			while(sf != null){
				String id = sf.getAttribute( (String)options.get(ID_FIELD_KEY) ).toString();
				
				
				sf = reader.next();
			}
		}finally{
			reader.close();
		}
		
		return units;
	}

}
