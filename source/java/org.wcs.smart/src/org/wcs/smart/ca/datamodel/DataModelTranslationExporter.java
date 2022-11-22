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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Exports all data model labels and translations to a csv file
 * with one column that contains the SMART key, and then one column 
 * for each language supported by the Conservation Area 
 * 
 * @author Emily
 *
 */
public class DataModelTranslationExporter {

	public DataModelTranslationExporter() {
	}
	
	/**
	 * Export that values from the data model to a csv file. One column for 
	 * key and other column for each language
	 * 
	 * @param file
	 * @param datamodel
	 * @throws IOException
	 */
	public void export(Path file, DataModel datamodel) throws IOException {
		
		try(FileWriter bw = new FileWriter(file.toFile(),StandardCharsets.UTF_8);
			CSVWriter writer = new CSVWriter(bw)){
			
			List<Language> lang = new ArrayList<>();
			lang.add(datamodel.getConservationArea().getDefaultLanguage());
			for (Language l : datamodel.getConservationArea().getLanguages()) {
				if (l.equals(datamodel.getConservationArea().getDefaultLanguage())) continue;
				lang.add(l);
			}
			
			String[] values = new String[lang.size() + 2];
			values[0] = DataModelTranslationImporter.SMART_KEY_COLUMN;
			values[1] = DataModelTranslationImporter.ICON_KEY_COLUMN;
			for (int i = 0; i < lang.size(); i ++) {
				values[i+2] = lang.get(i).getCode();
			}
			
			writer.writeNext(values);

			//get root categories
			Stack<Category> items = new Stack<>();
			datamodel.getCategories().forEach(c->{
				if (c.getParent() == null) items.add(c);
			});
			
			//process each category
			while(!items.isEmpty()) {
				Category c = items.pop();
				String key = createKey(c);
				writeValues(key, c, lang, writer);
				c.getChildren().forEach(kid->items.push(kid));
			}
			
			//attributes
			List<Attribute> attributes = datamodel.getAttributes();
			for (Attribute a : attributes) {
				String key = createKey(a);
				writeValues(key, a, lang, writer);
			
				if (a.getType().isList()) {
					for (AttributeListItem ali : a.getAttributeList()) {
						key = createKey(a, ali);
						writeValues(key, ali, lang, writer);
					}
				}else if (a.getType() == AttributeType.TREE) {
					Stack<AttributeTreeNode> nodes = new Stack<>();
					nodes.addAll(a.getActiveTreeNodes());
						
					while(!nodes.isEmpty()) {
						AttributeTreeNode node = nodes.pop();
						key = createKey(a, node);
						writeValues(key, node, lang, writer);
						for (AttributeTreeNode kid : node.getChildren()) nodes.push(kid);
					}
				}
			}
		}			
		
	}
	
	private String createKey(Category category) {
		return DataModelTranslationImporter.CATEGORY_KEY + 
				DataModelTranslationImporter.SEP_VALUE + 
				category.getHkey();
	}
	
	private String createKey(Attribute attribute) {
		return  DataModelTranslationImporter.ATTRIBUTE_KEY + 
				DataModelTranslationImporter.SEP_VALUE +  
				attribute.getKeyId();
	}
	
	private String createKey(Attribute attribute, AttributeListItem item) {
		return DataModelTranslationImporter.ATTRIBUTELIST_KEY + 
				DataModelTranslationImporter.SEP_VALUE +  
				attribute.getKeyId() + 
				DataModelTranslationImporter.SEP_VALUE + 
				item.getKeyId();
	}
		
	private String createKey(Attribute attribute, AttributeTreeNode node) {
			return DataModelTranslationImporter.ATTRIBUTENODE_KEY + 
					DataModelTranslationImporter.SEP_VALUE + 
					attribute.getKeyId() +  
					DataModelTranslationImporter.SEP_VALUE + 
					node.getHkey();	
	}
	
	
	private void writeValues(String key, DmObject item, List<Language> langs, CSVWriter writer) {
		String[] values = new String[langs.size() + 2];
		
		values[0] = key;
		values[1] = item.getIcon() == null ? "" : item.getIcon().getKeyId(); //$NON-NLS-1$
		int i  = 2;
		for (Language l : langs) {
			values[i++] = item.findNameNull(l);
		}
		writer.writeNext(values);
	}
}
