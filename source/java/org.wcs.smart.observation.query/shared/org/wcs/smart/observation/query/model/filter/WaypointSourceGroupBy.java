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
package org.wcs.smart.observation.query.model.filter;

import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;
/**
 * Group by for waypoint source field
 * @author Emily
 *
 */
public class WaypointSourceGroupBy implements IGroupBy {

	/**
	 * Creates a new waypoint source group by of the form:
	 *  <  WAYPOINT_SOURCE_GROUP_BY : "wptsrc:" < DM_KEY > ":" ( < DM_KEY > ":")* >
	 *  <p>where each dm key represents a waypoint source key</p>
	 * 
	 * @param key
	 * @return
	 */
	public final static WaypointSourceGroupBy createGroupBy(String key){
		return new WaypointSourceGroupBy(key);
	}
	
	private String[] keys = null;
	
	/**
	 * @param key
	 */
	protected WaypointSourceGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		if (bits.length > 2){
			keys= new String[bits.length-2];
			for (int i = 2; i < bits.length; i ++){
				keys[i-2] = bits[i];
			}
		}
	}
	
	public String[] getKeys(){
		return this.keys;
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		if (keys != null){
			for (int i =0; i < keys.length; i ++){
				sb.append(keys[i]);
				if (i < keys.length-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	@Override
	public String getKeyPart() {
		return "wpt:src"; //$NON-NLS-1$
	}

	@Override
	public GroupByType getType() {
		return GroupByType.KEY;
	}

	
	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);
	}

}
