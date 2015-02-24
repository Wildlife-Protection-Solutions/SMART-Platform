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
package org.wcs.smart.er.ui.samplingunit.export;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitDataSource;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Shapefile sampling unit importer.
 * 
 * @author Emily
 *
 */
public class ShpSamplingUnitExporter implements ISamplingUnitExporter{

	@Override
	public String getFileExtension(){
		return "shp"; //$NON-NLS-1$
	}
	
	@Override
	public void exportFile(File f, SurveyDesign sd, Session session,
			HashMap<Object, Object> options, IProgressMonitor monitor) throws Exception {

		monitor.beginTask(Messages.ShpSamplingUnitExporter_Progress1, 1);
		
		
		GeometryType type = (GeometryType) options.get(SU_TYPE_KEY);
		if (type == null){
			throw new Exception(Messages.ShpSamplingUnitExporter_SuTypeError);
		}
		
		Set<GeometryType> types = SurveyHibernateManager.getInstance().getSamplingUnitTypes(sd, session);
		if (!types.contains(type)){
			//nothing to export
			return;
		}
		
		String typeName = type.name();
		
		URL shpFileURL = URLUtils.fileToURL(f);
		
		FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
        Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, shpFileURL);
		
		DataStore shapefile = factory.createNewDataStore(params);
		
        
        SamplingUnitDataSource dataSource = new SamplingUnitDataSource(sd);
        SimpleFeatureSource fs = dataSource.getFeatureSource(typeName);
        shapefile.createSchema(dataSource.getSchema(typeName));
		FeatureStore<SimpleFeatureType, SimpleFeature> nfs = 
				(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
		nfs.addFeatures( DataUtilities.collection(fs.getFeatures()) );
		
		shapefile.dispose();
		
		monitor.worked(1);
		monitor.done();
		
	}


}
