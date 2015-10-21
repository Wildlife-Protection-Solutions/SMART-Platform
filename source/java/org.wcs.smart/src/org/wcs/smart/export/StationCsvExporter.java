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
package org.wcs.smart.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for stations
 * 
 * @author Emily
 * @since 2.0.0
 */
public class StationCsvExporter implements ICsvDataExporter {
	
	/**
	 * Creates a new exporter that exports Stations to csv format
	 */
	public StationCsvExporter() {
		//nothing
	}

	@Override
	public boolean exportCsvFile(File file, char delimiter, ConservationArea ca, 
			boolean headers, IProgressMonitor monitor, Session session) {
		List<Language> languages = new ArrayList<Language>(ca.getLanguages());
		try (CSVWriter writer = new CSVWriter(
					new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
					delimiter, '"',SharedUtils.LINE_SEPARATOR)){ 
			List<Station> stations = getStations(ca, session);

			// WriteHeaders
			String[] stationColumns = createColumns(languages);
			writer.writeNext(stationColumns);

			//for each station write one record
			for (Station station : stations) {
				if (monitor.isCanceled()) return false;
				
				// entry in string array (csv_out) of names
				int i = 0;
				String csvout[] = new String[stationColumns.length];
				for(Language l : languages){
					csvout[i++] = station.findName(l);
					csvout[i++] = station.findDescriptionNull(session, l) == null ? "" : station.findDescriptionNull(session, l); //$NON-NLS-1$
				}
				writer.writeNext(csvout);
				
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
	
	private String[] createColumns(List<Language> langs) {
		String[] stationCols = new String[(2*langs.size())];
		
		int i = 0;
		for (Language lng : langs) {
			stationCols[i++] = "Name>" + lng.getCode(); //$NON-NLS-1$
			stationCols[i++] = "Description>" + lng.getCode(); //$NON-NLS-1$
		}

		return stationCols;
	}

	private List<Station> getStations(ConservationArea ca, Session session) {
		session.beginTransaction();
		try{
			return HibernateManager.getStations(ca, session);
		}finally{
			session.getTransaction().rollback();
		}
	}

}
