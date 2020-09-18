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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tools for importing asset data from CSV File
 * @author Emily
 *
 */
public class AssetStationCsvImporter {

	private ConservationArea ca ;
	
	private Path file;
	private char delimiter;
	
	private Integer idField;
	private Integer xField;
	private Integer yField;
	private Integer bufferField;
	private boolean skipFirst;
	private HashMap<AssetAttribute, Integer> attributeMappings;
	private DateTimeFormatter dateTimeFormat;
	
	private List<String> warnings = null;
	private Set<String> stationIds = null;
	private Projection projection;
	
	@Inject
	IEventBroker broker;
	
	
	public AssetStationCsvImporter(ConservationArea ca, Path file, char delimiter, boolean skipFirst, Integer idField, 
			Integer xField, Integer yField, Integer bufferField, 
			HashMap<AssetAttribute, Integer> attributeMappings, 
			DateTimeFormatter dateTimeFormat, Projection projection) {
		this.ca = ca;
		this.file = file;
		this.delimiter = delimiter;
		
		this.idField = idField;
		this.xField = xField;
		this.yField = yField;
		this.bufferField = bufferField;
		this.attributeMappings = attributeMappings;
		this.skipFirst = skipFirst;
		this.dateTimeFormat = dateTimeFormat;
		this.projection =  projection;
	}
	
	@SuppressWarnings("unchecked")
	public boolean processFile() throws Exception {
		warnings = new ArrayList<>();
		stationIds = new HashSet<>();
		
		try(Session session = HibernateManager.openSession()){
			stationIds.addAll(session.createQuery("SELECT LOWER(id) FROM AssetStation WHERE conservationArea = :ca").setParameter("ca", ca).list()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		CoordinateReferenceSystem fromCrs = ReprojectUtils.stringToCrs(projection.getDefinition());
		CoordinateReferenceSystem toCrs = GeometryUtils.SMART_CRS;
		MathTransform transform = CRS.findMathTransform(fromCrs, toCrs);
		
		//read and parse asset data
		List<AssetStation> newStations = new ArrayList<>();
		try(CSVReader reader = new CSVReader(Files.newBufferedReader(file), delimiter)) {
			int row = 0;
			if (skipFirst) {
				reader.readNext();
				row ++;
			}
			
			String[] data = null;
			while((data = reader.readNext()) != null) {
				row ++;
				AssetStation a = processRow(row, data, transform);
				if (a != null) newStations.add(a);
			}
		}
		
		//process warnings from user
		if (!warnings.isEmpty()) {
			int[] ret = new int[1];
			Display.getDefault().syncExec(()->{
				WarningDialog warn = new WarningDialog(Display.getDefault().getActiveShell(), Messages.AssetStationCsvImporter_ImportTitle, Messages.AssetStationCsvImporter_WarningsMsg, warnings, 
					new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
				ret[0] = warn.open();
			});
			if (ret[0] != 0) {
				return false;
			}
		}
		if (newStations.isEmpty()) {
			Display.getDefault().syncExec(()->{
				MessageDialog.openWarning(Display.getDefault().getActiveShell(),Messages.AssetStationCsvImporter_ImportTitle, Messages.AssetStationCsvImporter_NoDataMsg);
			});
			return true;
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (AssetStation a : newStations) {
					session.save(a);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.AssetStationCsvImporter_SaveError + ex.getMessage(), ex);
				return false;
			}
		}
		
		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(),  Messages.AssetStationCsvImporter_ImportTitle, MessageFormat.format(Messages.AssetStationCsvImporter_SuccessMsg, newStations.size()));
		});
		broker.post(AssetEvents.ASSETSTATION_NEW, newStations);
		
		return true;
	}
	
	
	private AssetStation processRow(int row, String[] data, MathTransform transform) {
		String id = data[idField].trim();
		if (id.isEmpty()) {
			warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_StationIdREquired, row));
			return  null;
		}
		if (stationIds.contains(id.toLowerCase())){
			warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_duplicateIds, row));
			return null;
		}
		
		Double x = null;
		try {
			x = Double.parseDouble(data[xField]);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_XNotFound, data[xField], row));
			return null;
		}
		
		Double y = null;
		try {
			y = Double.parseDouble(data[yField]);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_YNotFound, data[yField], row));
			return null;
		}
		
		Point p = null;
		try {
			p = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
			p = (Point) JTS.transform(p, transform);
		}catch (Exception ex) {
			warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_PositiongNotfound, row));
			return null;
		}
		
		Double buffer = AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE;
		if (bufferField != null) {
			try {
				buffer = Double.parseDouble(data[bufferField]);
				if (buffer <= 0) throw new Exception(Messages.AssetStationCsvImporter_InvalidBuffer);
			}catch (Exception ex) {
				buffer = AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE;
				warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_BufferValueNotParsed, data[bufferField], row, AssetModuleSettings.STATION_BUFFER_DEFAULT_VALUE));
			}
		}
		
		AssetStation newStation = new AssetStation();
		newStation.setConservationArea(ca);
		newStation.setBuffer(buffer);
		newStation.setId(id);
		newStation.setX(p.getX());
		newStation.setY(p.getY());
		newStation.setAttributeValues(new ArrayList<>());
		//parse attribute values
		for (AssetAttribute assetAttribute : attributeMappings.keySet()) {
			Integer columnIndex = attributeMappings.get(assetAttribute);
			if (columnIndex == null) continue; //this attribute is not mapped
			
			String value = data[columnIndex].trim();
			if (value.isEmpty()) continue; //not value 
			
			
			//we need to convert value to the correct type based on the attribute type
			AssetStationAttributeValue attributeValue = new AssetStationAttributeValue();
			attributeValue.setStation(newStation);
			attributeValue.setAttribute(assetAttribute);
			attributeValue = convertAttributeValue(attributeValue, value, row);
			if (attributeValue != null) {
				newStation.getAttributeValues().add(attributeValue);
			}
		}
		
		stationIds.add(id.toLowerCase());
		return newStation;
	}
	
	private AssetStationAttributeValue convertAttributeValue(AssetStationAttributeValue attributeValue, String data, int rowIndex) {
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
				warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_BooleanParseError, data, attributeValue.getAttribute().getName(), rowIndex));
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
				warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_DateParseError, data, attributeValue.getAttribute().getName(), rowIndex));
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
				warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_ListParseError, data, attributeValue.getAttribute().getName(), rowIndex));
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
				warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_NumericParseError, data, attributeValue.getAttribute().getName(), rowIndex));
				return null;
			}
			attributeValue.setNumberValue(v);
			break;
		case POSITION:
			//TODO: add support for position attributes
			warnings.add(MessageFormat.format(Messages.AssetStationCsvImporter_PositionAttributeNotSupported, rowIndex));
			return null;
		case TEXT:
			attributeValue.setStringValue(data);
			break;
		}
		return attributeValue;
	}
}
