/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.data.inout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tools for importing asset data from CSV File
 * @author Emily
 *
 */
public class AssetLocationCsvImporter {

	private ConservationArea ca = SmartDB.getCurrentConservationArea();
	
	private Path file;
	private char delimiter;
	
	private Integer idField;
	private Integer stationIdField;
	private Integer xField;
	private Integer yField;
	private boolean skipFirst;
	private HashMap<AssetAttribute, Integer> attributeMappings;
	private DateTimeFormatter dateTimeFormat;
	
	private List<String> warnings = null;
	private Set<String> locationIds = null;
	private List<AssetStation> stations = null;
	private Projection projection;
	
	@Inject
	IEventBroker broker;
	
	
	public AssetLocationCsvImporter(Path file, char delimiter, boolean skipFirst, Integer idField, Integer stationField, Integer xField, Integer yField, HashMap<AssetAttribute, Integer> attributeMappings, String dateTimeFormat, Projection projection) {
		this.file = file;
		this.delimiter = delimiter;
		
		this.idField = idField;
		this.xField = xField;
		this.yField = yField;
		this.attributeMappings = attributeMappings;
		this.skipFirst = skipFirst;
		this.dateTimeFormat = DateTimeFormatter.ofPattern(dateTimeFormat);
		this.projection =  projection;
		this.stationIdField = stationField;
	}
	
	@SuppressWarnings("unchecked")
	public boolean processFile() throws Exception {
		warnings = new ArrayList<>();
		locationIds = new HashSet<>();
		
		stations = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			locationIds.addAll(session.createQuery("SELECT LOWER(id) FROM AssetStationLocation WHERE station.conservationArea = :ca").setParameter("ca", ca).list()); //$NON-NLS-1$ //$NON-NLS-2$
			
			stations.addAll(session.createQuery("FROM AssetStation WHERE conservationArea = :ca", AssetStation.class).setParameter("ca", ca).list()); //$NON-NLS-1$ //$NON-NLS-2$
			stations.forEach(s->s.getId());
		}
		
		CoordinateReferenceSystem fromCrs = ReprojectUtils.stringToCrs(projection.getDefinition());
		CoordinateReferenceSystem toCrs = GeometryUtils.SMART_CRS;
		MathTransform transform = CRS.findMathTransform(fromCrs, toCrs);
		
