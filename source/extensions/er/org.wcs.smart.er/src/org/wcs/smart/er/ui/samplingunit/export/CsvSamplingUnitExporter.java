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
package org.wcs.smart.er.ui.samplingunit.export;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;

import au.com.bytecode.opencsv.CSVWriter;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * CSV File importer sampling unit.
 * 
 * @author Emily
 *
 */
public class CsvSamplingUnitExporter implements ISamplingUnitExporter {

	public static final String DELIMETER_KEY = "DELIMITER"; //$NON-NLS-1$
	
	@Override
	public String getFileExtension(){
		return "csv"; //$NON-NLS-1$
	}

	@Override
	public void exportFile(File f, SurveyDesign sd,
			Session session,
			HashMap<Object, Object> options, IProgressMonitor monitor) throws Exception {
		
		GeometryType type = (GeometryType) options.get(SU_TYPE_KEY);
		if (type == null){
			throw new Exception(Messages.CsvSamplingUnitExporter_SuTypeError);
		}
		
		Set<GeometryType> types = SurveyHibernateManager.getInstance().getSamplingUnitTypes(sd, session);
		if (!types.contains(type)){
			//nothing to export
			return;
		}
				
		Character delimiter = (Character) options.get(DELIMETER_KEY);
		if (delimiter == null){
			delimiter = ',';
		}
		
		sd = (SurveyDesign) session.load(SurveyDesign.class, sd.getUuid());
		
		try(CSVWriter writer = new CSVWriter(new FileWriter(f), delimiter)){
			exportPlotsAndTransects(type, writer, sd, session, monitor);
		}finally{
			monitor.done();
		}
	}
	
	private void exportPlotsAndTransects(GeometryType type, CSVWriter writer, 
			SurveyDesign sd, Session session, IProgressMonitor monitor) throws Exception{
		
		WKTWriter wktWriter = new WKTWriter();
		
		//write header
		String[] headers = getHeaders(type, sd);
		writer.writeNext(headers);

		@SuppressWarnings("unchecked")
		List<SamplingUnit> units = session.createCriteria(SamplingUnit.class)
				.add(Restrictions.eq("surveyDesign", sd)) //$NON-NLS-1$
				.add(Restrictions.eq("type", type)) //$NON-NLS-1$
				.list();
		
		monitor.beginTask(Messages.CsvSamplingUnitExporter_Progress, units.size());
		int index = 0;
		for (SamplingUnit unit : units){
			index = 0;
			String[] data = new String[headers.length];
				
			data[index++] = unit.getId();
			data[index++] = unit.getType().getGuiName(Locale.getDefault());
			data[index++] = unit.getState().getGuiName(Locale.getDefault());
			
			if (type != GeometryType.PLOT){
				Double l = unit.getGeometryLengthKm();
				data[index++] = l == null ? "" : l.toString(); //$NON-NLS-1$
				LineString ls = (LineString)unit.getGeometry();
				data[index++] = String.valueOf(ls.getCoordinateN(0).x);
				data[index++] = String.valueOf(ls.getCoordinateN(0).y);
				data[index++] = String.valueOf(ls.getCoordinateN(ls.getNumPoints()-1).x);
				data[index++] = String.valueOf(ls.getCoordinateN(ls.getNumPoints()-1).y);
			
				data[index++] = wktWriter.write(unit.getGeometry());
			}else{
				Point p = (Point)unit.getGeometry();
				data[index++] = String.valueOf(p.getCoordinate().x);
				data[index++] = String.valueOf(p.getCoordinate().y);
			}
			
			for (SurveyDesignSamplingUnitAttribute a : sd.getSamplingUnitAttributes()){
				String value = null;
				for (SamplingUnitAttributeValue v : unit.getAttributes()){
					if (v.getSamplingUnitAttribute().equals(a.getSamplingUnitAttribute())){
						if (v.getSamplingUnitAttribute().getType() == AttributeType.TEXT){
							value = v.getStringValue();
						}else if (v.getSamplingUnitAttribute().getType() == AttributeType.NUMERIC){
							value = v.getNumberValue().toString();
						}else if (v.getSamplingUnitAttribute().getType() == AttributeType.LIST){
							if (v.getAttributeListItem() == null){
								value = null;
							}else{
								value = v.getAttributeListItem().getName();
							}
						}
					}
				}
				data[index++] = value;
			}
			writer.writeNext(data);
			monitor.worked(1);
		}
	}
	
	private String[] getHeaders(GeometryType type, SurveyDesign sd){
		if (type == GeometryType.TRANSECT ||
				type == GeometryType.PLOT){
			
			int size = 6;
			if (type != GeometryType.PLOT){
				size +=4;
			}
			size += sd.getSamplingUnitAttributes().size();
			
			//write header
			String[] data = new String[size];
			int index = 0;
			data[index++] = Messages.CsvSamplingUnitExporter_idColumnName;
			data[index++] = Messages.CsvSamplingUnitExporter_typeColumnName;
			data[index++] = Messages.CsvSamplingUnitExporter_stateColumnName;
			if (type != GeometryType.PLOT){
				data[index++] = Messages.CsvSamplingUnitExporter_lengthColumnName;
				data[index++] = Messages.CsvSamplingUnitExporter_x1ColumnName;
				data[index++] = Messages.CsvSamplingUnitExporter_y1ColumnName;
				data[index++] = Messages.CsvSamplingUnitExporter_x2ColumnName;
				data[index++] = Messages.CsvSamplingUnitExporter_y2ColumnName;
				data[index++] = Messages.CsvSamplingUnitExporter_wktColumnName;
			}else{
				data[index++] = Messages.CsvSamplingUnitExporter_xColumnName;
				data[index++] = Messages.CsvSamplingUnitExporter_yColumnName;
			}
			for (SurveyDesignSamplingUnitAttribute a : sd.getSamplingUnitAttributes()){
				data[index++] = a.getSamplingUnitAttribute().getName();
			}
			return data;
		}
		return null;
	}
}
