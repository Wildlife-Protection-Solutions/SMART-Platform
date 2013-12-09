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

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.ui.definition.WaypointSourceFilterDropItem;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;
/**
 * Waypoint source filter
 * @author Emily
 *
 */
public class WaypointSourceFilter implements IFilter {

	public static WaypointSourceFilter createFilter(String key, Operator op){
		return new WaypointSourceFilter(key, op);
	}
	
	private String key;
	private Operator op;
	
	public WaypointSourceFilter(String key, Operator op){
		this.key = SmartUtils.stripQuotes(key);
		this.op = op;
	}

	public String getWaypointSourceKey(){
		return this.key;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	@Override
	public String asString() {
		return "wpn:src " + op.asSmartValue() + " \"" + key + "\"";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);

	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		IWaypointSource src = WaypointSourceEngine.getInstance().getSource(key);
		DropItem di;
		if (src == null){
			di = new ErrorDropItem(MessageFormat.format(Messages.WaypointSourceFilter_InvalidSourceFilter, new Object[]{key}));
		}else{
	
			di = new WaypointSourceFilterDropItem();
			di.initializeData(new Object[]{op, src});
		}
		return new DropItem[]{di};
	}

}
