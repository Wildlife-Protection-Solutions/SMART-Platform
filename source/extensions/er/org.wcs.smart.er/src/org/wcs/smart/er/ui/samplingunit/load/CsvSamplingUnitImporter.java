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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * CSV File importer sampling unit.
 * 
 * @author Emily
 *
 */
public class CsvSamplingUnitImporter extends ISamplingUnitImporter {

	public static final String DELIMETER_KEY = "DELIMITER"; //$NON-NLS-1$

	/**
	 * Reads the file names from the csv file.
	 */
	@Override
	public String[] getFieldNames(File f, Map<String, Object> options) throws Exception {
		Character delim = (Character) options.get(DELIMETER_KEY);
		String[] headers = new String[0];
		
		try(CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8), delim.charValue())){ 
			//read the first line
			headers = reader.readNext();
		}
		return headers;
	}

	/**
	 * Imports sampling units from csv file.
	 * 
	 * <p>Required Options: </p>
	 * <ul>
	 * <li>TYPE_KEY - the SamplingUnitType</li>
	 * <li>PROJECTION_KEY - the projection of the source data</li>
	 * <li>ID_FIELD_KEY - the id field </li>
	 * <li>X1_FIELD_KEY - the x1 field </li>
	 * <li>Y1_FIELD_KEY - the y1 field </li>
	 * <li>X2_FIELD_KEY - the x2 field (for lines)</li>
	 * <li>Y2_FIELD_KEY - the y2 field (for lines)</li>
	 * <li>DELIMETER_KEY - the field delimiter</li>
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<SamplingUnit> importFile(File f, HashMap<Object, Object> options, IProgressMonitor monitor) throws Exception {
		
		List<SamplingUnit> units = new ArrayList<SamplingUnit>();
		
		//get required options
		SamplingUnit.GeometryType type = (GeometryType) options.get(TYPE_KEY);
		if (type == null){
			throw new Exception(Messages.CsvSamplingUnitImporter_InvalidType);
		}
		
		Projection proj = (Projection)options.get(PROJECTION_KEY);
		String idField = (String)options.get(ID_FIELD_KEY);
		String x1Field = (String)options.get(X1_FIELD_KEY);
		String y1Field = (String)options.get(Y1_FIELD_KEY);
		String x2Field = (String)options.get(X2_FIELD_KEY);
		String y2Field = (String)options.get(Y2_FIELD_KEY);
		Character delim = (Character) options.get(DELIMETER_KEY);
		CoordinateReferenceSystem crs = ReprojectUtils.stringToCrs(proj.getDefinition());
		
		HashSet<String> existingIds = (HashSet<String>) options.get(EXISTING_IDS_KEY);
		
		//read file - getting cnt for progress
		
		int fileCnt = 0;
		try(CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8), delim.charValue())){ 
			while(reader.readNext() != null){
				fileCnt++;
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		//read file 
		monitor.beginTask(MessageFormat.format(Messages.CsvSamplingUnitImporter_Progress1, new Object[]{f.getAbsoluteFile()}), fileCnt);
		
		try(CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8), delim.charValue())){ 
			//read the first line
			String[] headers = reader.readNext();
			monitor.worked(1);
			
			HashMap<String, Integer> fieldToColumn = new HashMap<String, Integer>();
			for (int i = 0; i < headers.length; i ++){
				fieldToColumn.put(headers[i], i);
			}
			
			List<SamplingUnitAttribute> attributes = new ArrayList<SamplingUnitAttribute>();
			
			HashMap<Object, Integer> valueToColumn = new HashMap<Object, Integer>();
			for (Object x : options.keySet()){
				if (x instanceof SamplingUnitAttribute){
					attributes.add((SamplingUnitAttribute)x);
					String fieldName = (String) options.get(x);
					Integer i = fieldToColumn.get(fieldName);
					valueToColumn.put(x, i);
				}
			}
			
			Integer idColumn = fieldToColumn.get(idField);
			Integer x1 = fieldToColumn.get(x1Field);
			Integer y1 = fieldToColumn.get(y1Field);
			Integer x2 = fieldToColumn.get(x2Field);
			Integer y2 = fieldToColumn.get(y2Field);
			
			GeometryFactory gf = GeometryFactoryProvider.getFactory();
			int cnt = 0;
			while(true){
				headers = reader.readNext();
				monitor.worked(1);
				cnt++;
				if (headers == null){
					break;
				}
				if (headers.length == 1 && headers[0].length() == 0){
					//blank line; skip
					continue;
				}
				
				SamplingUnit su = new SamplingUnit();
				su.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
				
				if (type == GeometryType.PLOT){
					//point
					Double x = Double.parseDouble(headers[x1]);
					Double y = Double.parseDouble(headers[y1]);
					
					Coordinate out = new Coordinate(x,y);
					if (!CRS.equalsIgnoreMetadata(crs,GeometryUtils.SMART_CRS)){
						out = ReprojectUtils.reproject(out.x, out.y, crs, GeometryUtils.SMART_CRS);
					}
					su.setGeometry(gf.createPoint(out));
				}else{
					//line
					Double dx1 = Double.parseDouble(headers[x1]);
					Double dy1 = Double.parseDouble(headers[y1]);
					
					Double dx2 = Double.parseDouble(headers[x2]);
					Double dy2 = Double.parseDouble(headers[y2]);
					
					Coordinate p1 = new Coordinate(dx1,dy1);
					Coordinate p2 = new Coordinate(dx2,dy2);
					if (!crs.equals(GeometryUtils.SMART_CRS)){
						p1 = ReprojectUtils.reproject(p1.x, p1.y, crs, GeometryUtils.SMART_CRS);
						p2 = ReprojectUtils.reproject(p2.x, p2.y, crs, GeometryUtils.SMART_CRS);
					}
					su.setGeometry(gf.createLineString(new Coordinate[]{p1,p2}));
				}

				String id = null;
				if (idColumn != null){
					id = headers[idColumn];
				}
				id = generateId(id, cnt, existingIds, warnings);
				
				su.setId(id);
				su.setState(SamplingUnit.State.ACTIVE);
				su.setType(type);

				//attributes
				for (SamplingUnitAttribute att : attributes){
					SamplingUnitAttributeValue suv = new SamplingUnitAttributeValue();
					suv.setSamplingUnit(su);
					suv.setSamplingUnitAttribute(att);
					
					Integer col = valueToColumn.get(att);
					if (col == null) continue;
					
					String value = headers[col];

					boolean add = false;
					if (att.getType() == AttributeType.TEXT){
						if (value.length() > 0){
							String error = super.validateStringAttributeValue(value, att);
							if (error != null){
								warnings.add(error);
							}else{
								suv.setStringValue(value);
								add = true;
							}
						}
					}else if (att.getType() == AttributeType.NUMERIC){						
						if (value.trim().length() > 0){
							try{
								suv.setNumberValue(  Double.valueOf(value) );
								add = true;
							}catch (Exception ex){
								warnings.add(MessageFormat.format(Messages.CsvSamplingUnitImporter_InvalidDoubleValue, new Object[]{value, att.getName()}));
							}
						}
					}else if (att.getType() == AttributeType.LIST){
						SamplingUnitAttributeListItem listValue = findMatch(att, value);
						if (listValue != null){
							add = true;
							suv.setAttributeListItem(listValue);
						}else{
							warnings.add(getSamplingUnitListItemNotFoundError(value, att));
						}
					}else{
						throw new Exception(MessageFormat.format(Messages.CsvSamplingUnitImporter_InvalidAttributeType, new Object[]{att.getType()}));
					}
					if (add){
						su.getAttributes().add(suv);
					}
					
				}
				units.add(su);
			}
		}finally{
			monitor.done();
		}
		
		super.showWarnings(warnings);
		return units;
	}

}
