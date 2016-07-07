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
package org.wcs.smart.intelligence.query.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn;
import org.wcs.smart.intelligence.query.model.FixedQueryColumn.FixedColumns;
import org.wcs.smart.query.model.QueryColumn;


/**
 * Class for managing intelligence query columns.
 * 
 * @author Emily
 *
 */
public class IntelligenceQueryColumnCache {
	private static volatile IntelligenceQueryColumnCache instance = null;
	private static Object INSTANCE_LOCK = new Object();
	
	private volatile QueryColumn[] fixedRecordColumns;
	
	/**
	 * 
	 * @return query cache instance
	 */
	public static IntelligenceQueryColumnCache getInstance(){
		if (instance == null){
			synchronized (INSTANCE_LOCK) {
				if (instance == null){
					instance = new IntelligenceQueryColumnCache();
				}
			}			
		}
		return instance;
	}
	
	/**
	 * 
	 * @return valid columns for intelligence record queries
	 */
	public QueryColumn[] getIntelligenceRecordQueryColumns(){
		
		if (fixedRecordColumns == null){
			synchronized (INSTANCE_LOCK) {
				if (fixedRecordColumns == null){
					List<QueryColumn> cols = new ArrayList<QueryColumn>();
					for (FixedQueryColumn.FixedColumns col : FixedQueryColumn.FixedColumns.values()){
						if (col == FixedColumns.CA_ID || col == FixedColumns.CA_NAME){
							if (SmartDB.isMultipleAnalysis()){
								cols.add(new FixedQueryColumn(col, Locale.getDefault()));		
							}
						}else{
							cols.add(new FixedQueryColumn(col, Locale.getDefault()));
						}
					}
					fixedRecordColumns = cols.toArray(new QueryColumn[cols.size()]);
				}
			}
		}
		return fixedRecordColumns;
	}
}