		//read and parse asset data
		List<AssetStationLocation> newLocations = new ArrayList<>();
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(file), delimiter)) {
			int row = 0;
			if (skipFirst) {
				reader.readNext();
				row ++;
			}
			
			String[] data = null;
			while((data = reader.readNext()) != null) {
				row ++;
				AssetStationLocation a = processRow(row, data, transform);
				if (a != null) newLocations.add(a);
			}
		}
		
		//process warnings from user
		if (!warnings.isEmpty()) {
			WarningDialog warn = new WarningDialog(Display.getDefault().getActiveShell(), Messages.AssetLocationCsvImporter_ImportingTitle, Messages.AssetLocationCsvImporter_WarningsMsg, warnings, 
					new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
			if (warn.open() != 0) {
				return false;
			}
		}
		if (newLocations.isEmpty()) {
			MessageDialog.openWarning(Display.getDefault().getActiveShell(),Messages.AssetLocationCsvImporter_ImportingTitle, Messages.AssetLocationCsvImporter_NoData);
			return true;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (AssetStationLocation a : newLocations) {
					session.save(a);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetLocationCsvImporter_8 + ex.getMessage(), ex);
				return false;
			}
		}
		
		MessageDialog.openInformation(Display.getDefault().getActiveShell(),  Messages.AssetLocationCsvImporter_SuccessMsg, MessageFormat.format(Messages.AssetLocationCsvImporter_10, newLocations.size()));
		broker.post(AssetEvents.ASSETSTATION_NEW, newLocations);
		
		return true;
	}
	
	
	private AssetStationLocation processRow(int row, String[] data, MathTransform transform) {
		String id = data[idField].trim();
		if (id.isEmpty()) {
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_IdRequired, row));
			return  null;
		}
		if (locationIds.contains(id.toLowerCase())){
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_IdDuplicated, row));
			return null;
		}
		
		AssetStation stn = null;
		String stationId = data[stationIdField].trim();
		for (AssetStation s : stations) {
			if (s.getId().equalsIgnoreCase(stationId)) {
				stn = s;
				break;
			}
		}
		if (stn == null) {
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_StationIdNotFound, stationId));
			return  null;
		}
		
		Double x = null;
		try {
			x = Double.parseDouble(data[xField]);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_xRequired, data[xField], row));
			return null;
		}
		
		Double y = null;
		try {
			y = Double.parseDouble(data[yField]);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_yRequired, data[yField], row));
			return null;
		}
		
		Point p = null;
		try {
			p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
			p = (Point) JTS.transform(p, transform);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_LocationParseError, row));
			return null;
		}
		
		AssetStationLocation newLocation = new AssetStationLocation();
		newLocation.setStation(stn);
		newLocation.setId(id);
		newLocation.setX(p.getX());
		newLocation.setY(p.getY());
		newLocation.setAttributeValues(new ArrayList<>());
		
		//parse attribute values
		for (AssetAttribute assetAttribute : attributeMappings.keySet()) {
			Integer columnIndex = attributeMappings.get(assetAttribute);
			if (columnIndex == null) continue; //this attribute is not mapped
			
			String value = data[columnIndex].trim();
			if (value.isEmpty()) continue; //not value 
			
			
			//we need to convert value to the correct type based on the attribute type
			AssetStationLocationAttributeValue attributeValue = new AssetStationLocationAttributeValue();
			attributeValue.setStationLocation(newLocation);
			attributeValue.setAttribute(assetAttribute);
			attributeValue = convertAttributeValue(attributeValue, value, row);
			if (attributeValue != null) {
				newLocation.getAttributeValues().add(attributeValue);
			}
		}
		
		locationIds.add(id.toLowerCase());
		return newLocation;
	}
	
	private AssetStationLocationAttributeValue convertAttributeValue(AssetStationLocationAttributeValue attributeValue, String data, int rowIndex) {
		data = data.trim();
		switch (attributeValue.getAttribute().getType()) {
		case BOOLEAN:
			Boolean x = null;
			try {
				x = Boolean.valueOf(data);
				
			}catch (Exception ex) {
				x = null;
			}
			if (x == null) {
				warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_BooleanParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(x ? 1.0 : 0);
			break;
			
		case DATE:
			Date d = null;
			try {
				d = Date.from(LocalDate.parse(data, dateTimeFormat).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
			}catch (Exception e) {
				d = null;
			}
			if (d == null) {
				warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_DateParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setDateValue(d);
			break;
		case LIST:
			AssetAttributeListItem av = null;
			for (AssetAttributeListItem i : attributeValue.getAttribute().getAttributeList()) {
				if (i.getKeyId().equalsIgnoreCase(data)) {
					av = i;
					break;
				}
				for (Label l : i.getNames()) {
					if (l.getValue().equalsIgnoreCase(data)) {
						av = i;
						break;
					}
				}
			}
			if (av == null) {
				warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_ListParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setAttributeListItem(av);
			break;
			
		case NUMERIC:
			Double v = null;
			try {
				v = Double.parseDouble(data);
			}catch (Exception ex) {
				v = null;
			}
			if (v == null) {
				warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_NumericParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(v);
			break;
		case POSITION:
			//TODO: add support for position attributes
			warnings.add(MessageFormat.format(Messages.AssetLocationCsvImporter_PositionAttributeNotSupported, rowIndex));
			return null;
		case TEXT:
			attributeValue.setStringValue(data);
			break;
		}
		return attributeValue;
	}
}
