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
package org.wcs.smart.patrol.query.model;

import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.model.summary.IValueItem;


/**
 * A representation of a grid query parsed into the various
 * components that make up the query.  The summary query is made up
 * of one or two values and a filter.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PatrolGridQueryDefinition extends GridQueryDefinition {

	
	/**
	 * Creates a new grid query definition
	 * @param valueItem the values 
	 * @param queryFilter the filter
	 */
	public static final PatrolGridQueryDefinition createQuery(IValueItem valueItem, 
			Double gridSize, 
			QueryFilter queryFilter,
			QueryFilter rateFilter,
			ZeroFilterOption zeroOp,
			QueryFilter zeroFilter){
		
		return new PatrolGridQueryDefinition(valueItem, gridSize, queryFilter, rateFilter, zeroOp, zeroFilter);
	}
	
	public enum ZeroFilterOption{
		DEFAULT("default"), //$NON-NLS-1$
		NONE("none"), //$NON-NLS-1$
		CUSTOM("custom"); //$NON-NLS-1$
		
		private String key;
		
		ZeroFilterOption(String key){
			this.key = key;
		}
		
		public String getKey(){
			return this.key;
		}
		
		public static ZeroFilterOption fromKey(String key){
			if (key.equals(DEFAULT.key)) return DEFAULT;
			if (key.equals(NONE.key)) return NONE;
			if (key.equals(CUSTOM.key)) return CUSTOM;
			throw new IllegalArgumentException("No enum constant with key " + ZeroFilterOption.class + "." + key); //$NON-NLS-1$ //$NON-NLS-2$
		}
	};
	
	
	private ZeroFilterOption zeroOp;
	private QueryFilter zeroFilter;
	
	/**
	 * Creates a new summary query definition
	 * @param valueItem
	 * @param gridSize
	 * @param queryFilter
	 */
	protected PatrolGridQueryDefinition (IValueItem valueItem, 
			Double gridSize, 
			QueryFilter queryFilter,
			QueryFilter rateFilter,
			ZeroFilterOption zeroOp,
			QueryFilter zeroFilter){
		super(valueItem, gridSize, queryFilter, rateFilter);
		this.zeroFilter = zeroFilter;
		this.zeroOp = zeroOp;
	}
	
	/**
	 * @return the zero data filter
	 */
	public QueryFilter getZeroDataFilter(){
		return this.zeroFilter;
	}
	
	/**
	 * 
	 * @return the zero data filter option
	 */
	public ZeroFilterOption getZeroDataFilterOption(){
		return this.zeroOp;
	}
	
	/**
	 * @return the string representation of the query
	 */
	public String asQuery(){
		String query = super.asQuery();
		query += "|" + zeroOp.getKey() + "|" + (zeroFilter == null ? "" : zeroFilter.asString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return query;
	}

}
