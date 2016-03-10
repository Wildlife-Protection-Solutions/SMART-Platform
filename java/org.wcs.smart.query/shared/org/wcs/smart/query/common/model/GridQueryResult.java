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
package org.wcs.smart.query.common.model;

import java.io.File;
import java.util.Collection;

import org.wcs.smart.query.common.engine.IQueryResult;

/**
 * Result set for gridded queries.
 * 
 * @author Emily
 *
 */
public class GridQueryResult implements IQueryResult {
	
	protected Collection<GridResultItem> data;
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
