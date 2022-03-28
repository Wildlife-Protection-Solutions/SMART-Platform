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
package org.wcs.smart.report.birt.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.wcs.smart.report.birt.map.item.LayerItem;

public class BoundsSetting {

	public enum BoundsOption{
		MAP_EXTENTS,
		ALL_QUERY_LAYERS,
		LAYER,
		CUSTOM
	};
	
	private BoundsOption option;
	private ReferencedEnvelope re;
	
	private LayerItem item;
	
	public BoundsSetting() {
		this(BoundsOption.MAP_EXTENTS);
	}
	public BoundsSetting(BoundsOption option) {
		this.option = option;
		this.re = null;
	}
	
	public BoundsSetting(ReferencedEnvelope env) {
		this.option = BoundsOption.CUSTOM;
		this.re = env;
	}
	
	public BoundsOption getOption() {
		return this.option;
	}
	
	public ReferencedEnvelope getEnvelope() {
		return re;
	}
	
	public LayerItem getLayerItem() {
		return item;
	}
	
	public void setLayerItem(LayerItem item) {
		this.item  = item;
	}
	
	public BoundsSetting clone() {
		BoundsSetting clone = new BoundsSetting();
		clone.option = option;
		if (re != null) {
			clone.re = new ReferencedEnvelope(re);
		}
		return clone;		
	}
}
