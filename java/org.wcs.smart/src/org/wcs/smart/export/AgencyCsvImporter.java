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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.OptionSelectionDialog;

import au.com.bytecode.opencsv.CSVReader;


/**
 * Importer for importing agencies and ranks data.  This importer
 * imports data into memory and NOT the conservation area.  The
 * imported data is available via the getImportedData function.
 * 
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class AgencyCsvImporter implements ICsvDataImporter {

	private Map<String, Language> code2Language;
	private Map<String, Agency> record2Agency;
	private Collection<Agency> importedData;
	
	public AgencyCsvImporter() {
		//nothing
	}
	
	@Override
	public List<String> getWarnings(){
		return null;
	}
	
	/**
	 * 
	 * @return the set of agencies and associated ranks
	 * imported
	 */
	public Collection<Agency> getImportedData(){
		return importedData;
	}
	
	@Override
	public boolean importCsvFile(File file, char delimiter, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		if (!file.exists()){
			throw new IOException(MessageFormat.format(Messages.EmployeeCsvImporter_Error_InputFileDoesNotExist1, new Object[]{file.toString()}));
		}
		
		try(CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), delimiter)){
			//reading the first line with language codes
			String[] row = reader.readNext();
			int size = row.length;
			int langNumber = size/2;
			List<String> langCodes = getLanguageCodes(row);

			code2Language = createCode2Language(langCodes);
			if (code2Language == null) {
				return false;
			}
			record2Agency = new HashMap<String, Agency>();
		
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
		}
		if (monitor.isCanceled()) return false;
		
		importedData = record2Agency.values();
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
		return code2Language.get(code);
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

	private Map<String, Language> createCode2Language(List<String> langCodes) {
		Map<String, Language> result = new HashMap<String, Language>();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Set<Language> languages = ca.getLanguages();
		for (Language language : languages) {
			result.put(language.getCode(), language);
		}
		//need to check if default language present in importing file,
		//cause default language values must be set anyway
		String defaultCode = ca.getDefaultLanguage().getCode();
		if (!langCodes.contains(defaultCode)) {
			//if it is not present than asking user which language to use as default
			LanguageDialogRunnable runnable = new LanguageDialogRunnable(defaultCode, langCodes.toArray(new String[0]));
			Display.getDefault().syncExec(runnable);
			String code = runnable.getResult();
			if (code == null) {
				return null; //no default code specified
			}
			//make a fake correspondence of selected code with default language
			result.put(code, ca.getDefaultLanguage());
		}
		return result;
	}

	/**
	 * 
	 * Class is responsible for launching {@link DefaultLanguageSelectDialog} in
	 * GUI thread and provide back selected result.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LanguageDialogRunnable implements Runnable {
		private final String defaultCode;
		private final String[] options;
		private String result;

		private LanguageDialogRunnable(String defaultCode, String[] options) {
			this.defaultCode = defaultCode;
			this.options = options;
		}

		@Override
		public void run() {
			Shell shell = Display.getDefault().getActiveShell();
			OptionSelectionDialog dialog = new OptionSelectionDialog(shell, options,
					Messages.AgencyCsvImporter_LanguageDialog_Title,
					MessageFormat.format(Messages.AgencyCsvImporter_LanguageDialog_Message, defaultCode));
			
			if (dialog.open() != IDialogConstants.OK_ID) {
				setResult(null);
				return;
			}
			setResult(dialog.getSelectedOption());
		}
		
		public String getResult() {
			return result;
		}
		
		public void setResult(String result) {
			this.result = result;
		}
	}

}
