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
package org.wcs.smart.qa.routine;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.routine.ILocationRoutineData.Type;

import com.vividsolutions.jts.algorithm.MCPointInRing;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * A QA routine that validates position of geometry objects.
 * 
 * Expected data providers must provide data objects that extend
 * the ILocationRoutineData interface.
 * 
 * 
 * @author Emily
 *
 */
public class LocationRoutineType implements IQaRoutineType {

	public static final String ID = "org.wcs.smart.qa.waypointlocation";  //$NON-NLS-1$
	
	public static final String LOCATION_PARAM_ID = LocationRoutineType.ID + ".parameter.area";  //$NON-NLS-1$
	
	public static enum GeometryType{
		FILE("file"), //$NON-NLS-1$
		AREA("area"), //$NON-NLS-1$
		WKT("text"); //$NON-NLS-1$
		
		public String key;
		
		GeometryType(String key){
			this.key =  key;
		}
	}
	public LocationRoutineType() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName(Locale l) {
		return "Location Routine";
	}

	@Override
	public String getDescription(Locale l) {
		return "Validates waypoints & track positions against a user provided areas flagging all waypoints outside of the boundaries.";
	}
	
	@Override
	public String getParameterSummary(QaRoutine routine){
		QaRoutineParameter p = routine.findParameter(LOCATION_PARAM_ID);
		if (p == null) return "";
		
		String value = p.getStringValue();
		if (value.startsWith(GeometryType.FILE.key)){
			WKBReader reader = new WKBReader();
			try {
				Geometry g = reader.read(p.getByteValue());
				StringBuilder sb = new StringBuilder();
				sb.append(value);
				sb.append(": "); //$NON-NLS-1$
				Envelope env = g.getEnvelopeInternal();
				sb.append("(" + env.getMinX() + "," + env.getMinY() + ")  (" + env.getMaxX() + "," + env.getMaxY() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				return sb.toString();
			} catch (ParseException e) {
				QaPlugIn.log(e.getMessage(), e);
			}
			return "Error";
			
		}else if (value.startsWith(GeometryType.AREA.key)){
			return value;
		}else if (value.startsWith(GeometryType.WKT.key)){
			String geom = new String(p.getByteValue());
			return value + ": " + geom; //$NON-NLS-1$
		}
		return p.getStringValue();
		
	}
	
	@Override
	public Collection<QaError> validateData(ValidationTask task, Session session, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(task.getRoutine().getName(), 103);
		monitor.subTask("Loading Data");
		
		Collection<?> data = task.getDataProvider().getData(session, task.getConservationArea(), task.getStartDate(), task.getEndDate());
		monitor.worked(1);
		
		QaRoutine routine = task.getRoutine();
		Geometry g = null;
		QaRoutineParameter p = routine.findParameter(LOCATION_PARAM_ID);
		
		
		if (p == null) throw new Exception("No valid geometry found for waypoint position routine: " + routine.getName());
		
		if (p.getStringValue().startsWith(GeometryType.WKT.key) || 
				p.getStringValue().startsWith(GeometryType.FILE.key)){
			WKBReader reader = new WKBReader();
			g = reader.read(p.getByteValue());
			
		}else if (p.getStringValue().startsWith(GeometryType.AREA.key)){
			Area.AreaType type = Area.AreaType.valueOf(p.getStringValue().split(":")[1]);
			if (type == null) throw new Exception("No valid geometry found for waypoint position routine: " + routine.getName());
			List<Area> areas = session.createCriteria(Area.class)
					.add(Restrictions.eq("conservationArea", task.getConservationArea()))
					.add(Restrictions.eq("type", type))
					.list();
			List<Geometry> geoms = new ArrayList<>();
			for (Area a : areas ){
				geoms.add(a.getGeometry());
			}
			if (geoms.isEmpty()) throw new Exception("No valid geometry found for waypoint position routine: " + routine.getName()); 
			g = new GeometryCollection(geoms.toArray(new Geometry[geoms.size()]), GeometryFactoryProvider.getFactory());
		}else{
			throw new Exception("No valid geometry found for waypoint position routine: " + routine.getName());
		}
		
		monitor.worked(2);
		
		monitor.subTask("Validating Data");
		
		int lastsize = 0;
		int cnt = 0;
		List<ProcessedPolygon> toCheck = preprocess(g);
		List<QaError> errors = new ArrayList<>();
		for (Object x : data){

			if (x instanceof ILocationRoutineData){
				ILocationRoutineData wp = (ILocationRoutineData)x;
				
				for(ProcessedPolygon pp : toCheck){
					if (wp.getType() == Type.POINT){
						if (!pp.isInside(wp.getPoint())){
							//TODO: this computes distances in degrees/min/sec
							double distance = Double.MAX_VALUE;
							for (ProcessedPolygon ppp : toCheck){
								double d = ppp.distance(wp.getPoint());
								if (d < distance){
									distance = d;
								}
							}
							
							QaError error = new QaError();
							error.setDataProviderId(task.getDataProvider().getId());
							error.setConservationArea(SmartDB.getCurrentConservationArea());
							error.setErrorDescription(MessageFormat.format("Point is {0} units outside of area.",distance));
							error.setErrorId( task.getDataProvider().getFeatureId(session, x));
							error.setSourceId( task.getDataProvider().getFeatureSource(session, x));
							error.setQaRoutine(routine);
							error.setStatus(QaError.Status.NEW);
							error.setValidateDate(new Date());
							errors.add(error);
							error.setGeometryObject(GeometryFactoryProvider.getFactory().createPoint(wp.getPoint()));
							
							break;
						}
					}else if (wp.getType() == Type.LINESTRING){
						
						if (!pp.polygon.contains(wp.getGeometry())){
							double distance = pp.polygon.distance(wp.getGeometry());
							QaError error = new QaError();
							error.setDataProviderId(task.getDataProvider().getId());
							error.setConservationArea(SmartDB.getCurrentConservationArea());
							error.setErrorDescription(MessageFormat.format("Linestring is {0} units outside of area.",distance));
							error.setErrorId( task.getDataProvider().getFeatureId(session, x));
							error.setSourceId( task.getDataProvider().getFeatureSource(session, x));
							error.setQaRoutine(routine);
							error.setStatus(QaError.Status.NEW);
							error.setValidateDate(new Date());
							errors.add(error);
							error.setGeometryObject(wp.getGeometry());
							
						}
					}
				}
			}
			cnt++;
			int work = (int)Math.round( (cnt / (double)data.size()) * 100 );
			monitor.worked(work - lastsize);
			lastsize = work;
		}
		monitor.done();
		return errors;
	}

	private List<ProcessedPolygon> preprocess(Geometry g){
		List<ProcessedPolygon> polygons = new ArrayList<>();
		if (g instanceof Polygon){
			polygons.add(new ProcessedPolygon((Polygon) g));
		}else if (g instanceof MultiPolygon){
			MultiPolygon mg = (MultiPolygon)g;
			for (int i = 0; i < mg.getNumGeometries(); i ++){
				polygons.add(new ProcessedPolygon((Polygon)mg.getGeometryN(i)));
			}
		}else if (g instanceof GeometryCollection){
			GeometryCollection gc = (GeometryCollection)g;
			for (int i = 0; i < gc.getNumGeometries(); i ++){
				Geometry p = gc.getGeometryN(i);
				if (p instanceof Polygon){
					polygons.add(new ProcessedPolygon((Polygon) p));
				}else if (p instanceof MultiPolygon){
					MultiPolygon mg = (MultiPolygon)p;
					for (int j = 0; j < mg.getNumGeometries(); j ++){
						polygons.add(new ProcessedPolygon((Polygon)mg.getGeometryN(j)));
					}
				}
			}
		}
		return polygons;
	}
	
	private class ProcessedPolygon{
		private MCPointInRing outer;
		private List<MCPointInRing> inner;
		private Polygon polygon;
		
		public ProcessedPolygon(Polygon p){
			this.polygon = p;
			outer = new MCPointInRing(GeometryFactoryProvider.getFactory().createLinearRing(p.getExteriorRing().getCoordinates()));
			inner = new ArrayList<>();
			for (int i = 0; i < p.getNumInteriorRing(); i ++){
				inner.add(new MCPointInRing(GeometryFactoryProvider.getFactory().createLinearRing(p.getInteriorRingN(i).getCoordinates())));
			}
		}
		
		public boolean isInside(Coordinate c){
			if (outer.isInside(c)){
				for (MCPointInRing i : inner){
					if (i.isInside(c)) return false;
				}
				return true;
			}
			return false;
		}
		public double distance(Coordinate c){
			return polygon.distance(GeometryFactoryProvider.getFactory().createPoint(c));
		}
	}
}
