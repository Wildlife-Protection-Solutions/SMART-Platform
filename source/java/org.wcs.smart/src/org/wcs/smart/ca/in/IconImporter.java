/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.ca.in;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Import custom icon files from a csv file and directory or files.
 * 
 * @author Emily
 *
 */
public class IconImporter {

	public static final String ICON_KEY_COLUMN = "icon_key"; //$NON-NLS-1$
	public static final String NAME_COLUMN = "name_"; //$NON-NLS-1$
	public static final String ICONSET_COLUMN = "filename_"; //$NON-NLS-1$
	
	public List<String> warnings;
	public List<Icon> icons;
	
	public IconImporter() {
		warnings = new ArrayList<>();
		icons = new ArrayList<>();
	}
	
	public List<String> getWarnings(){
		return this.warnings;
	}
	
	public List<Icon> getIcons(){
		return this.icons;
	}
	
	public void importIcons(ConservationArea ca, Collection<IconSet> iconSets, Path iconFile, Path iconDirectory) throws Exception {
		
		try(FileReader freader = new FileReader(iconFile.toString(), StandardCharsets.UTF_8);
			CSVReader reader = new CSVReader(freader)){
			
		
			int keyColumn = -1;
			HashMap<Language, Integer> nameColumns = new HashMap<>();
			HashMap<IconSet, Integer> setColumns = new HashMap<>();
			
			String[] headers = reader.readNext();
			
			for (int i = 0; i < headers.length; i ++) {
				String header = headers[i];
				if (header.equalsIgnoreCase(ICON_KEY_COLUMN)) {
					keyColumn = i;
				}else if (header.startsWith(NAME_COLUMN)) {
					String code = header.substring(NAME_COLUMN.length());
					Language ll = null;
					for (Language l : ca.getLanguages()) {
						if (l.getCode().equalsIgnoreCase(code)) {
							ll = l;
							break;
						}
					}
					if (ll == null) {
						warnings.add(MessageFormat.format(Messages.IconImporter_LanguageCodeNotFound, code));
					}else {
						nameColumns.put(ll, i);
					}
				}else if (header.startsWith(ICONSET_COLUMN)) {
					String key = header.substring(ICONSET_COLUMN.length());
					
					IconSet sset = null;
					for (IconSet set : iconSets) {
						if (set.getKeyId().equalsIgnoreCase(key)) {
							sset = set;
							break;
						}
					}
					if (sset == null) {
						warnings.add(MessageFormat.format(Messages.IconImporter_IconSetNotFound, key));
					}else {
						setColumns.put(sset, i);
					}
				}
			}
			
			if (keyColumn == -1) {
				throw new Exception(MessageFormat.format(Messages.IconImporter_IconKeyColumnnotFound, ICON_KEY_COLUMN));
			}
			for (IconSet set : iconSets) {
				if (!setColumns.containsKey(set)) {
					warnings.add(MessageFormat.format(Messages.IconImporter_IconFileNotSpecified, set.getName(), ICONSET_COLUMN + set.getKeyId()));
				}
			}
			if (!nameColumns.containsKey(ca.getDefaultLanguage())) {
				warnings.add(MessageFormat.format(Messages.IconImporter_IconNameNotFound, ca.getDefaultLanguage().getDisplayName(), NAME_COLUMN+ ca.getDefaultLanguage().getCode()));
			}
			if (setColumns.isEmpty()) {
				warnings.add(Messages.IconImporter_NoIconFilesFound); 
			}

			
			while((headers = reader.readNext()) != null) {
				
				String key = headers[keyColumn];
				
				Icon icon = new Icon();
				icon.setConservationArea(ca);
				icon.setKeyId(key);
				icon.setFiles( new ArrayList<>() );
				for (Entry<Language, Integer> names : nameColumns.entrySet()) {
					String value = headers[names.getValue()];
					if (!value.isBlank()) icon.updateName(names.getKey(),value);
				}
				if (icon.findNameNull(ca.getDefaultLanguage()) == null) {
					if (icon.getNames().isEmpty()) 
						icon.updateName(ca.getDefaultLanguage(), Messages.IconImporter_DefaultName);
					else
						icon.updateName(ca.getDefaultLanguage(), icon.getNames().iterator().next().getValue());
				}
				icon.setName(icon.findName(SmartDB.getCurrentLanguage()));
				for (Entry<IconSet, Integer> names : setColumns.entrySet()) {
					String filename = headers[names.getValue()];
					Path setFile = iconDirectory.resolve(filename);
					
					if (!Files.exists(setFile)) {
						warnings.add(MessageFormat.format(Messages.IconImporter_IconFileNotFound, setFile.toString(), icon.getKeyId()));
						continue;
					}
					
					IconFile file = new IconFile();
					file.setIcon(icon);
					file.setCopyFromLocation(setFile);
					file.setFilename(setFile.getFileName().toString());
					file.setIconSet(names.getKey());
					
					icon.getFiles().add(file);
				}
				
				icons.add(icon);
			}
		}
	}
	
	public List<String> validate(Collection<Icon> existingIcons) {
		//ensure 
		Set<String> keys = new HashSet<>();
		existingIcons.forEach(e->keys.add(e.getKeyId()));
		
		List<String> errors = new ArrayList<>();
		
		for (Icon newIcon : icons) {
			if (keys.contains(newIcon.getKeyId())) {
				errors.add(MessageFormat.format(Messages.IconImporter_KeyExists, newIcon.getKeyId()));
			}
		}
		
		return errors;
	}
	
	public static void writeSampleFile(ConservationArea ca, List<IconSet> sets, Path outputFile) throws Exception{
		
		try(FileWriter fw = new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8);			
			CSVWriter writer = new CSVWriter(fw)){
				
			List<String> cols = new ArrayList<>();
			List<String> row = new ArrayList<>();
			cols.add(ICON_KEY_COLUMN);
			row.add("unique_icon_key"); //$NON-NLS-1$
			cols.add(NAME_COLUMN + ca.getDefaultLanguage().getCode());
			row.add("(Required) Icon Name For " + ca.getDefaultLanguage().getCode()); //$NON-NLS-1$
			for (Language l : ca.getLanguages()) {
				if (l == ca.getDefaultLanguage()) continue;
				cols.add(NAME_COLUMN + l.getCode());
				row.add("Icon Name For " + l.getCode()); //$NON-NLS-1$
			}
			for (IconSet set : sets) {
				cols.add(ICONSET_COLUMN + set.getKeyId());
				row.add("File for icon set " + set.getName()); //$NON-NLS-1$
			}
					
			writer.writeNext(cols.toArray(new String[cols.size()]));
			writer.writeNext(row.toArray(new String[cols.size()]));	
		}
	}
}
