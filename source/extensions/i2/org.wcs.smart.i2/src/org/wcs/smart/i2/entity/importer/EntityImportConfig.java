/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.entity.importer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Entity import configuration.
 * 
 * @author Emily
 *
 */
public class EntityImportConfig {

	private Path file;
	private IntelEntityType type;
	private Map<IntelAttribute, Integer> columnMapping;
	private boolean skipFirstLine = false;
	
	private char delimiter;
	private String dateFormatStr;
	
	public EntityImportConfig(){
		
	}
	
	/**
	 * Sets the file, type and clears any mappings
	 * 
	 * @param file
	 * @param type
	 */
	public void setFile(Path file, IntelEntityType type, boolean skipFirstLine, char delimiter, String dateFormat){
		this.file = file;
		this.type = type;
		this.skipFirstLine = skipFirstLine;
		columnMapping = new HashMap<IntelAttribute, Integer>();
		this.delimiter = delimiter;
		this.dateFormatStr = dateFormat;
	}
	
	public String getDateFormatString(){
		return dateFormatStr;
	}
	
	public char getDelimiter(){
		return this.delimiter;
	}
	
	public Path getFile(){
		return this.file;
	}
	
	public IntelEntityType getType(){
		return this.type;
	}
	
	public boolean skipFirstLine(){
		return this.skipFirstLine;
	}
	public void addMapping(IntelAttribute attribute, int column){
		columnMapping.put(attribute, column);
	}
	
	public Collection<IntelAttribute> getMappedAttributes(){
		return columnMapping.keySet();
	}
	
	public int getMaxColumn(){
		return columnMapping.values().stream().max(Integer::compare).get();
	}
	public int getColumn(IntelAttribute attribute){
		return columnMapping.get(attribute);
	}
}
