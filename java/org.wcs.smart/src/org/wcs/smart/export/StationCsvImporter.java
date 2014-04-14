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
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.OptionSelectionDialog;

import au.com.bytecode.opencsv.CSVReader;

import com.ibm.icu.text.MessageFormat;

/**
 * Importer for importing stations.  This importer
 * imports data into memory and NOT the conservation area.  The
 * imported data is available via the getImportedData function.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class StationCsvImporter implements ICsvDataImporter {

	private Collection<Station> importedData;
	
	public StationCsvImporter() {
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
	public Collection<Station> getImportedData(){
		return importedData;
	}
	
	@Override
	public boolean importCsvFile(File file, char delimiter, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		if (!file.exists()){
			throw new IOException(MessageFormat.format(Messages.EmployeeCsvImporter_Error_InputFileDoesNotExist1, new Object[]{ file.toString() }));
		}

		CSVReader reader = new CSVReader(new FileReader(file), delimiter);
		
		//reading the first line with language codes
		String[] headerRow = reader.readNext();
		List<String> langCodes = getLanguageCodes(headerRow);
		if (headerRow.length % 2 != 0){
			throw new Exception(Messages.StationCsvImporter_InvalidStationCsvFileFormat);
		}
		Map<String,Language> code2Language = createCode2Language(langCodes);
		if (code2Language == null) {
			return false;
		}
		ArrayList<Station> stations = new ArrayList<Station>();
		String[] row;
		int line = 2;
		while( (row = reader.readNext()) != null ) {
			if (monitor.isCanceled()) return false;
			if (row.length != headerRow.length) {
				throw new Exception(MessageFormat.format(Messages.AgencyCsvImporter_Error_IncorrectFieldsNumber, new Object[]{line, row.length, headerRow.length}));
			}
			
			Station station = handleStation(row, langCodes, code2Language);
			stations.add(station);
			line++;
		}

		if (monitor.isCanceled()) return false;
		
		importedData = stations;
		return true;
	}

	/**
	 *  Returns already existing agency for given row record or creates new one if required
	 * @param row
	 * @param langCodes
	 * @return
	 */
	private Station handleStation(String[] row, List<String> columnLanguages, Map<String, Language> langCodes) {
		Station station = new Station();
		station.setIsActive(true);
		station.setConservationArea(SmartDB.getCurrentConservationArea());
		
		for (int i = 0; i < row.length; i += 2){
			String name = row[i];
			String description = row[i+1];
			String code = columnLanguages.get(i);
			Language l = langCodes.get(code);
			if (l != null){
				if (name.length() > 0){
					station.updateName(l, name);
				}
				if (description.length() > 0){
					station.updateDescription(l, description);
				}
			}
		}
		station.setName(station.findName(SmartDB.getCurrentLanguage()));
		return station;
	}

	private List<String> getLanguageCodes(String[] columns) {
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < columns.length; i ++){
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
