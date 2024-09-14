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
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributePatrolType;
import org.wcs.smart.patrol.model.PatrolTransportGroup;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.ui.OptionSelectionDialog;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Importer for importing stations.  This importer
 * imports data into memory and NOT the conservation area.  The
 * imported data is available via the getImportedData function.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTransportCsvImporter implements ICsvDataImporter {

	private Object[] importedData;
	private ConservationArea ca;
	
	public PatrolTransportCsvImporter(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Override
	public List<String> getWarnings(){
		return null;
	}
	
	/**
	 * <p>This does not do any validation of keys.  Therefore
	 * whoever uses this information should validate and update
	 * keys as necessary.
	 * 
	 * Any objects with zero uuid are objects that could not be matched
	 * in import file - should attempt to match
	 * these to existing objects, but if can't then they either need to be
	 * created or dropped
	 * </p>
	 * @return the set of patrol transport types imported
	 */
	public Object[] getImportedData(){
		return importedData;
	}
	
	@Override
	public boolean importCsvFile(Path file, char delimiter, boolean headers, Charset cs, IProgressMonitor monitor, Session session) throws Exception {
		if (!Files.exists(file)){
			throw new IOException(MessageFormat.format(Messages.PatrolTransportCsvImporter_ErrorfileNotFound, new Object[]{file.toString()}));
		}
		
		ArrayList<PatrolType> types = new ArrayList<>();
		ArrayList<PatrolTransportType> ttypes = new ArrayList<>();
		ArrayList<PatrolTransportGroup> groups= new ArrayList<>();
		
		List<Icon> icons = IconManager.INSTANCE.getIcons(session,  ca);
		icons.addAll(IconManager.INSTANCE.getSystemIcons(session, ca));
		
		List<PatrolAttribute> customAttributes = QueryFactory.buildQuery(session, PatrolAttribute.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		
		try(CSVReader reader = new CSVReader(new InputStreamReader(Files.newInputStream(file), cs), delimiter)){
			//reading the first line with language codes
			String[] headerRow = reader.readNext();
			List<String> langCodes = getLanguageCodes(headerRow);
			
			Map<String,Language> code2Language = createCode2Language(langCodes);
			if (code2Language == null) {
				return false;
			}
			
			String[] row;
			int line = 2;
			HashMap<String, String> ttypeToptype = new HashMap<>();
			while( (row = reader.readNext()) != null ) {
				if (monitor.isCanceled()) return false;
				if (row.length != headerRow.length) {
					throw new Exception(MessageFormat.format(Messages.PatrolTransportCsvImporter_InvalidLine, new Object[]{line, row.length, headerRow.length}));
				}
				
				if (row[0].equalsIgnoreCase(PatrolTransportCsvExportConfig.TRANSPORTTYPE)) {
					PatrolTransportType type = handleTransportType(row, langCodes, code2Language, line, icons);
					ttypes.add(type);
					ttypeToptype.put(type.getKeyId(), row[6].trim());
				}else if (row[0].equalsIgnoreCase(PatrolTransportCsvExportConfig.TRACKTYPE)) {
					PatrolType ptype = handlePatrolType(row, langCodes, code2Language, line, icons, customAttributes);
					types.add(ptype);
				}else if (row[0].equalsIgnoreCase(PatrolTransportCsvExportConfig.GROUP)) {
					PatrolTransportGroup group = handleTransportGroup(row, langCodes, code2Language, line, icons);
					groups.add(group);
				}
				
				line++;
			}
			
			for (PatrolTransportType ttype : ttypes) {
				String typeKey = ttypeToptype.get(ttype.getKeyId());
				for (PatrolType ptype : types) {
					if (ptype.getKeyId().equalsIgnoreCase(typeKey)) {
						ttype.setPatrolType(ptype);
						ptype.getTransportTypes().add(ttype);
					}
				}
			}
			
			for (PatrolTransportType ttype : ttypes) {
				if (ttype.getPatrolType() == null) {
					PatrolType ptype = new PatrolType();
					ptype.setKeyId(ttypeToptype.get(ttype.getKeyId()));
					ptype.setUuid( UuidUtils.stringToUuid( UuidUtils.ZERO_UUID_STR));
				}
			}
			
			Set<PatrolTransportType> groupsSet = new HashSet<>();
			for (PatrolTransportGroup group : groups) {
				boolean found = false;
				for (PatrolType ptype : types) {
					if (ptype.getKeyId().equalsIgnoreCase(group.getPatrolType().getKeyId())) {
						group.setPatrolType(ptype);
						ptype.getTransportGroups().add(group);
						found = true;
						break;
					}
				}
				if (found) {
					for (PatrolTransportType type : ttypes) {
						if (type.getTransportGroup() != null && type.getTransportGroup().getKeyId().equals(group.getKeyId())) {
							type.setTransportGroup(group);
							group.getTransportTypes().add(type);
							groupsSet.add(type);
						}
					}
				}
				
			}
			for (PatrolTransportType type : ttypes) {
				if (!groupsSet.contains(type)) type.setTransportGroup(null);
			}
			
		}
		
		
		
		if (monitor.isCanceled()) return false;
		this.importedData = new Object[] {types, ttypes};
		
		return true;
	}

	/**
	 * 
	 * @param row
	 * @param langCodes
	 * @return
	 * @throws Exception 
	 */
	private PatrolType handlePatrolType(String[] row, List<String> columnLanguages, 
			Map<String, Language> langCodes, int linenumber, List<Icon> icons, List<PatrolAttribute> attributes) throws Exception {
		
		PatrolType type = new PatrolType();
		type.setConservationArea(SmartDB.getCurrentConservationArea());
		
		type.setKeyId(row[1]);
		
		String icon = row[2];
		for (Icon i : icons) {
			if (i.getKeyId().equalsIgnoreCase(icon)) {
				type.setIcon(i);
				break;
			}
		}
		type.setIsActive(parseBoolean(row[3]));
		
		for (int i = 8; i < row.length; i ++){
			String name = row[i];
			String code = columnLanguages.get(i-3);
			Language l = langCodes.get(code);
			if (l != null){
				if (name.length() > 0){
					type.updateName(l, name);
				}
			}
		}
		type.setName(type.findName(SmartDB.getCurrentLanguage()));
		type.setTransportTypes(new ArrayList<>());
		type.setCustomAttributes(new ArrayList<>());
		type.setTransportGroups(new ArrayList<>());
		
		String[] attributekeys = row[8].split(":"); //$NON-NLS-1$
		for (String key : attributekeys) {
			for (PatrolAttribute pa : attributes) {
				if (pa.getKeyId().equalsIgnoreCase(key)) {
					PatrolAttributePatrolType link = new PatrolAttributePatrolType();
					link.setPatrolAttribute(pa);
					link.setPatrolType(type);
					type.getCustomAttributes().add(link);
				}
			}
		}
		return type;
	}
	
	/**
	 * 
	 * @param row
	 * @param langCodes
	 * @return
	 * @throws Exception 
	 */
	private PatrolTransportGroup handleTransportGroup(String[] row, List<String> columnLanguages, 
			Map<String, Language> langCodes, int linenumber, List<Icon> icons) throws Exception {
		
		PatrolTransportGroup group = new PatrolTransportGroup();
		group.setKeyId(row[1]);
		
		String icon = row[2];
		for (Icon i : icons) {
			if (i.getKeyId().equalsIgnoreCase(icon)) {
				group.setIcon(i);
				break;
			}
		}
		
		for (int i = 8; i < row.length; i ++){
			String name = row[i];
			String code = columnLanguages.get(i-3);
			Language l = langCodes.get(code);
			if (l != null){
				if (name.length() > 0){
					group.updateName(l, name);
				}
			}
		}
		group.setName(group.findName(SmartDB.getCurrentLanguage()));
		group.setTransportTypes(new ArrayList<>());
		
		
		String typeKey = row[6].trim();
		PatrolType temp = new PatrolType();
		temp.setKeyId(typeKey);
		group.setPatrolType(temp);
		
		return group;
	}
	
	/**
	 *  Returns already existing agency for given row record or creates new one if required
	 * @param row
	 * @param langCodes
	 * @return
	 * @throws Exception 
	 */
	private PatrolTransportType handleTransportType(String[] row, List<String> columnLanguages, 
			Map<String, Language> langCodes, int linenumber, List<Icon> icons) throws Exception {
		
		PatrolTransportType type = new PatrolTransportType();
		type.setConservationArea(SmartDB.getCurrentConservationArea());
		
		type.setKeyId(row[1]);
		
		String icon = row[2];
		for (Icon i : icons) {
			if (i.getKeyId().equalsIgnoreCase(icon)) {
				type.setIcon(i);
				break;
			}
		}
		type.setIsActive(parseBoolean(row[3]));
		type.setRequiresPilot(parseBoolean(row[4]));
		
		String typeKey = row[6].trim();
		PatrolType temp = new PatrolType();
		temp.setKeyId(typeKey);
		type.setPatrolType(temp);
		type.setMaxSpeed(Integer.parseInt(row[5]));
		for (int i = 8; i < row.length; i ++){
			String name = row[i];
			String code = columnLanguages.get(i-3);
			Language l = langCodes.get(code);
			if (l != null){
				if (name.length() > 0){
					type.updateName(l, name);
				}
			}
		}
		type.setName(type.findName(SmartDB.getCurrentLanguage()));
	
		String groupKey = row[7].trim();
		PatrolTransportGroup group = new PatrolTransportGroup();
		group.setKeyId(groupKey);
		type.setTransportGroup(group);
		
		return type;
	}

	private boolean parseBoolean(String data) {
		if (data.equalsIgnoreCase("true")) return true; //$NON-NLS-1$
		return false;
	}
	private List<String> getLanguageCodes(String[] columns) {
		List<String> result = new ArrayList<String>();
		for (int i = 3; i < columns.length; i ++){
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
					Messages.PatrolTransportCsvImporter_LanguageSelection,
					MessageFormat.format(Messages.PatrolTransportCsvImporter_LanguageMessage, defaultCode));
			
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
