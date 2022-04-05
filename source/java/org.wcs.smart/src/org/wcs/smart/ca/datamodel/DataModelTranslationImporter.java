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
package org.wcs.smart.ca.datamodel;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import com.ibm.icu.text.MessageFormat;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Import translations from csv file. 
 * File must be utf-8, there must be one column with the header
 * smart_key, and the other column languages must match a conservation area language
 * 
 * @author Emily
 *
 */
public class DataModelTranslationImporter {

	public static final String SEP_VALUE = ":"; //$NON-NLS-1$
	public static final String ATTRIBUTENODE_KEY = "attributenode"; //$NON-NLS-1$
	public static final String ATTRIBUTELIST_KEY = "attributelist"; //$NON-NLS-1$
	public static final String ATTRIBUTE_KEY = "attribute"; //$NON-NLS-1$
	public static final String CATEGORY_KEY = "category"; //$NON-NLS-1$
	public static final String SMART_KEY_COLUMN = "smart_key"; //$NON-NLS-1$
	
	private List<String> warnings;
	
	private DataModel datamodel;
	private Path file;
	
	/**
	 * Create new importer from given file and data model
	 * @param file
	 * @param datamodel
	 */
	public DataModelTranslationImporter(Path file, DataModel datamodel) {
		this.file = file;
		this.datamodel = datamodel;
	}

	/**
	 * List of warnings generated while processing dataset 
	 * 
	 * @return
	 */
	public List<String> getWarnings(){
		return this.warnings;
	}
				
	/**
	 * Import translations
	 * @throws IOException
	 */
	public void importTranslations() throws IOException {
		warnings = new ArrayList<>();
		
		try(FileReader bw = new FileReader(file.toFile(),StandardCharsets.UTF_8);
			CSVReader reader = new CSVReader(bw)){
			
			String[] header = reader.readNext();
					
			Integer keyColumn = -1;
			HashMap<Language, Integer> langcolumns = new HashMap<>();
					
			for (int i = 0; i < header.length; i ++) { 
				String code = header[i];
				if (code.equals(SMART_KEY_COLUMN)) {
					keyColumn = i;
					continue;
				}
				Language found = null;
				for (Language l : datamodel.getConservationArea().getLanguages()) {
					if (l.getCode().equals(code)) {
						found = l;
					}
				}
				if (found == null) {
					warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_LanguageNotFound, code, datamodel.getConservationArea().getNameLabel()));
				}else {
					langcolumns.put(found, i);
				}
			}
			if (keyColumn == -1) {
				throw new IOException(MessageFormat.format(Messages.DataModelTranslationImporter_KeyColumnNotFound, SMART_KEY_COLUMN));
			}
					
			List<Category> categories = new ArrayList<>();
			ArrayDeque<Category> toVisit = new ArrayDeque<>();
			toVisit.addAll(datamodel.getCategories());
			while(!toVisit.isEmpty()) {
				Category c = toVisit.removeFirst();
				categories.add(c);
				toVisit.addAll(c.getChildren());
			}
			
			List<Attribute> attributes = datamodel.getAttributes();
					
