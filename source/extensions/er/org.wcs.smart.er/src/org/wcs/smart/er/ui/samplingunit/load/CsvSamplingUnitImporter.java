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
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import au.com.bytecode.opencsv.CSVReader;

/**
 * CSV File importer sampling unit.
 * 
 * @author Emily
 *
 */
public class CsvSamplingUnitImporter implements ISamplingUnitImporter {

	public static final String DELIMETER_KEY = "delimiter";

	@Override
	public String[] getFieldNames(File f, Map<String, Object> options) throws Exception {
		Character delim = (Character) options.get(DELIMETER_KEY);
		String[] headers = new String[0];
		CSVReader reader = new CSVReader(new FileReader(f), delim.charValue());
		try{
			//read the first line
			headers = reader.readNext();
		}finally{
			reader.close();
		}
		return headers;
	}

	@Override
	public List<SamplingUnit> importFile(File f, HashMap<Object, Object> options) throws Exception {
		
		List<SamplingUnit> units = new ArrayList<SamplingUnit>();
		
		SamplingUnit.SamplingUnitType type = (SamplingUnitType) options.get(TYPE_KEY);
		if (type == null){
			throw new Exception("Sampling unit type cannot be determined.");
		}
		
		Double bufferValue = (Double) options.get(BUFFER_KEY);
		Projection proj = (Projection)options.get(PROJECTION_KEY);
		
		String idField = (String) options.get(ID_FIELD_KEY);
		String x1Field = (String)options.get(X1_FIELD_KEY);
		String y1Field = (String)options.get(Y1_FIELD_KEY);
		String x2Field = (String)options.get(X2_FIELD_KEY);
		String y2Field = (String)options.get(Y2_FIELD_KEY);
		
		Character delim = (Character) options.get(DELIMETER_KEY);
		
		CSVReader reader = new CSVReader(new FileReader(f), delim.charValue());
		try{
			//read the first line
			String[] headers = reader.readNext();
			
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
			
			GeometryFactory gf = new GeometryFactory();
			int cnt = 0;
			while(true){
				headers = reader.readNext();
				if (headers == null){
					break;
				}
				
				SamplingUnit su = new SamplingUnit();
				su.setAttributes(new ArrayList<SamplingUnitAttributeValue>());
								
				if (type != SamplingUnit.SamplingUnitType.OPEN_TRANSECT){
					su.setBuffer(bufferValue);
				}
				
				if (type == SamplingUnitType.PLOT){
					//point
					Double x = Double.parseDouble(headers[x1]);
					Double y = Double.parseDouble(headers[y1]);
					
					Coordinate out = new Coordinate(x,y);
					if (!CRS.equalsIgnoreMetadata(proj.getCrs(),SmartDB.DATABASE_CRS)){
						out = ReprojectUtils.reproject(out.x, out.y, proj.getCrs(), SmartDB.DATABASE_CRS);
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
					if (!proj.getCrs().equals(SmartDB.DATABASE_CRS)){
						p1 = ReprojectUtils.reproject(p1.x, p1.y, proj.getCrs(), SmartDB.DATABASE_CRS);
						p2 = ReprojectUtils.reproject(p2.x, p2.y, proj.getCrs(), SmartDB.DATABASE_CRS);
					}
					su.setGeometry(gf.createLineString(new Coordinate[]{p1,p2}));
				}
				
				if (idColumn != null){
					String id = headers[idColumn];
					if (id.length() == 0){
						id = "Unit " + cnt;
					}
					su.setId(id);
				}else{
					su.setId("Unit " + cnt);
				}
				cnt++;
				
				su.setState(SamplingUnit.State.ACTIVE);
				su.setType(type);
//				su.setSurveyDesign(survey);
				
				
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
							suv.setStringValue(value);
							add = true;
						}
					}else if (att.getType() == AttributeType.NUMERIC){						
						if (value.trim().length() > 0){
							try{
								suv.setDoubleValue(  Double.valueOf(value) );
								add = true;
							}catch (Exception ex){
								//TODO: deal with this
							}
						}
					}else{
						throw new Exception(MessageFormat.format("Attribute type {0} not supported for sampling unit attributes", new Object[]{att.getType()}));
					}
					if (add){
						su.getAttributes().add(suv);
					}
					
				}
				units.add(su);
			}
			
		}finally{
			reader.close();
		}
		
		return units;
	}

}
