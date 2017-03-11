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
package org.wcs.smart.i2.record.importer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map.Entry;

import org.wcs.smart.ca.Projection;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;

/**
 * Configuration for importing record from CSV files
 * @author Emily
 *
 */
public class RecordImportConfig {

	public enum Column{
		TITLE (Messages.RecordImportConfig_RecordTitleColumnName), 
		SOURCE (Messages.RecordImportConfig_RecordSourceColumnName),
		NARRATIVE (Messages.RecordImportConfig_RecordNarrativeColumnName),
		SCRATCHPAD (Messages.RecordImportConfig_RecordScratchpadColumnName);
		
		private String gui;
		
		Column(String gui){
			this.gui = gui;
		}
		
		public String getGuiName(){
			return this.gui;
		}
	}
	
	private HashMap<Integer, IntelAttribute> attributeMappings = new HashMap<>();
	private HashMap<Integer, IntelRecordSourceAttribute> attributeSourceMappings = new HashMap<>();
	private HashMap<Integer, Column> columnMappings = new HashMap<>();
	private boolean skipFirstLine = false;
	private char delimiter;
	private Projection projection;
	private Path file;
	private String dateFormatStr;
	
	public RecordImportConfig(){
		
	}

	public Integer getMappedColumn(IntelAttribute a){
		for (Entry<Integer, IntelAttribute> m : attributeMappings.entrySet()){
			if (m.getValue().equals(a)) return m.getKey();
		}
		return null;
	}
	
	public Integer getMappedColumn(IntelRecordSourceAttribute a){
		for (Entry<Integer, IntelRecordSourceAttribute> m : attributeSourceMappings.entrySet()){
			if (m.getValue().equals(a)) return m.getKey();
		}
		if (a.getAttribute() != null){
			return getMappedColumn(a.getAttribute());
		}
		return null;
	}
	
	public Integer getYMappedColumn(IntelAttribute a){
		for (Entry<Integer, IntelAttribute> m : attributeMappings.entrySet()){
			if (m.getValue() == null) continue;
			if (m.getValue().getUuid() == null && m.getValue().getKeyId().equals(a.getKeyId())) return m.getKey();
		}
		return null;
	}
	
	public Integer getYMappedColumn(IntelRecordSourceAttribute a){
		for (Entry<Integer, IntelRecordSourceAttribute> m : attributeSourceMappings.entrySet()){
			if (m.getValue().getAttribute() == null) continue;
			if (m.getValue().getAttribute().getUuid() == null && m.getValue().getAttribute().getKeyId().equals(a.getAttribute().getKeyId())) return m.getKey();
		}
		return null;
	}
	
	public Integer getMappedColumn(Column a){
		for (Entry<Integer, Column> m : columnMappings.entrySet()){
			if (m.getValue().equals(a)) return m.getKey();
		}
		return null;
	}
	
	public void clearColumnMappings(){
		attributeSourceMappings.clear();
		columnMappings.clear();
	}
	
	public void addColumnMapping(Integer col, IntelRecordSourceAttribute attribute){
		attributeSourceMappings.put(col,  attribute);
	}
	
	
	public void addColumnMapping(Integer col, IntelAttribute attribute){
		attributeMappings.put(col,  attribute);
	}
	
	public void addColumnMapping(Integer col, Column column){
		columnMappings.put(col,  column);
	}
	
	public char getDelimiter(){
		return this.delimiter;
	}
	
	public void setDelimiter(char delimiter){
		this.delimiter = delimiter;
	}
	
	public Projection getProjection(){
		return this.projection;
	}
	
	public void setProjection(Projection projection){
		this.projection = projection;
	}

	public boolean skipFirstLine(){
		return this.skipFirstLine;
	}
	
	public void setSkipFileLine(boolean skip){
		this.skipFirstLine = skip;
	}
	
	public void setFile(Path file){
		this.file = file;
	}
	
	public Path getFile(){
		return this.file;
	}
	
	public String getDateFormatString(){
		return dateFormatStr;
	}
	
	public void setDateFormatString(String dateFormat){
		this.dateFormatStr = dateFormat;
	}
}
