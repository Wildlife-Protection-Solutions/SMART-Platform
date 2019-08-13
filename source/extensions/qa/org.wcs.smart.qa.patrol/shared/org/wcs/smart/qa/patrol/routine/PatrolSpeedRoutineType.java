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
package org.wcs.smart.qa.patrol.routine;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.referencing.GeodeticCalculator;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.patrol.ILabelProvider;
import org.wcs.smart.qa.patrol.ILabelProvider.Key;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.qa.routine.WaypointLocationData;

/**
 * A QA routine that validates speed for track and waypoints.
 * 
 * Expected data providers must provide data objects that extend
 * the ILocationRoutineData interface.
 * 
 * 
 * @author Emily
 *
 */
public class PatrolSpeedRoutineType implements IQaRoutineType {

	public static final String ID = "org.wcs.smart.qa.patrol.speed";  //$NON-NLS-1$
	
	public static final String MAX_SPEED_PARAM_ID = PatrolSpeedRoutineType.ID + ".parameter.speed";  //$NON-NLS-1$
	public static final String PATROL_TYPES_PARAM_ID = PatrolSpeedRoutineType.ID + ".parameter.type";  //$NON-NLS-1$
	public static final String PARAM_SEP = ","; //$NON-NLS-1$
	
	private static final DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("#.##"); //$NON-NLS-1$
	
