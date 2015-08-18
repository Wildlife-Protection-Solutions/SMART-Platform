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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.util.UuidUtils;

/**
 * Conservation area group by item.
 * 
 * @author Emily
 *
 */
public class ConservationAreaGroupByViewer extends AbstractGroupByViewer<ConservationAreaGroupBy> { 

	
	public ConservationAreaGroupByViewer(ConservationAreaGroupBy gb) {
		super(gb);
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		//get children categories
		String[] filterHkeys = groupBy.getFilterKeys();
		List<ListItem> items = new ArrayList<ListItem>();
		for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
		}
		Collections.sort(items);
		
		if (filterHkeys != null){
			HashSet<String> lookup = new HashSet<String>();
			for (String k : filterHkeys){
				lookup.add(k);
			}
			List<ListItem> remove = new ArrayList<ListItem>();
			for (ListItem i : items){
				if (!lookup.contains(UuidUtils.uuidToString(i.getUuid()))){
					remove.add(i);
				}
			}
			items.removeAll(remove);
		}
		
		return items;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		String[] filterHkeys = groupBy.getFilterKeys();
		DropItem it = BasicDropItemFactory.INSTANCE.createConservationAreaGroupByDropItem();
		
		if (filterHkeys != null) {
			ArrayList<ListItem> items = new ArrayList<ListItem>();
			for (int i = 0; i < filterHkeys.length; i++) {
				ListItem li = new ListItem(UuidUtils.stringToUuid(filterHkeys[i]), null);
				items.add(li);
			}
			it.initializeData(items);
		}
		return it;
	}
	
}