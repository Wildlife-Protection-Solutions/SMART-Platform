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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV Query exporter for simple queries.
 * 
 * @author Garrett
 * @author elitvin
 * @since 1.0.0
 */
public class AgencyCsvExporter implements ICsvDataExporter {
	protected List<Language> languages;
	public ConservationArea ca;
	
	/**
	 * Creates a new exporter that exports Agencies and their Ranks to csv format
	 */
	public AgencyCsvExporter() {
		//nothing
	}

	@Override
	public boolean exportCsvFile(File file, char delimiter, ConservationArea ca, boolean headers, IProgressMonitor monitor, Session session) {
		CSVWriter writer = null;
		try {
			this.ca = ca;
			languages = new ArrayList<Language>(ca.getLanguages());
			writer = new CSVWriter(new FileWriter(file), delimiter, '"',SmartUtils.LINE_SEPARATOR);
			List<Agency> agencies = getAgencies(session);

			// WriteHeaders
			String[] agencyCols = createColumns(languages);
			writer.writeNext(agencyCols);

			Set<Label> agentAllLang;
			Set<Label> rankAllLang;
			String csv_out[] = new String[agencyCols.length];
			// must change the following to write one column for each of the
			// corresponding languages
			for (Agency agt : agencies) {
				if (monitor.isCanceled()) return false;
				agentAllLang = agt.getNames();
				// entry in string array (csv_out) of names
				for (Label agt_lbl : agentAllLang) {
					csv_out[findcolumnIndex(agt_lbl)] = agt_lbl.getValue();
				}
				writer.writeNext(csv_out);
				// now for this agency get ranks
				List<Rank> ranks = getRanks(agt);
				if (!ranks.isEmpty()) {
					for (Rank ranker : ranks) {
						rankAllLang = ranker.getNames();
						for (Label rnk_lbl : rankAllLang) {
							csv_out[(languages.size() + findcolumnIndex(rnk_lbl))] = rnk_lbl.getValue();	
						}
						writer.writeNext(csv_out);
					}
					// clear the csv_out Array.
					for (int i = 0; i < csv_out.length; i++) {
						csv_out[i] = ""; //$NON-NLS-1$
					}
				}
			}
			writer.close();
			return true;
		} catch (IOException ex) {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				return false;
			}
			return false;
		}
	}
	
	private String[] createColumns(List<Language> langs) {
		String[] agtcols = new String[(2*langs.size())];
		
		int i=0;
		for (Language lng : langs) {
			agtcols[i] = "Agent>" + lng.getCode(); //$NON-NLS-1$
			i++;
		}

		for (Language lng : langs) {
			agtcols[i] = "Rank>" + lng.getCode(); //$NON-NLS-1$
			i++;
		}
		return agtcols;
	}

	private int findcolumnIndex(Label lbl){
		int i = 0;
		for (Language lng : languages) {
			if (lbl.getLanguage().getCode().equals(lng.getCode())){
				return i;
			}
			i++;
		}
		return -1;
	}

	private List<Rank> getRanks(Agency agent) {
		if (agent.getUuid() != null) {
			return agent.getRanks();
		}
		return Collections.emptyList();
	}

	private List<Agency> getAgencies(Session session) {
		session.beginTransaction();
		try{
			return HibernateManager.getAgencies(ca, session);
		}finally{
			session.getTransaction().rollback();
		}
	}

}
