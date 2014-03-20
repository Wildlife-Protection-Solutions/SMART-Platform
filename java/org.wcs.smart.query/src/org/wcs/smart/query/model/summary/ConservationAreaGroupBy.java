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
import org.wcs.smart.util.SmartUtils;

/**
 * Conservation area group by item.
 * 
 * @author Emily
 *
 */
public class ConservationAreaGroupBy implements IGroupBy {

	
	/**
	 * Creates a new conservation  group by of the form:
	 * |   <  CA_GROUP_BY : "ca:" ( < DM_KEY > )? (":" < DM_KEY > )* >
			
	 *  <p>The DM_KEYs are the conservation area uuids used in the filter</p>
	 *  
	 * @param key
	 * @return
	 */
	public final static ConservationAreaGroupBy createGroupBy(String key){
		return new ConservationAreaGroupBy(key);
	}
	
	
	private String[] filterHkeys = null;
	
	/**
	 * @param key
	 */
	protected ConservationAreaGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$

		if (bits.length > 1){
			filterHkeys = new String[bits.length - 1];
			for (int i = 1; i < bits.length; i ++){
				filterHkeys[i-1] = bits[i];
			}
		}
		
	}

	/**
	 * 
	 * @return the keys of the items to include in the group
	 * by; may be null if all items are included
	 */
	public String[] getFilterKeys(){
		return this.filterHkeys;
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("ca:"); //$NON-NLS-1$
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
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return GroupByType.BYTE;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		//get children categories
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
				if (!lookup.contains(SmartUtils.encodeHex(i.getUuid()))){
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
		DropItem it = BasicDropItemFactory.INSTANCE.createConservationAreaGroupByDropItem();
		
		if (filterHkeys != null) {
			ArrayList<ListItem> items = new ArrayList<ListItem>();
			for (int i = 0; i < filterHkeys.length; i++) {
				ListItem li = new ListItem(SmartUtils.decodeHex(filterHkeys[i]), null);
				items.add(li);
			}
			it.initializeData(items);
		}
		return it;
	}
	
	
	public void visit(IGroupByVisitor visitor){
		visitor.visit(this);
	}
	
}