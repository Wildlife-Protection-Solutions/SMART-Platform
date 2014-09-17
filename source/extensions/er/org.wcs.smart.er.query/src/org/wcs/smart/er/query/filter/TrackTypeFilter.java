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
package org.wcs.smart.er.query.filter;

import org.hibernate.Session;
import org.wcs.smart.er.model.MissionTrack.TrackType;
import org.wcs.smart.er.query.ui.dropitems.TrackTypeDropItem;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Track type filter.
 * 
 * @author Emily
 *
 */
public class TrackTypeFilter  implements IFilter {

	public static final String QUERY_KEY = "s:mission:tracktype:"; //$NON-NLS-1$
	
	/**
	 * Creates a track type filter.
	 * 
	 * @return
	 */
	public static TrackTypeFilter createFilter(String value){
		TrackType type = TrackType.valueOf(value.split(":")[3]); //$NON-NLS-1$
		return new TrackTypeFilter(type);
	}

	private TrackType type;
	
	public TrackTypeFilter(TrackType type){
		this.type = type;
	}
	
	/**
	 * Get survey filter type
	 * @return
	 */
	public TrackType getTrackType(){
		return this.type;
	}

	
	@Override
	public String asString() {
		return QUERY_KEY + type.name(); 
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		return new DropItem[]{new TrackTypeDropItem(type)};
	}

}