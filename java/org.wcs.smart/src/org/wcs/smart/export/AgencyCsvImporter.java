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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import au.com.bytecode.opencsv.CSVReader;

import com.ibm.icu.text.MessageFormat;

/**
 * Importer for importing agencies and ranks data into
 * the current conservation area.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class AgencyCsvImporter implements ICsvDataImporter {

	private Map<String, Language> code2Language;
	private Map<String, Agency> record2Agency;

	public AgencyCsvImporter() {
		//nothing
	}

	@Override
	public boolean importCsvFile(File file, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		if (!file.exists()){
			throw new IOException(Messages.EmployeeCsvImporter_Error_InputFileDoesNotExist + file.toString() );
		}

		code2Language = null;
		record2Agency = new HashMap<String, Agency>();

		CSVReader reader = new CSVReader(new FileReader(file));
		
		
		//reading the first line with language codes
		String[] row = reader.readNext();
		int size = row.length;
		int langNumber = size/2;
		List<String> langCodes = getLanguageCodes(row);

		int line = 2;
		Agency agency = null;
		while( (row = reader.readNext()) != null ) {
			if (monitor.isCanceled()) return false;
			if (row.length != size) {
				throw new Exception(MessageFormat.format(Messages.AgencyCsvImporter_Error_IncorrectFieldsNumber, new Object[]{line, row.length, size}));
			}
			
			agency = handleAgency(row, langCodes);
			
			//adding ranks
			Rank rank = new Rank();
			boolean hasRecords = false;
			for (int i = langNumber; i < size; i++) {
				String value = row[i];
				if (value != null && !value.isEmpty()) {
					Language lang = getLanguage(langCodes.get(i-langNumber));
					if (lang != null) {
						hasRecords = true;
						rank.updateName(lang, value);
					}
				}
			}
			if (hasRecords) {
				rank.setAgency(agency);
				agency.getRanks().add(rank);
			}
			//adding ranks end
		}

		Collection<Agency> agencies = record2Agency.values();
		if (monitor.isCanceled()) return false;
		try{
			session.beginTransaction();
			for (Agency a : agencies) {
				session.saveOrUpdate(a);
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			throw new Exception(Messages.AgencyCsvImporter_Error_DatabaseSaveFailed + ex.getLocalizedMessage(), ex);
		}
		return true;
	}

	/**
	 *  Returns already existing agency for given row record or creates new one if required
	 * @param row
	 * @param langCodes
	 * @return
	 */
	private Agency handleAgency(String[] row, List<String> langCodes) {
		int langNum = row.length/2;
		String record = Arrays.toString(Arrays.copyOf(row, langNum));
		Agency agency = record2Agency.get(record);
		if (agency == null) {
			agency = new Agency();
			for (int i = 0; i < langNum; i++) {
				String value = row[i];
				if (value != null && !value.isEmpty()) {
					Language lang = getLanguage(langCodes.get(i));
					if (lang != null) {
						agency.updateName(lang, value);
					}
				}
			}
			agency.setConservationArea(SmartDB.getCurrentConservationArea());
			record2Agency.put(record, agency);
		}
		return agency;
	}

	private Language getLanguage(String code) {
		return getCode2Language().get(code);
	}

	private List<String> getLanguageCodes(String[] columns) {
		List<String> result = new ArrayList<String>();
		int langNum = columns.length/2;
		for (int i = 0; i < langNum; i++) {
			int index = columns[i].lastIndexOf('>');
			result.add(columns[i].substring(index+1));
		}
		return result;
	}

	private Map<String, Language> getCode2Language() {
		if (code2Language == null) {
			code2Language = createCode2Language();
		}
		return code2Language;
	}

	private Map<String, Language> createCode2Language() {
		Map<String, Language> result = new HashMap<String, Language>();
		Set<Language> languages = SmartDB.getCurrentConservationArea().getLanguages();
		for (Language language : languages) {
			result.put(language.getCode(), language);
		}
		return result;
	}
}
