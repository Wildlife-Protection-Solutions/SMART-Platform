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
import java.sql.SQLException;
import java.util.Collection;

import org.hibernate.Session;
import org.wcs.smart.map.raster.GridMetadata;
import org.wcs.smart.query.common.engine.IQueryResult;

/**
 * Result set for gridded queries.
 * 
 * @author Emily
 *
 */
public class GridQueryResult implements IQueryResult {
	
	protected Collection<QueryGridResultItem> data;
	private File lastFile;
	protected boolean isDisposed = false;
	
	protected GridMetadata resultMetadata;
	
		
	 public GridQueryResult(Collection<QueryGridResultItem> data){
		 this.data = data;
	 }
	 
	 public  Collection<QueryGridResultItem> getData(){
		 return this.data;
	 }
	 
	 public void setResultsMetadata(GridMetadata metadata){
		 this.resultMetadata = metadata;
	 }
	 
	 public GridMetadata getMetadata(){
		 return this.resultMetadata;
	 }
	 
	 public void setLastRasterFile(File f){
		 this.lastFile = f;
	 }
	 public File getRasterFile(){
		 return this.lastFile;
	 }

	@Override
	public void dispose(Session session) throws SQLException {
		this.isDisposed = true;
		data = null;
		if (lastFile != null){
			try{
				lastFile.delete();
			}catch (Exception ex){
		
			}
		}
	}
	
	@Override
	public boolean isDisposed(){
		return isDisposed;
	}
}
