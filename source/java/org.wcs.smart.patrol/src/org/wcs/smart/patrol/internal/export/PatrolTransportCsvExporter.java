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
package org.wcs.smart.patrol.internal.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for stations
 * 
 * @author Emily
 * @since 2.0.0
 */
public class PatrolTransportCsvExporter implements ICsvDataExporter {
	
	/**
	 * Creates a new exporter that exports Stations to csv format
	 */
	public PatrolTransportCsvExporter() {
		//nothing
	}

	@Override
	public boolean exportCsvFile(File file, char delimiter, ConservationArea ca, boolean headers, IProgressMonitor monitor, Session session) {

		List<Language> languages = new ArrayList<Language>(ca.getLanguages());
		
		try (CSVWriter writer = new CSVWriter(
					new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), //$NON-NLS-1$ 
					delimiter, '"',SharedUtils.LINE_SEPARATOR)){

			List<PatrolTransportType> types = getTransportTypes(ca, session);

			// WriteHeaders
			String[] columns = createColumns(languages);
			writer.writeNext(columns);

			//for each station write one record
			for (PatrolTransportType type : types) {
				if (monitor.isCanceled()) return false;
				
				// entry in string array (csv_out) of names
				int i = 0;
				String csvout[] = new String[columns.length];
				csvout[i++] = type.getPatrolType().name();
				csvout[i++] = type.getKeyId();
				
				for(Language l : languages){
					csvout[i++] = type.findName(l);
				}
				writer.writeNext(csvout);
				
			}
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
	
	private String[] createColumns(List<Language> langs) {
		String[] cols = new String[langs.size() + 2];
		int i = 0;
		cols[i++] = "Type"; //$NON-NLS-1$
		cols[i++] = "Key"; //$NON-NLS-1$
		for (Language lng : langs) {
			cols[i++] = "Name>" + lng.getCode(); //$NON-NLS-1$
		}

		return cols;
	}

	@SuppressWarnings("unchecked")
	private List<PatrolTransportType> getTransportTypes(ConservationArea ca, Session session) {
		session.beginTransaction();
		try{
			return session.createCriteria(PatrolTransportType.class).add(Restrictions.eq("conservationArea", ca)).list(); //$NON-NLS-1$
		}finally{
			session.getTransaction().rollback();
		}
	}

}