	public PatrolSpeedRoutineType() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName(Locale l) {
		return ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_Name, l);
	}

	@Override
	public String getDescription(Locale l) {
		return ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_Desc, l);
	}
	
	@Override
	public String getParameterSummary(QaRoutine routine, Locale l, Session session){
		QaRoutineParameter speedParameter = routine.findParameter(MAX_SPEED_PARAM_ID);
		QaRoutineParameter typeParameter = routine.findParameter(PATROL_TYPES_PARAM_ID);
		
		String[] patrolTypes = typeParameter.getStringValue().split(PARAM_SEP);
		
		StringBuilder sb = new StringBuilder();
		sb.append(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_Param_MaxSpeedName, l));
		sb.append(speedParameter.getStringValue());
		sb.append(" "); //$NON-NLS-1$
		sb.append(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_Param_SpeedUnits, l));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_Param_TypeName, l));
		for (int i = 0; i < patrolTypes.length; i ++){
			if (i != 0) sb.append(", "); //$NON-NLS-1$
			
			PatrolTransportType ttype = QueryFactory.buildQuery(session, PatrolTransportType.class,
					new Object[] {"conservationArea", routine.getConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId", patrolTypes[i]}).uniqueResult(); //$NON-NLS-1$
			if (ttype == null){
				sb.append(patrolTypes[i]);
			}else{
				sb.append(ttype.getName());
			}	
		}
		return sb.toString();
	}
	
	@Override
	public Collection<QaError> validateData(ValidationTask task, Session session, IProgressMonitor monitor) throws Exception{
		monitor.beginTask(task.getRoutine().getName(), 103);
		monitor.subTask(ILabelProvider.getLabel(Key.LoadingString, task.getLocale()));
		
		Collection<?> data = task.getDataProvider().getData(session, task.getConservationArea(), task.getStartDate(), task.getEndDate());
		monitor.worked(1);
		
		QaRoutine routine = task.getRoutine();
		
		QaRoutineParameter speedParameter = routine.findParameter(MAX_SPEED_PARAM_ID);
		Double maxSpeed = 0.0;
		try{
			maxSpeed = Double.parseDouble(speedParameter.getStringValue());
		}catch (Exception ex){
			throw new Exception(MessageFormat.format(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_InvalidMaxSpeed, task.getLocale()), speedParameter.getStringValue()), ex);
		}
		
		QaRoutineParameter typeParameter = routine.findParameter(PATROL_TYPES_PARAM_ID);
		Set<PatrolTransportType> types = new HashSet<>();
		String[] keys = typeParameter.getStringValue().split(PARAM_SEP);
		for (String key : keys){
			PatrolTransportType type = QueryFactory.buildQuery(session, PatrolTransportType.class,
					new Object[] {"conservationArea", task.getConservationArea()}, //$NON-NLS-1$
					new Object[] {"keyId",key}).uniqueResult(); //$NON-NLS-1$
			if (type != null){
				types.add(type);
			}
		}
		
		List<QaError> errors = new ArrayList<>();
		int cnt = 0;
		int lastsize = 0;
		for (Object x : data){
			if (x instanceof TrackLocationData){
				QaError error = validateTrack((TrackLocationData)x, task, maxSpeed, types, session);
				if (error != null) errors.add(error);
			}else if (x instanceof WaypointLocationData){
				QaError error = validateWaypoint((WaypointLocationData)x, task, maxSpeed, types, session);
				if (error != null) errors.add(error);
			}
			cnt++;
			int work = (int)Math.round( (cnt / (double)data.size()) * 100 );
			monitor.worked(work - lastsize);
			lastsize = work;
				
		}
		monitor.done();
		
		return errors;
	}
	
	private QaError validateWaypoint(WaypointLocationData wp, ValidationTask task, double maxSpeed, Set<PatrolTransportType> types, Session session){
		PatrolWaypoint pw = QueryFactory.buildQuery(session, PatrolWaypoint.class, "id.waypoint", wp.getWaypoint()).uniqueResult(); //$NON-NLS-1$
		if (pw == null) return null;
		if (!types.contains(pw.getPatrolLegDay().getPatrolLeg().getType())) return null;
				
		List<PatrolWaypoint> allWaypoints = pw.getPatrolLegDay().getWaypoints();
		Collections.sort(allWaypoints, (a,b)->{
			if (a.getWaypoint().getDateTime().equals(b.getWaypoint().getDateTime())){
				return Integer.compare(a.getWaypoint().getId(), b.getWaypoint().getId());
			}
			return a.getWaypoint().getDateTime().compareTo(b.getWaypoint().getDateTime());
		});
		int index = allWaypoints.indexOf(pw);
		if (index <= 0) return null;
		Waypoint previous = allWaypoints.get(index-1).getWaypoint();
		
		double speed = computeSpeed(new Coordinate(previous.getRawX(), previous.getRawY(), previous.getDateTime().getTime()),  new Coordinate(pw.getWaypoint().getRawX(), pw.getWaypoint().getRawY(), pw.getWaypoint().getDateTime().getTime()), maxSpeed);
		if  (speed > maxSpeed){
			String message = MessageFormat.format(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_WpSpeedExceeded, task.getLocale()), DISTANCE_FORMATTER.format(speed), maxSpeed);
			return createError(task, session, wp, GeometryFactoryProvider.getFactory().createPoint(new Coordinate(pw.getWaypoint().getRawX(), pw.getWaypoint().getRawY())), message);
		}
		return null;
	}
	
	
	
	private QaError validateTrack(TrackLocationData x, ValidationTask task, double maxSpeed, Set<PatrolTransportType> types, Session session){
		Track track = ((TrackLocationData) x).getTrack();
		if (!types.contains(track.getPatrolLegDay().getPatrolLeg().getType())) return null;
				
		//go through the track computing the speed between any two points
		try{
			for (LineString ls : track.getLineStrings()) {
				Coordinate[] c = ls.getCoordinates();	
				for (int i = 1; i < c.length; i ++){
					Coordinate previous = c[i-1];
					Coordinate current = c[i];
					double speed = computeSpeed(previous,  current, maxSpeed);
					if  (speed > maxSpeed){
						String message = MessageFormat.format(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_TrackSpeedExceeded, task.getLocale()), DISTANCE_FORMATTER.format(speed), maxSpeed, current.x, current.y);
						return createError(task, session, x, ls, message);
					}
				}
			}
		}catch (Exception ex){
			Logger.getLogger(PatrolSpeedRoutineType.class.getName()).log(Level.WARNING,ex.getMessage(), ex);
			String message = MessageFormat.format(ILabelProvider.getLabel(Key.PatrolSpeedRoutineType_TrackError, task.getLocale()), ex.getMessage());
			return createError(task, session, x, null, message);
		}
		return null;
		
	}
	
	private double computeSpeed(Coordinate last, Coordinate current, double maxSpeed){
		
		GeodeticCalculator cal = new GeodeticCalculator();
		cal.setStartingGeographicPoint(last.x, last.y);
		cal.setDestinationGeographicPoint(current.x, current.y);
				
		double distance = cal.getOrthodromicDistance(); //meters
		double time = current.getZ() - last.getZ(); //milliseconds
				
		double speed = distance * 3600 / time ;
		return speed;
		
	}
	
	private QaError createError(ValidationTask task, Session session, Object data, Geometry geometry, String message){
		if (message == null) return null;
		QaError error = new QaError();
		error.setConservationArea(task.getConservationArea());
		error.setDataProviderId(task.getDataProvider().getId());
		error.setErrorDescription(message);
		error.setErrorId(task.getDataProvider().getFeatureId(session, data, task.getLocale()));
		error.setGeometryObject(geometry);
		error.setQaRoutine(task.getRoutine());
		error.setSourceId(task.getDataProvider().getFeatureSource(session, data));
		error.setStatus(QaError.Status.NEW);
		error.setValidateDate(new Date());
		return error;
		
	}

}
