package org.wcs.smart.query.common.model;

import java.io.File;
import java.util.Collection;

import org.wcs.smart.query.common.engine.IQueryResult;


public class GridQueryResult implements IQueryResult {
	
	private Collection<GridResultItem> data;
	private File lastFile;
	 
	protected GridQueryResultMetadata resultMetadata;
	
		
	 public GridQueryResult(Collection<GridResultItem> data){
		 this.data = data;
	 }
	 
	 public  Collection<GridResultItem> getData(){
		 return this.data;
	 }
	 
	 public void setResultsMetadata(GridQueryResultMetadata metadata){
		 this.resultMetadata = metadata;
	 }
	 
	 public GridQueryResultMetadata getMetadata(){
		 return this.resultMetadata;
	 }
	 
	 public void setLastRasterFile(File f){
		 this.lastFile = f;
	 }
	 public File getRasterFile(){
		 return this.lastFile;
	 }
}