			while( (header = reader.readNext()) != null) {
						
				String key = header[keyColumn];
						
				if (key.startsWith(CATEGORY_KEY + SEP_VALUE)) {
					processCategory(key, header, langcolumns, categories);
				}else if (key.startsWith(ATTRIBUTE_KEY + SEP_VALUE)) {
					processAttribute(key, header, langcolumns,attributes);
				}else if (key.startsWith(ATTRIBUTELIST_KEY + SEP_VALUE)) {
					processAttributeListItem(key, header, langcolumns, attributes);
				}else if (key.startsWith(ATTRIBUTENODE_KEY + SEP_VALUE)) {
					processAttributeTreeNode(key, header, langcolumns, attributes);
				}
			}
		}			
		
	}
	
	private void processAttribute(String key, String[] row, Map<Language, Integer> columns,
			List<Attribute> attributes) {
		String akey = key.split(SEP_VALUE)[1];

		Attribute toUpdate = null;
		for (Attribute a : attributes) {
			if (a.getKeyId().equalsIgnoreCase(akey)) {
				toUpdate = a;
				break;
			}
		}
		if (toUpdate == null) {
			warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_AttributeNotFound, akey, datamodel.getConservationArea().getNameLabel()));
		} else {
			processItem(toUpdate, row, columns);
		}
	}

	private void processAttributeListItem(String key, String[] row, Map<Language, Integer> columns,
			List<Attribute> attributes) {
		String[] bits = key.split(SEP_VALUE);

		String akey = bits[1];
		String lkey = bits[2];

		Attribute toUpdate = null;
		for (Attribute a : attributes) {
			if (a.getKeyId().equalsIgnoreCase(akey)) {
				toUpdate = a;
				break;
			}
		}
		if (toUpdate == null) {
			warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_AttributeNotFound, akey, datamodel.getConservationArea().getNameLabel()));
		} else {
			AttributeListItem itemToUpdate = null;
			for (AttributeListItem ali : toUpdate.getAttributeList()) {
				if (ali.getKeyId().equalsIgnoreCase(lkey)) {
					itemToUpdate = ali;
					break;
				}
			}
			if (itemToUpdate == null) {
				warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_AttributeListItemNotFound, lkey, toUpdate.getName(), datamodel.getConservationArea().getNameLabel()));

			} else {
				processItem(itemToUpdate, row, columns);
			}
		}
	}
		
	
	private void processAttributeTreeNode(String key, String[] row, Map<Language, Integer> columns,
			List<Attribute> attributes) {
		String[] bits = key.split(SEP_VALUE);

		String akey = bits[1];
		String nodekey = bits[2];

		Attribute toUpdate = null;
		for (Attribute a : attributes) {
			if (a.getKeyId().equalsIgnoreCase(akey)) {
				toUpdate = a;
				break;
			}
		}
		if (toUpdate == null) {
			warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_AttributeNotFound, akey, datamodel.getConservationArea().getNameLabel()));
		} else {
			AttributeTreeNode nodeToUpdate = null;
			
			ArrayDeque<AttributeTreeNode> toVisit = new ArrayDeque<>();
			toVisit.addAll(toUpdate.getTree());
			while(!toVisit.isEmpty()) {
				AttributeTreeNode node = toVisit.removeFirst();
				if (node.getHkey().equalsIgnoreCase(nodekey)) {
					nodeToUpdate = node;
					break;
				}
				toVisit.addAll(node.getChildren());
			}
			
			if (nodeToUpdate == null) {
				warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_TreeNodeNotFound, nodekey, toUpdate.getName(), datamodel.getConservationArea().getNameLabel()));
			} else {
				processItem(nodeToUpdate, row, columns);
			}
		}
	}
	
	private void processCategory(String key, String[] row, Map<Language, Integer> columns, List<Category> categories) {
		String hkey = key.split(SEP_VALUE)[1];
		
		Category toUpdate = null;
		for (Category c : categories) {
			if (c.getHkey().equalsIgnoreCase(hkey)) {
				toUpdate = c;
				break;
			}
		}
		if (toUpdate == null) {
			warnings.add(MessageFormat.format(Messages.DataModelTranslationImporter_CategoryNotFound, hkey, datamodel.getConservationArea().getNameLabel()));
		}else {
			processItem(toUpdate, row, columns);
		}
	}
	
	private void processItem(NamedKeyItem item, String[] row, Map<Language, Integer> columns) {
		
		for (Entry<Language, Integer> value : columns.entrySet()) {
			String newValue = row[value.getValue()];
			if(newValue != null && !newValue.isBlank() ) {
				item.updateName(value.getKey(), newValue);
				
				if (datamodel.getConservationArea().equals(SmartDB.getCurrentConservationArea())) {
					//update cached name
					String newName = item.findNameNull(SmartDB.getCurrentLanguage());
					if (newName == null) newName = item.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
					item.setName(newName);
				}
			}
		}
	}
	
	/**
	 * Validates the file for import.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static void validateFile(Path file) throws Exception{

		//can validate the file is in utf-8
		try(FileReader bw = new FileReader(file.toFile(),StandardCharsets.UTF_8);
			CSVReader reader = new CSVReader(bw)){
				
			String[] header = reader.readNext();
			Integer keyColumn = -1;
						
			for (int i = 0; i < header.length; i ++) { 
				String code = header[i];
				if (code.equals(SMART_KEY_COLUMN)) {
					keyColumn = i;
					continue;
				}
			}
			if (keyColumn == -1) {
				throw new IOException(MessageFormat.format(Messages.DataModelTranslationImporter_KeyColumnNotFoundInFile, SMART_KEY_COLUMN, file.toString()));
			}
		}
	}
}
