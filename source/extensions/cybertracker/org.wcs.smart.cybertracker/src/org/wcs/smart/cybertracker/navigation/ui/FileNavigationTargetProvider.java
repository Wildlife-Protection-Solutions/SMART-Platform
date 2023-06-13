/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.gpx.xml.GpxType;
import org.wcs.smart.gpx.xml.WptType;
import org.wcs.smart.map.GeometryFactoryProvider;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;

/**
 * File navigation layer target provider for reading shapefiles and gpx files
 * and converting features into navigation layer targets.
 * 
 * @author Emily
 *
 */
public class FileNavigationTargetProvider implements INavigationLayerTargetProvider {

	FileSelectionWizardPage page = null;
	
	@Override
	public String getTypeName() {
		return Messages.FileNavigationTargetProvider_ProviderName;
	}

	@Override
	public Image getImage() {
		return CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_FILE32);
	}
	
	@Override
	public List<WizardPage> getPages() {
		if (page == null) {
			page = new FileSelectionWizardPage();
		}
		return Collections.singletonList(page);
	}

	@Override
	public boolean canFinish() {
		return page.isPageComplete();
	}
	
	@Override
	public List<NavigationTarget> getTargets(IProgressMonitor monitor) {
		String fname = page.getFile();
		Path importFile = Paths.get(fname);
		
		List<NavigationTarget> targets = new ArrayList<>();
		
		if (importFile.getFileName().toString().endsWith(".shp")) { //$NON-NLS-1$
			//read shapefile
			try {
				Map<String,Object> mps = new HashMap<>();
				mps.put("url", importFile.toUri().toURL() ); //$NON-NLS-1$
				DataStore ds = DataStoreFinder.getDataStore(mps);
				SimpleFeatureSource fsource = ds.getFeatureSource(ds.getTypeNames()[0]);
				SimpleFeatureCollection fcollection = fsource.getFeatures();
				try(SimpleFeatureIterator it = fcollection.features()){
					while(it.hasNext()) {
						SimpleFeature f = it.next();
						Geometry geom = (Geometry) f.getDefaultGeometry();
						targets.addAll(getTargets(geom));
					}
				}
			}catch (Exception ex) {
				CyberTrackerPlugIn.displayError(Messages.ImportTool_ErrorTitle, Messages.ImportTool_ShpErrorMsg + ex.getMessage(), ex);
			}
			
		}else if (importFile.getFileName().toString().endsWith(".gpx")) { //$NON-NLS-1$
			//read gpx
			try {
				JAXBContext context = JAXBContext.newInstance(GPSDataImport.GPX_METADATA_CLASSES);
				Unmarshaller un = context.createUnmarshaller();
				Object o = un.unmarshal(importFile.toFile());
				GpxType type = (GpxType) ((JAXBElement<?>) o).getValue();
				
				for (WptType wp : type.getWpt()) {
					double cx = wp.getLon().doubleValue();
					double cy = wp.getLat().doubleValue();
					targets.add(new NavigationTarget(null, GeometryFactoryProvider.getFactory().createPoint(new Coordinate(cx,cy))));
				}
			} catch (Exception ex) {
				CyberTrackerPlugIn.displayError(Messages.ImportTool_ErrorTitle, Messages.ImportTool_GpxErrorMsg + ex.getMessage(), ex);
			}
		}
		return targets;
	}
	
	private List<NavigationTarget> getTargets(Geometry geom) throws Exception{
		List<NavigationTarget> targets = new ArrayList<>();
		
		if (geom instanceof Point) {
			targets.add(new NavigationTarget(null, (Point)geom));
		}else if (geom instanceof MultiPoint) {
			MultiPoint mp = (MultiPoint)geom;
			for (int i = 0; i < mp.getNumGeometries(); i ++) {
				Point pp = (Point) mp.getGeometryN(i);
				targets.add(new NavigationTarget(null, pp));
			}
		}else if (geom instanceof LineString) {
			targets.add(new NavigationTarget(null, (LineString)geom));
		}else if (geom instanceof MultiLineString) {
			MultiLineString mp = (MultiLineString)geom;
			for (int i = 0; i < mp.getNumGeometries(); i ++) {
				LineString pp = (LineString) mp.getGeometryN(i);
				targets.add(new NavigationTarget(null, (LineString)pp));
			}
		}else {
			throw new Exception(MessageFormat.format(Messages.ImportTool_GeomNotSupported, geom.getGeometryType()));
		}
		return targets;
	}

}
