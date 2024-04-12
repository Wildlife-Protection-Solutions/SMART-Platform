package org.wcs.smart.entity;

import java.util.HashMap;

import org.wcs.smart.ca.Projection;
import org.wcs.smart.entity.model.EntityAttribute;

public class CsvImporterConfig {

	private boolean skipHeader = false;
	
	private Integer idColumn = -1;
	private Integer statusColumn = -1;
	private Integer xColumn = -1;
	private Integer yColumn = -1;
	
	private HashMap<EntityAttribute, Integer> attributeToColumn;
	private Projection projection;
	private String dateFormatString = null;
	
	private char fieldDelimiter = ',';
	
	public CsvImporterConfig(){
		attributeToColumn = new HashMap<EntityAttribute, Integer>();
	}
	
	public void setProjection(Projection proj){
		this.projection = proj;
	}
	
	public void setIdColumn(Integer column){
		this.idColumn = column;
	}
	
	public void setStatusColumn(Integer column){
		this.statusColumn = column;
	}
	
	public void setXColumn(Integer column){
		this.xColumn = column;
	}
	
	public void setYColumn(Integer column){
		this.yColumn = column;
	}
	
	public void setColumn(EntityAttribute ea, Integer column){
		attributeToColumn.put(ea, column);
	}
	
	public Integer getIdColumn(){
		return this.idColumn;
	}
	
	public Integer getStatusColumn(){
		return this.statusColumn;
	}
	public Integer getXColumn(){
		return this.xColumn;
	}
	public Integer getYColumn(){
		return this.yColumn;
	}
	
	public Projection getProjection(){
		return this.projection;
	}
	
	public Integer getColumn(EntityAttribute ea){
		return attributeToColumn.get(ea);
	}

	public void setSkipHeader(boolean selection) {
		this.skipHeader = selection;
	}
	
	public boolean getSkipHeader(){
		return this.skipHeader;
	}
	
	public void setDateFormatString(String dateFormat){
		this.dateFormatString = dateFormat;
	}
	public String getDateFormatString(){
		return this.dateFormatString;
	}
	
	public void setDelimitier(char delimiter){
		this.fieldDelimiter = delimiter;
	}
	
	public char getDelimiter(){
		return this.fieldDelimiter;
	}
}
