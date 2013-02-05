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
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.util.SmartUtils;
import au.com.bytecode.opencsv.CSVWriter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.ca.export.CaExporter;
import org.hibernate.Session;

/**
 * CSV Query exporter for simple queries.
 * 
 * @author Garrett
 * @since 1.0.0
 */
public class AgencyCsvExport {
	protected List<Agency> agencies;
	protected List<Rank> ranks;
	protected Set<Language> langs;
	public File outputFile;
	private CSVWriter writer = null;
	private String agencyCols[];
	private String rankCols[]; 
	public ConservationArea ca;
	private DataExportEngine exportengine;
	
	/**
	 * Creates a new exporter that exports Agencies and their Ranks to csv format
	 * @throws Exception 
	 */
	public AgencyCsvExport() throws Exception{
		this.init();
	}
	
	/**
	 * Close csv writer
	 * 
	 */
	protected void finish() throws Exception{
		writer.close();
	}

	protected void init() throws Exception {
		ca = SmartDB.getCurrentConservationArea();
		setOutputFile();
		writer = new CSVWriter(new FileWriter(outputFile), ',', '"',SmartUtils.LINE_SEPARATOR);
		createcols();
		this.agencies = this.getAgencies();
	}

	private void createcols(){
		this.langs = ca.getLanguages();
		String[] agtcols = new String[(2*langs.size())];
		
		int i=0;
		for (Language lng : langs) {
			agtcols[i] = "Agent>" + lng.getCode();
			i++;
		}

		for (Language lng : langs) {
			agtcols[i] = "Rank>" + lng.getCode();
			i++;
		}
		this.agencyCols = agtcols;
	}

	private void writeHeaders(){
		writer.writeNext(agencyCols);
	}

	public boolean export() {
		try {
			// WriteHeaders
			writeHeaders();
			Set<Label> agentAllLang;
			Set<Label> rankAllLang;
			String csv_out[] = new String[agencyCols.length];
			// must change the following to write one column for each of the
			// corresponding languages
			for (Agency agt : agencies) {
				agentAllLang = agt.getNames();
				// entry in string array (csv_out) of names
				for (Label agt_lbl : agentAllLang) {
					csv_out[findcolumnIndex(agt_lbl)] = agt_lbl.getValue();
				}
				writer.writeNext(csv_out);
				// now for this agency get ranks
				this.getRanks(agt);
				if (!ranks.isEmpty()) {
					for (Rank ranker : ranks) {
						rankAllLang = ranker.getNames();
						for (Label rnk_lbl : rankAllLang) {
							csv_out[(langs.size() + findcolumnIndex(rnk_lbl))] = rnk_lbl.getValue();	
						}
						writer.writeNext(csv_out);
					}
					// clear the csv_out Array.
					for (int i = 0; i < csv_out.length; i++) {
						csv_out[i] = "";
					}
					ranks.clear();
				}
			}
			agencies.clear();
			finish();
			return true;
		} catch (Exception ex) {
			try {
				finish();
			} catch (Exception e) {
				return false;
			}
			return false;
		}
	}

	private int findcolumnIndex(Label lbl){
		int i = 0;
		for (Language lng : langs) {
			if (lbl.getLanguage().getCode().equals(lng.getCode())){
				return i;
			}
			i++;
		}
		return -1;
	}

	public String getId(){
		return "org.wcs.smart.export.AgencyCsvExport";
	}

	public String getName() {
		return "Comma Separated Values";
	}

	public String getDefaultExtension() {
		return "csv";
	}

	private List<Rank> getRanks(Agency agent) {
		if (agent.getUuid() != null) {
			ranks = agent.getRanks();
		}
		return ranks;
	}

	private List<Agency> getAgencies() {
		if (agencies == null) {
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			agencies = HibernateManager.getAgencies(ca, s);
			s.getTransaction().rollback();
		}
		return agencies;
	}

	private void setOutputFile() throws IOException {
		File tmpFile = new File("C:/" + ca.getName() + "_Agencies.csv");
		if(!tmpFile.exists()){
			  tmpFile.createNewFile();
			  }
		this.outputFile = tmpFile;
	}

}
