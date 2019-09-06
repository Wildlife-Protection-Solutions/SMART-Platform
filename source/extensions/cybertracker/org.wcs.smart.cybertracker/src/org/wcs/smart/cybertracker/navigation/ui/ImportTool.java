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
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
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
import org.locationtech.udig.project.ui.tool.AbstractActionTool;
import org.opengis.feature.simple.SimpleFeature;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.gpx.xml.GpxType;
import org.wcs.smart.gpx.xml.WptType;

/**
 * Import targets from shapefile or gpx file.
 * 
 * @author Emily
 *
 */
public class ImportTool  extends AbstractActionTool {

	public static final String ID = "org.wcs.smart.ui.map.navigation.import"; //$NON-NLS-1$

	
	@Override
	public void run() {
		
		Object x = super.getContext().getMap().getBlackboard().get(ITargetEditor.ID);
		if ( x == null || !(x instanceof ITargetEditor) ) return;
		ITargetEditor target = (ITargetEditor)x;
		
		String[] fnames = new String[] {null};
		Display.getDefault().syncExec(()->{
			FileDialog fd = new FileDialog(Display.getDefault().getActiveShell());
			fd.setText(Messages.ImportTool_ImportTitle);
			fd.setFilterExtensions(new String[] {"*.shp;*.gpx", "*.shp", "*.gpx"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fd.setFilterNames(new String[] {Messages.ImportTool_SupportedFiles, Messages.ImportTool_Shapefiles, Messages.ImportTool_GpxFiles});
			fnames[0] = fd.open();
			
			if (fnames[0] == null) return;
			
			Path importFile = Paths.get(fnames[0]);
			
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
							addGeometry(target, geom);
							
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
						target.addPointTarget(new Coordinate(cx,cy));
					}
				} catch (Exception ex) {
					CyberTrackerPlugIn.displayError(Messages.ImportTool_ErrorTitle, Messages.ImportTool_GpxErrorMsg + ex.getMessage(), ex);
				}
			}
		});
		
	}
	
	private void addGeometry(ITargetEditor target, Geometry geom) throws Exception{
		if (geom instanceof Point) {
			target.addPointTarget(geom.getCoordinate());	
		}else if (geom instanceof MultiPoint) {
			MultiPoint mp = (MultiPoint)geom;
			for (int i = 0; i < mp.getNumGeometries(); i ++) {
				Point pp = (Point) mp.getGeometryN(i);
				target.addPointTarget(pp.getCoordinate());
			}
		}else if (geom instanceof LineString) {
			target.addLinearTarget((LineString)geom);
		}else if (geom instanceof MultiLineString) {
			MultiLineString mp = (MultiLineString)geom;
			for (int i = 0; i < mp.getNumGeometries(); i ++) {
				LineString pp = (LineString) mp.getGeometryN(i);
				target.addLinearTarget(pp);
			}
		}else {
			throw new Exception(MessageFormat.format(Messages.ImportTool_GeomNotSupported, geom.getGeometryType()));
		}
	}

	@Override
	public void dispose() {
	}
}
