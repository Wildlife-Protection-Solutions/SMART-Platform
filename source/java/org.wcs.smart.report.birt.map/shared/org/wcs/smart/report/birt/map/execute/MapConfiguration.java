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
package org.wcs.smart.report.birt.map.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * SMART Birt Map Configuration
 * @author Emily
 *
 */
public class MapConfiguration {

	private List<QueryLayer> results;
	
	public MapConfiguration(){
		results = new ArrayList<QueryLayer>();
	}
	
	public void addQuery(IBaseResultSet qresults, MapLayerInfo info){
		this.results.add( new QueryLayer(qresults, info) );
	}
	
	public int getQueryCount(){
		return results.size();
	}
	
	public IBaseResultSet getQueryResults(int index){
		return results.get(index).results;
	}
	
	public MapLayerInfo getInfo(int index){
		return results.get(index).info;
	}
	
	private class QueryLayer{
		private IBaseResultSet results;
		private MapLayerInfo info;
		
		public QueryLayer(IBaseResultSet results, MapLayerInfo info){
			this.results = results;
			this.info =  info;
		}
	}
	
}
