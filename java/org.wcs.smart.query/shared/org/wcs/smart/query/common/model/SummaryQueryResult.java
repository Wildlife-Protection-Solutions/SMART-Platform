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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IQueryResult;

/**
 * Class to track the results of a summary query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryQueryResult implements IQueryResult{

	private List<SummaryHeader[]> rowHeaders;
	private List<SummaryHeader[]> columnHeaders;
	
	private List<SummaryHeader> valueHeaders;
	
	private Double[][] data;
	
	private SummaryHeader[][] rowHeaderValues;
	private SummaryHeader[][] columnHeaderValues;
	
	private int[] colheaders;
	private int[] rowheaders;
	
	/**
	 * Creates an empty summary results
	 */
	public SummaryQueryResult(){
		rowHeaders = new ArrayList<SummaryHeader[]>();
		columnHeaders = new ArrayList<SummaryHeader[]>();
		valueHeaders = new ArrayList<SummaryHeader>();
	}
	
	/**
	 * Adds an array of row headers.  These should all
	 * be of the same type (ie have the same key).
	 * 
	 * @param headers the row headers to add
	 */
	public void addRowHeader(SummaryHeader[] headers){
		rowHeaders.add(headers);
		rowheaders = null;
		rowHeaderValues = null;
	}
	/**
	 * 
	 * @return list of row headers
	 */
	public List<SummaryHeader[]> getRowHeaders(){
		return this.rowHeaders;
	}
	
	/**
	 * Adds an array of column headers.  These should all
	 * be of the same type (ie have the same key). 
	 * @param headers
	 */
	public void addColumnHeader(SummaryHeader[] headers){
		columnHeaders.add(headers);
		colheaders = null;
		columnHeaderValues = null;
	}
	
	/**
	 * @return the column headers
	 */
	public List<SummaryHeader[]> getColumnHeaders(){
		return this.columnHeaders;
	}
	
	/**
	 * @return the value headers
	 */
	public List<SummaryHeader> getValueHeaders(){
		return this.valueHeaders;
	}
	
	/**
	 * Adds a value header
	 * @param header the header to add
	 */
	public void addValueHeader(SummaryHeader header){
		valueHeaders.add(header);
		colheaders = null;
		columnHeaderValues = null;
	}
	
	
	
	/**
	 * For each column headers it computes the number
	 * of cells each item in the header should
	 * span.
	 * 
	 * For example if the query contains two column headers and
	 * a single value 
	 * Team: {TEAM_A and TEAM_B} and Station (SA, SB, SC) the
	 * results of the query will update the colheaders
	 * array with the values [3, 1, 1].  The results
	 * table would look similar to :
	 * <pre>
	 * -----------------------------------
	 * TEAM_A          | TEAM_B
	 * -----------------------------------
	 * SA  | SB  | SC  | SA  | SB  | SC  
	 * -----------------------------------
	 * V1  | V1  | V1  | V1  | V1  | V1 
	 * -----------------------------------
	 * </pre> 
	 * 
	 * 
	 */
	private void computeColumnHeaderSize(){
		if (colheaders != null){
			return;
		}
		colheaders = new int[getColumnHeaders().size() + 1];
		int numcols = getValueHeaders().size();
		colheaders[colheaders.length-1] = getValueHeaders().size();				
		for (int i = colheaders.length - 2; i >= 0; i --){
			colheaders[i] = colheaders[i+1] * getColumnHeaders().get(i).length;
			numcols = numcols * getColumnHeaders().get(i).length;
		}

	}
	/**
	 * Similar to computerColHeaderSize() except 
	 * values are not included as they are
	 * never rwo headers.
	 * 
	 */
	private void computeRowHeaderSize(){
		if (rowheaders != null){
			return;
		}
		rowheaders = new int[getRowHeaders().size()];
		if (getRowHeaders().size() > 0){
			rowheaders[rowheaders.length-1] = getRowHeaders().get(getRowHeaders().size() -1).length;
			for (int i = rowheaders.length - 2; i >= 0; i --){
				rowheaders[i] = rowheaders[i+1] * getRowHeaders().get(i).length;
			}
		}
	}
	
	
	/**
	 * Example:
	 * this.addColumnHeader( {TEAM_1, TEAM_2} )
	 * this.addColumnHeader( {SA, SB, SC} )
	 * this.addValueHeader( V1 );
	 * 
	 * thie.getColumnHeaderValues will return
	 * 
	 * [ [TEAM_1, TEAM_1, TEAM_1, TEAM_2, TEAM_2, TEAM_2].
	 *   [SA, SB, SC, SA, SB, SC]
	 *   [V1, V1, V1, V1, V1, V1] ]
	 * 
	 * 
	 * 
	 * 
	 * @return array of array of column headers
	 */
	public SummaryHeader[][] getColumnHeaderValues(){		
		if (columnHeaderValues != null){
			return columnHeaderValues;
		}
		int size = getNumDataColumns();		
		computeColumnHeaderSize();
		columnHeaderValues = new SummaryHeader[getColumnHeaders().size() + 1][size];
		for (int i = 0; i < getColumnHeaders().size(); i ++){
			for (int j = 0; j < size; j ++){
				int divsor = 1;
				if (i < colheaders.length -1){
					divsor = colheaders[i+1];
				}
				int index = (j / divsor) % getColumnHeaders().get(i).length;
				columnHeaderValues[i][j] = getColumnHeaders().get(i)[index];
			}
		}
		for (int j = 0; j < size; j ++){
			columnHeaderValues[columnHeaderValues.length -1][j] = getValueHeaders().get( (j % getValueHeaders().size()) );
		}
		//need to return array of SummaryHeader
		return columnHeaderValues;
	}
	
	/**
	 * See: getColumnHeaderValues
	 * @return
	 */
	public SummaryHeader[][] getRowHeaderValues(){
		if (rowHeaderValues != null){
			return rowHeaderValues;
		}
		
		int size = getNumDataRows();
		computeRowHeaderSize();
		
		rowHeaderValues = new SummaryHeader[size][getRowHeaders().size()];
		for (int i = 0; i < getRowHeaders().size(); i ++){
			for (int j = 0; j < size; j ++){
				int divsor = 1;
				if (i < rowheaders.length -1){
					divsor = rowheaders[i+1];
				}
				int index = (j / divsor) % getRowHeaders().get(i).length;
				rowHeaderValues[j][i] = getRowHeaders().get(i)[index];
			}
		}
		return rowHeaderValues;
	}
	
	
	/**
	 * Sets the summary results data 
	 * 
	 * @param dataResults the summary results data which is a 
	 * map of a summary key to the associated value
	 */
	public void setData(HashMap<SummaryResultKey, Double> dataResults){
		int colSize = getNumDataColumns();
		int rowSize = getNumDataRows();
		computeColumnHeaderSize();
		computeRowHeaderSize();
		
		data = new Double[rowSize][colSize];
			
		//TODO: consider going through results instead
		//of each cell as it will likely be faster
			
		for (int x = 0; x < rowSize; x++){
			String[] rowBys = new String[getRowHeaders().size()];
			int index = 0;
			for(int i = 0; i < getRowHeaders().size(); i++){
				int divsor = 1;
				if (i < rowheaders.length -1){
					divsor = rowheaders[i+1];
				}
				int thisindex = (x / divsor) % getRowHeaders().get(i).length;
				SummaryHeader header = getRowHeaders().get(i)[thisindex];					
				if (header.getIdentifier() == null){
					rowBys[index++] = header.getKey();
				}else{
					rowBys[index++] = header.getKey() + ":" + header.getIdentifier(); //$NON-NLS-1$
				}
			}
				
			
			for (int y = 0; y < colSize; y ++){
				String[] groupBys = new String[getColumnHeaders().size() + getRowHeaders().size()];
				index = 0;
				for (int i = 0; i < getColumnHeaders().size(); i ++){
					int divsor = 1;
					if (i < colheaders.length -1){
						divsor = colheaders[i+1];
					}
					int thisindex = (y / divsor) % getColumnHeaders().get(i).length;
					SummaryHeader header = getColumnHeaders().get(i)[thisindex];
					if (header.getIdentifier() == null){
						groupBys[index++] = header.getKey();
					}else{
						groupBys[index++] = header.getKey() + ":" + header.getIdentifier(); //$NON-NLS-1$
					}
				}
				for (int i = 0; i < rowBys.length; i ++){
					groupBys[index++] = rowBys[i];
				}
					
					
				SummaryHeader val = getValueHeaders().get( y % getValueHeaders().size());
				SummaryResultKey r = new SummaryResultKey(val.getKey(), groupBys);
					
				data[x][y] = dataResults.get(r);
				
			}
		}
	}
	
	/**
	 * @return the data array representing the
	 * results from left to right top to bottom based on
	 * the order the column, row and value headers are 
	 * ordered in the query.
	 */
	public Double[][] getData(){
		return this.data;
	}
	
	/**
	 * 
	 * @return the number of rows in the results
	 * table not including the rows required for the
	 * headers
	 */
	public int getNumDataRows(){
		int numrows = 1;
		for (SummaryHeader[] h:  rowHeaders){
			numrows = numrows * h.length;
		}
		return numrows;
	}
	/**
	 * The number of columns in the results table
	 * not including the columns required for the headers.
	 * 
	 * @return
	 */
	public int getNumDataColumns(){
		int num = valueHeaders.size();
		for (SummaryHeader[] h: columnHeaders){
			num = num * h.length;
		}
		return num;
	}
	
	@Override
	public void dispose(Session session) throws SQLException{
	}
}
