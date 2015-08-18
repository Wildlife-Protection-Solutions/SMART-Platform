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
package org.wcs.smart.query.model.summary;

import org.wcs.smart.query.model.filter.IGroupByVisitor;

/**
 * Class that represents a category
 * group by class for a summary group by.
 *  
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryGroupBy implements IGroupBy {

	/**
	 * Creates a new category group by of the form:
	 *  <  CATEGORY_GROUP_BY : "category:" < DM_KEY > ":" ( < DM_KEY > ":")* >
	 *  <p>The first DM_KEY is the parent class category hkey.  The
	 *  remaining hkeys must be direct children of the parent
	 *  hkey and are used to filter the results</p>
	 * 
	 * @param key
	 * @return
	 */
	public final static CategoryGroupBy createGroupBy(String key){
		return new CategoryGroupBy(key);
	}
	
	//private String categoryHkey = null;
	private int treeLevel = 0;
	private String[] filterHkeys = null;
	
	/**
	 * @param key
	 */
	protected CategoryGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
//		this.categoryHkey = bits[1];
		this.treeLevel = Integer.parseInt(bits[1]);
		if (bits.length - 2 > 0){
			filterHkeys = new String[bits.length-2];
			for (int i = 2; i < bits.length; i ++){
				filterHkeys[i-2] = bits[i];
			}
		}
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("category:"); //$NON-NLS-1$
//		sb.append(categoryHkey);
		sb.append(treeLevel);
		return sb.toString();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		if (filterHkeys != null){
			for (int i =0; i < filterHkeys.length; i ++){
				sb.append(filterHkeys[i]);
				if (i < filterHkeys.length-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * @return the tree level category
	 */
	public int getTreeLevel(){
		return this.treeLevel;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return GroupByType.KEY;
	}
	
	public void visit(IGroupByVisitor visitor){
		visitor.visit(this);
	}

	public String[] getFilterKeys(){
		return this.filterHkeys;
	}
}
