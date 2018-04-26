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
package org.wcs.smart.patrol.query.parser.internal.summary;

import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * ValueItem for patrol values.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolValueItem implements IValueItem {

	/**
	 * If this is included in the query then no data values 
	 * should be excluded from the results.  Otherwise all
	 * data should be included
	 * 
	 */
	public static final String EXCLUDE_DATA_OPTION = "nodata"; //$NON-NLS-1$

	/**
	 * Creates a patrol value item from a key of the form
	 * patrol:<aggregation>:patrolkey:nodata
	 * 
	 * The :nodata is option and if included then no data days
	 * will be excluded from the query results.
	 * 
	 * @param part
	 * @return
	 */
	public static final PatrolValueItem createItem(String key){
		return new PatrolValueItem(key);
	}
	

	private String key = null;
	private String aggregation = null;
	private PatrolValueOption patrolOp;
	private Boolean includeNoData = false; 
	
	/**
	 * Creates a patrol value item from a key of the form
	 * patrol:<aggregation>:patrolkey:nodata
	 * 
	 * The :nodata is optional. If supplied then we exclude 
	 * days with no data from the value computation.  Nodata only
	 * applies to some patrol value options
	 * 
	 * @param part
	 */
	public PatrolValueItem(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 3; i ++) {
			sb.append(bits[i]);
			sb.append(":"); //$NON-NLS-1$
		}
		this.key = sb.substring(0, sb.length() -1 );
		patrolOp = PatrolQueryOptions.findPatrolValueItem(bits[2]);
		this.aggregation = bits[1];	
		
		this.includeNoData = false;
		if (patrolOp.hasNoDataOption()) {
			if (bits.length > 3) {
				if (bits[3].equalsIgnoreCase(EXCLUDE_DATA_OPTION)) {
					this.includeNoData = true;
				}
			}
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	public String asString(){
		String key = this.key;
		if (patrolOp.hasNoDataOption() && includeNoData()) {
			key += ":" + EXCLUDE_DATA_OPTION; //$NON-NLS-1$
		}
		return key;
	}
	
	/**
	 * @return the patrol value option
	 */
	public PatrolValueOption getPatrolValueOption(){
		return patrolOp;
	}
	
	public boolean includeNoData() {
		return this.includeNoData;
	}
	
	public void setIncludeNoData(boolean includeNoData) {
		this.includeNoData = includeNoData;
	}
	
	public void accept(IValueVisitor visitor){
		visitor.visit(this);
	}
}
