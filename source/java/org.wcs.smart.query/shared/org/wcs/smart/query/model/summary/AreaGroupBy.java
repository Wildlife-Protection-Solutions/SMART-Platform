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

import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.query.model.filter.IGroupByVisitor;

/**
 * Group by for area option
 * @author Emily
 *
 */
public class AreaGroupBy implements IGroupBy {

	/**
	 * Creates a new category group by of the form:
	 *  <  AREA_GROUPBY_ITEM : "area:" < AREA_TYPE_KEY > ":" ( < DM_KEY > )? (":" < DM_KEY > )* >
	 *  < AREA_TYPE_KEY : ( "CA" | "BA" | "ADMIN" | "MNGT" | "PATRL" ) >
	 * 
	 * 
	 * @param key
	 * @return
	 */
	public final static AreaGroupBy createGroupBy(String key){
		return new AreaGroupBy(key);
	}
	
	private AreaType areaType;
	private String[] filterkeys = null;
	
	/**
	 * @param key
	 */
	protected AreaGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		this.areaType = Area.AreaType.valueOf(bits[1]);
		
		if (bits.length - 2 > 0){
			filterkeys = new String[bits.length-2];
			for (int i = 2; i < bits.length; i ++){
				filterkeys[i-2] = bits[i];
			}
		}
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("area:"); //$NON-NLS-1$
		sb.append(areaType.name());
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
		if (filterkeys != null){
			for (int i =0; i < filterkeys.length; i ++){
				sb.append(filterkeys[i]);
				if (i < filterkeys.length-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	public AreaType getAreaType(){
		return this.areaType;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return GroupByType.KEY;
	}
	
	@Override
	public void visit(IGroupByVisitor visitor){
		visitor.visit(this);
	}
	
	public String[] getAreaFilterKeys(){
		return this.filterkeys;
	}

}
