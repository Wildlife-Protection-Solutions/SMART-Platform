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

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.ILabelProvider;
import org.wcs.smart.qa.ILabelProvider.Key;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.routine.ILocationRoutineData.Type;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.algorithm.MCPointInRing;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.operation.distance.DistanceOp;

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

	/**
	 * Routine ID
	 */
	public static final String ID = "org.wcs.smart.qa.waypointlocation";  //$NON-NLS-1$
	
	/**
	 * Area parameter id
	 */
	public static final String LOCATION_PARAM_ID = LocationRoutineType.ID + ".parameter.area";  //$NON-NLS-1$
	
	/**
	 * Seperator character for string values 
	 */
	public static final String SEPERATOR_CHAR = ":"; //$NON-NLS-1$
	
	private static final DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("#.###"); //$NON-NLS-1$
	
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
		return ILabelProvider.getLabel(Key.LocationRoutineType_Name, l);
	}

	@Override
	public String getDescription(Locale l) {
		return ILabelProvider.getLabel(Key.LocationRoutineType_Description, l);
	}
	
	@Override
	public String getParameterSummary(QaRoutine routine, Locale l, Session session){
		QaRoutineParameter p = routine.findParameter(LOCATION_PARAM_ID);
		if (p == null) return ""; //$NON-NLS-1$
		
		String value = p.getStringValue();
		if (value.startsWith(GeometryType.FILE.key)){
			WKBReader reader = new WKBReader();
			try {
				Geometry g = reader.read(p.getByteValue());
				StringBuilder sb = new StringBuilder();
				sb.append(value);
				sb.append(": "); //$NON-NLS-1$
				Envelope env = g.getEnvelopeInternal();
				sb.append(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_FileParamDescription, l), env.getMinX(),env.getMinY(), env.getMaxX(), env.getMaxY())); 
				return sb.toString();
			} catch (ParseException e) {
				Logger.getLogger(LocationRoutineType.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
			return ILabelProvider.getLabel(Key.LocationRoutineType_Error, l);
			
		}else if (value.startsWith(GeometryType.AREA.key)){
			String[] parts = value.split(LocationRoutineType.SEPERATOR_CHAR);
			String areaKey = parts[1];
			for (AreaType t : Area.AreaType.values()){
				if (areaKey.equals(t.name())){
					return MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_AreaParamDescription, l), SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(t, l));
				}
			}
			return value;
		}else if (value.startsWith(GeometryType.WKT.key)){
			String geom = new String(p.getByteValue());
			return MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_WktParamDescription, l), geom);
		}
		return p.getStringValue();
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<QaError> validateData(ValidationTask task, Session session, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(task.getRoutine().getName(), 103);
		monitor.subTask(ILabelProvider.getLabel(Key.LocationRoutineType_LoadingDataMsg, task.getLocale()));
		
		Collection<?> data = task.getDataProvider().getData(session, task.getConservationArea(), task.getStartDate(), task.getEndDate());
		monitor.worked(1);
		
		QaRoutine routine = task.getRoutine();
		Geometry g = null;
		QaRoutineParameter p = routine.findParameter(LOCATION_PARAM_ID);
		
		
		if (p == null) throw new Exception(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_NoGeomFound, task.getLocale()), routine.getName()));
		
		if (p.getStringValue().startsWith(GeometryType.WKT.key) || 
				p.getStringValue().startsWith(GeometryType.FILE.key)){
			WKBReader reader = new WKBReader();
			g = reader.read(p.getByteValue());
			
		}else if (p.getStringValue().startsWith(GeometryType.AREA.key)){
			Area.AreaType type = Area.AreaType.valueOf(p.getStringValue().split(SEPERATOR_CHAR)[1]);
			if (type == null) throw new Exception(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_NoGeomFound, task.getLocale()), routine.getName()));
			List<Area> areas = session.createCriteria(Area.class)
					.add(Restrictions.eq("conservationArea", task.getConservationArea())) //$NON-NLS-1$
					.add(Restrictions.eq("type", type)) //$NON-NLS-1$
					.list();
			List<Geometry> geoms = new ArrayList<>();
			for (Area a : areas ){
				geoms.add(a.getGeometry());
			}
			if (geoms.isEmpty()) throw new Exception(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_NoGeomFound, task.getLocale()), routine.getName())); 
			g = new GeometryCollection(geoms.toArray(new Geometry[geoms.size()]), GeometryFactoryProvider.getFactory());
		}else{
			throw new Exception(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_NoGeomFound, task.getLocale()), routine.getName()));
		}
		
		monitor.worked(2);
		monitor.subTask(ILabelProvider.getLabel(Key.LocationRoutineType_ValidatingDataTaskName, task.getLocale()));
		
		int lastsize = 0;
		int cnt = 0;
		
		List<ProcessedPolygon> toCheck = null;
		Geometry mergedPolygon = null;
		List<QaError> errors = new ArrayList<>();
		
		for (Object x : data){
			if (x instanceof ILocationRoutineData){
				ILocationRoutineData wp = (ILocationRoutineData)x;

				if (wp.getType() == Type.POINT){
					if (toCheck == null) toCheck = preprocess(g);
					boolean contains = false;
					for(ProcessedPolygon pp : toCheck){
						if (pp.isInside(wp.getPoint())){
							contains = true;
						}
					}
					if (!contains){
						QaError error = new QaError();
						error.setDataProviderId(task.getDataProvider().getId());
						error.setConservationArea(task.getConservationArea());
						error.setErrorDescription(ILabelProvider.getLabel(Key.LocationRoutineType_WpOutsideArea, task.getLocale()));
						error.setErrorId( task.getDataProvider().getFeatureId(session, x, task.getLocale()));
						error.setSourceId( task.getDataProvider().getFeatureSource(session, x));
						error.setQaRoutine(routine);
						error.setStatus(QaError.Status.NEW);
						error.setValidateDate(new Date());
						errors.add(error);
						error.setGeometryObject(GeometryFactoryProvider.getFactory().createPoint(wp.getPoint()));
						
						try{
							Coordinate[] minPnts = DistanceOp.nearestPoints(g, GeometryFactoryProvider.getFactory().createPoint(wp.getPoint()));
							double distancekm = JTS.orthodromicDistance(minPnts[0], minPnts[1], GeometryUtils.SMART_CRS) / 1000.0;									
							error.setErrorDescription(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_WpOutsideArea2, task.getLocale()),DISTANCE_FORMATTER.format(distancekm)));
						}catch (Exception ex){
							ex.printStackTrace();
							error.setErrorDescription(ILabelProvider.getLabel(Key.LocationRoutineType_WpOutsideArea, task.getLocale()));
						}
					}
						
				}else if (wp.getType() == Type.LINESTRING){
					if (mergedPolygon == null)  mergedPolygon = g.union();
					if (!mergedPolygon.contains(wp.getGeometry())){
						double distance = g.distance(wp.getGeometry());
						QaError error = new QaError();
						error.setDataProviderId(task.getDataProvider().getId());
						error.setConservationArea(task.getConservationArea());
						if (distance == 0){
							error.setErrorDescription(ILabelProvider.getLabel(Key.LocationRoutineType_TrackOutsideArea, task.getLocale()));
						}else{
							try{
								Coordinate[] minPnts = DistanceOp.nearestPoints(g, wp.getGeometry());
								double distancekm = JTS.orthodromicDistance(minPnts[0], minPnts[1], GeometryUtils.SMART_CRS) / 1000.0;									
								error.setErrorDescription(MessageFormat.format(ILabelProvider.getLabel(Key.LocationRoutineType_TrackOutsideArea2, task.getLocale()), DISTANCE_FORMATTER.format(distancekm)));
							}catch (Exception ex){
								ex.printStackTrace();
								error.setErrorDescription(ILabelProvider.getLabel(Key.LocationRoutineType_TrackOutsideArea, task.getLocale()));
							}
						}
						error.setErrorId( task.getDataProvider().getFeatureId(session, x, task.getLocale()));
						error.setSourceId( task.getDataProvider().getFeatureSource(session, x));
						error.setQaRoutine(routine);
						error.setStatus(QaError.Status.NEW);
						error.setValidateDate(new Date());
						errors.add(error);
						error.setGeometryObject(wp.getGeometry());
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
		
		public ProcessedPolygon(Polygon p){
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
	}
}
