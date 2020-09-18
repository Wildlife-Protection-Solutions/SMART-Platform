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
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for field sensors
 * 
 * @author Emily
 * @since 7.0.0
 */
public class AssetCsvExporter implements ICsvDataExporter {
	
	public enum FixedField{
		ID (Messages.AssetCsvExporter_IDFieldName),
		TYPE (Messages.AssetCsvExporter_TypeFieldName),
		RETIRED(Messages.AssetCsvExporter_RetiredFieldName);
		
		String name;
		
		FixedField(String name) {
			this.name = name;
		}
	}
	
	private HashMap<AssetAttribute,Integer> attribute2column;
	

	@Override
	public boolean exportCsvFile(Path file, char delimiter, ConservationArea ca, 
			boolean headers, Charset cs, IProgressMonitor monitor, Session session) {
		
		List<AssetAttribute> attributes = getAttributes(ca, session);
		
		List<Asset> assets = getAssets(ca, session);
		
		try (CSVWriter writer = new CSVWriter(
					new OutputStreamWriter(Files.newOutputStream(file), cs),
					delimiter, '"',SharedUtils.LINE_SEPARATOR)){ 

			// WriteHeaders
			String[] columns = createColumns(attributes);
			if (headers) {
				writer.writeNext(columns);
			}

			//for each station write one record
			String data[] = new String[columns.length];
			for (Asset asset : assets) {
				if (monitor.isCanceled()) return false;
				
				int i = 0;
				
				data[i++] = asset.getId();
				data[i++] = asset.getAssetType().getName();
				data[i++] = asset.getIsRetired().toString();
				
				for (AssetAttribute a : attributes) {
					data[i] = null;
					
					for (AssetAttributeValue v : asset.getAttributeValues()) {
						if (v.getAttribute().equals(a)) {
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
	
	
	/**
	 * Only configured after call to export made
	 * @return
	 */
	public HashMap<AssetAttribute,Integer> getAttribute2ColumnMapping(){
		return attribute2column;
	}
	
	private String[] createColumns(List<AssetAttribute> attributes) {
		
		attribute2column = new HashMap<>();
		List<String> columns = new ArrayList<>();
		
		for (FixedField f : FixedField.values()) {
			columns.add(f.name);
		}
		
		int i = 3;
		for (AssetAttribute a : attributes) {
			columns.add(a.getName());
			attribute2column.put(a,i);
			i++;
		}

		return columns.toArray(new String[columns.size()]);
	}

	private List<AssetAttribute> getAttributes(ConservationArea ca, Session session){
			return session.createQuery("SELECT distinct b FROM AssetTypeAttribute a join a.id.attribute b WHERE b.conservationArea = :ca", AssetAttribute.class) //$NON-NLS-1$
					.setParameter("ca", ca) //$NON-NLS-1$
					.list();
	}
	private List<Asset> getAssets(ConservationArea ca, Session session) {
		return session.createQuery("FROM Asset WHERE conservationArea = :ca", Asset.class) //$NON-NLS-1$
				.setParameter("ca",  ca).list(); //$NON-NLS-1$
	}

}
