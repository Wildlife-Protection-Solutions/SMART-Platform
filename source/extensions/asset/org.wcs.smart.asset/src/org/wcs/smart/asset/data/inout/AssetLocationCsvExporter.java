/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV exporter for exporting field sensor locations and associated attributes
 * 
 * @author Emily
 * @since 7.0.0
 */
public class AssetLocationCsvExporter implements ICsvDataExporter {

	public enum FixedField{
		ID (Messages.AssetLocationCsvExporter_IDFieldName),
		STATION_ID (Messages.AssetLocationCsvExporter_StationIdFieldName),
		X (Messages.AssetLocationCsvExporter_XFieldName),
		Y (Messages.AssetLocationCsvExporter_YFieldName),
		GEOM (Messages.AssetLocationCsvExporter_GeometryFieldName),
		BUFFER (Messages.AssetLocationCsvExporter_BufferFieldName);
		
		String name;
		
		FixedField(String name) {
			this.name = name;
		}
	}
	
	private HashMap<AssetAttribute,Integer> attribute2column;


	/**
	 * Only configured after call to export made
	 * 
	 * @return
	 */
	public HashMap<AssetAttribute,Integer> getAttribute2ColumnMapping(){
		return attribute2column;
	}
	
	@Override
	public boolean exportCsvFile(Path file, char delimiter, ConservationArea ca, 
			boolean headers, Charset cs, IProgressMonitor monitor, Session session) {
		
		List<AssetStationLocationAttribute> attributes = getAttributes(ca, session);
		
		List<AssetStationLocation> stations = getLocations(ca, session);
		
		try (CSVWriter writer = new CSVWriter(
					new OutputStreamWriter(Files.newOutputStream(file), cs),
					delimiter, '"',SharedUtils.LINE_SEPARATOR)){ 
			
			// WriteHeaders
			String[] stationColumns = createColumns(attributes);
			if (headers) {
				writer.writeNext(stationColumns);
			}

			//for each station write one record
			String data[] = new String[stationColumns.length];
			for (AssetStationLocation location : stations) {
				if (monitor.isCanceled()) return false;
				
				int i = 0;
				
				data[i++] = location.getId();
				data[i++] = location.getStation().getId();
				data[i++] = location.getX().toString();
				data[i++] = location.getY().toString();
				data[i++] = "POINT(" + location.getX() + " " + location.getY() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				data[i++] = location.getBuffer().toString();
				
				
				for (AssetStationLocationAttribute a : attributes) {
					data[i] = null;
					for (AssetStationLocationAttributeValue v : location.getAttributeValues()) {
						if (v.getAttribute().equals(a.getAttribute())) {
							data[i] = v.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
							break;
						}
					}
					i++;
				}
				
				writer.writeNext(data);
				
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
	
	private String[] createColumns(List<AssetStationLocationAttribute> attributes) {
		attribute2column = new HashMap<>();
		List<String> columns = new ArrayList<>();
		
		int i = 0;
		for (FixedField f : FixedField.values()) {
			columns.add(f.name);
			i++;
		}
		
		for (AssetStationLocationAttribute a : attributes) {
			attribute2column.put(a.getAttribute(),i);
			columns.add(a.getAttribute().getName());
			i++;
		}
		return columns.toArray(new String[columns.size()]);

	}

	private List<AssetStationLocationAttribute> getAttributes(ConservationArea ca, Session session){
			return session.createQuery("FROM AssetStationLocationAttribute WHERE attribute.conservationArea = :ca", AssetStationLocationAttribute.class) //$NON-NLS-1$
					.setParameter("ca",  ca).list(); //$NON-NLS-1$
	}
	private List<AssetStationLocation> getLocations(ConservationArea ca, Session session) {
		return session.createQuery("FROM AssetStationLocation WHERE station.conservationArea = :ca", AssetStationLocation.class) //$NON-NLS-1$
				.setParameter("ca",  ca).list(); //$NON-NLS-1$
	}

}
