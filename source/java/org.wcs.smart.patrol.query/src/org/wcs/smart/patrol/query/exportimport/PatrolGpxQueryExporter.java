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
package org.wcs.smart.patrol.query.exportimport;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.wcs.smart.gpx.xml.GpxType;
import org.wcs.smart.gpx.xml.ObjectFactory;
import org.wcs.smart.gpx.xml.TrkType;
import org.wcs.smart.gpx.xml.TrksegType;
import org.wcs.smart.gpx.xml.WptType;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.engine.test.WaypointQueryResultItem;
import org.wcs.smart.query.common.importexport.SimpleQueryExporter;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.SmartUtils;

/**
 * GPX Query exporter for patrols.
 *
 * @author elitvin
 * @since 5.0.0
 */
public class PatrolGpxQueryExporter extends SimpleQueryExporter implements IQueryExporter {
	
	private GpxType gpx;

	@Override
	public String getId() {
		return "org.wcs.smart.query.export.gpx"; //$NON-NLS-1$
	}

	@Override
	public boolean supportsProjection() {
		return false; //GPX files are always WGS84
	}

	@Override
	public String getName() {
		return Messages.PatrolGpxQueryExporter_GPX_ExporterName;
	}

	@Override
	public String getDefaultExtension() {
		return "gpx"; //$NON-NLS-1$
	}

	@Override
	public boolean canExport(Query query) {
		if (query.getTypeKey().equals(PatrolObservationQuery.KEY) ||
				query.getTypeKey().equals(PatrolWaypointQuery.KEY)||
				query.getTypeKey().equals(PatrolQuery.KEY)){
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void export(Query query, IQueryResult result, Path file, HashMap<String, Object> parameters, IProgressMonitor monitor) throws Exception {
		List<QueryColumn> columns = (((SimpleQuery)query).computeQueryColumns(Locale.getDefault(), null, null));
		
		//set data
		if (result instanceof IPagedQueryResultSet){
			super.setData((IPagedQueryResultSet)result, columns, file);
		}else if (result instanceof MemoryQueryResult){
			super.setData( ((MemoryQueryResult<IResultItem>)result).getData(), columns, file);
		}else if (result instanceof GridQueryResult){
			super.setData( ((GridQueryResult)result).getData(), columns, file);
		}
		//export
		super.export(monitor);		
	}

	@Override
	protected void init() throws Exception {
		gpx = new GpxType();
	}

	@Override
	protected void writeRow(IResultItem row) throws Exception {
		if (row instanceof IPatrolQueryResultItem) {
			recordWaypoint((IPatrolQueryResultItem)row);
		}
		if (row instanceof PatrolQueryResultItem) {
			recordTrack((PatrolQueryResultItem)row);
		}
	}


	@Override
	protected void finish() throws Exception {
		ObjectFactory gpxFactory = new ObjectFactory();
		JAXBContext context = JAXBContext.newInstance(GpxType.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(gpxFactory.createGpx(gpx), outputFile.toAbsolutePath().toFile());
	}

	private void recordTrack(PatrolQueryResultItem item) throws DatatypeConfigurationException {
		if (item.getTrack() != null) {
			Geometry geometry = item.asGeometry(PatrolQueryResultItem.TRACK_GEOMCOLUMN_KEY);
			if (geometry != null) {
				TrkType trk = new TrkType();
				MultiLineString mls = (MultiLineString) geometry;
				for (int i = 0; i < mls.getNumGeometries(); i++) {
					TrksegType trkseg = new TrksegType();
					LineString ls = (LineString) mls.getGeometryN(i);
					Coordinate[] coordinateArray = ls.getCoordinates();
					for (Coordinate coordinate : coordinateArray) {
						WptType wpt = new WptType();
						wpt.setLon(new BigDecimal(coordinate.x));
						wpt.setLat(new BigDecimal(coordinate.y));
						
						long timestamp = Double.valueOf(coordinate.getZ()).longValue();
						LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
						wpt.setTime(SmartUtils.toXmlDateTimeUTC(ldt));
						
						trkseg.getTrkpt().add(wpt);
					}
					trk.getTrkseg().add(trkseg);
				}
				gpx.getTrk().add(trk);
			}
		}
	}

	
	
	private void recordWaypoint(IPatrolQueryResultItem item) throws DatatypeConfigurationException {
		if (item instanceof WaypointQueryResultItem) {
			WaypointQueryResultItem it = (WaypointQueryResultItem)item;
			
			WptType w = new WptType();
			double x = it.getWaypointX(null);
			double y = it.getWaypointY(null);
			w.setLon(new BigDecimal(x));
			w.setLat(new BigDecimal(y));
			
			w.setTime(toXmlTime(it.getWaypointDateTime().toLocalDate(), it.getWaypointDateTime().toLocalTime()));
			w.setCmt(it.getWaypointComment());

			gpx.getWpt().add(w);
		}
	}

	private XMLGregorianCalendar toXmlTime(LocalDate date, LocalTime time) throws DatatypeConfigurationException {
		return SmartUtils.toXmlDateTime(date.atTime(time));
	}
	
}
