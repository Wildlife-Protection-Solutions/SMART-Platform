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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.util.SharedUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for track and transport types (to single files)
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
	public boolean exportCsvFile(Path file, char delimiter, ConservationArea ca, boolean headers, Charset cs, IProgressMonitor monitor, Session session) {

		List<Language> languages = new ArrayList<Language>(ca.getLanguages().size());
		for (Language l : ca.getLanguages()) {
			languages.add(session.get(Language.class, l.getUuid()));
		}
		try (CSVWriter writer = new CSVWriter(
					new OutputStreamWriter(Files.newOutputStream(file), cs),
					delimiter, '"',SharedUtils.LINE_SEPARATOR)){

			// WriteHeaders
			String[] columns = createColumns(languages);
			writer.writeNext(columns);

			String csvout[] = new String[columns.length];
			List<PatrolType> types = getPatrolTypes(ca, session);
			for (PatrolType type : types) {
				if (monitor.isCanceled()) return false;
				if (type.isMixed()) continue;
				
				int i = 0;
				csvout[i++] = PatrolTransportCsvExportConfig.TRACKTYPE;
				csvout[i++] = type.getKeyId();
				csvout[i++] = type.getIcon() == null ? "" : type.getIcon().getKeyId(); //$NON-NLS-1$
				csvout[i++] = type.getIsActive() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
				csvout[i++] = type.getRequiresPilot() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
				csvout[i++] = String.valueOf(type.getMaxSpeed());
				csvout[i++] = ""; //$NON-NLS-1$
				for(Language l : languages){
					csvout[i++] = type.findName(l);
				}
				writer.writeNext(csvout);
			}
			List<PatrolTransportType> ttypes = getTransportTypes(ca, session);


			//for each station write one record
			for (PatrolTransportType type : ttypes) {
				if (monitor.isCanceled()) return false;
				
				// entry in string array (csv_out) of names
				int i = 0;
				csvout[i++] = PatrolTransportCsvExportConfig.TRANSPORTTYPE;
				csvout[i++] = type.getKeyId();
				csvout[i++] = type.getIcon() == null ? "" : type.getIcon().getKeyId(); //$NON-NLS-1$
				csvout[i++] = type.getIsActive() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
				csvout[i++] = ""; //$NON-NLS-1$
				csvout[i++] = ""; //$NON-NLS-1$
				csvout[i++] = type.getPatrolType().getKeyId();
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
		String[] cols = new String[langs.size() + 7];
		int i = 0;
		cols[i++] = "Type"; //$NON-NLS-1$
		cols[i++] = "Key"; //$NON-NLS-1$
		cols[i++] = "Icon_Key"; //$NON-NLS-1$
		cols[i++] = "Is_Active"; //$NON-NLS-1$
		cols[i++] = "Requires_Pilot"; //$NON-NLS-1$
		cols[i++] = "Max_Speed"; //$NON-NLS-1$
		cols[i++] = "Track_Type"; //$NON-NLS-1$
		for (Language lng : langs) {
			cols[i++] = "Name>" + lng.getCode(); //$NON-NLS-1$
		}

		return cols;
	}

	private List<PatrolTransportType> getTransportTypes(ConservationArea ca, Session session) {
		List<PatrolTransportType> tt = QueryFactory.buildQuery(session, 
				PatrolTransportType.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		return tt;
	}

	private List<PatrolType> getPatrolTypes(ConservationArea ca, Session session) {
		List<PatrolType> tt = QueryFactory.buildQuery(session, 
				PatrolType.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		return tt;
	}
}
