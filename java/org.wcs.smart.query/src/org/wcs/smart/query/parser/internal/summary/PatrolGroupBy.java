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
package org.wcs.smart.query.parser.internal.summary;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.util.SmartUtils;

/**
 * A patrol group by option
 * @author egouge
 * @since 1.0.0
 */
public class PatrolGroupBy implements IGroupBy {

	
	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by key of the form
	 * "patrol:<key>:<uniqueid>,<uniqueid>,<uniqueid>"
	 * 
	 * Where <key> is one of the PatrolQueryOption and uniqueid
	 * is a hex encoded uuid or unique string value.
	 * 
	 * @return
	 */
	public static final PatrolGroupBy createGroupBy(String key){
		return new PatrolGroupBy(key);
	}
	
	public String key;
	public PatrolQueryOption option;
	public String[] items;
	
	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by item key of the format "patrol:key:<uuid>,<uuid>..."
	 */
	public PatrolGroupBy(String key){
		this.key = key;
		String[] bits = key.split(":");
		
		option = PatrolQueryOptions.findPatrolQueryOption(bits[1]);
		if (bits.length > 2){
			items = new String[bits.length - 2];
			for (int i = 2; i < bits.length; i ++){
				items[i-2] = bits[i];
			}
		}else{
			items = null;
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		return this.key;
	}
	
	/**
	 * @return the patrol query option
	 */
	public PatrolQueryOption getOption(){
		return this.option;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		if (option.getType() == PatrolQueryOptionType.UUID){
			return GroupByType.BYTE;
		}else if (option.getType() == PatrolQueryOptionType.STRING){
			return GroupByType.STRING;
		}
		return GroupByType.STRING;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		if (this.items != null){
			return option.getValues(session, items);
		}
		List<ListItem> items = option.getAllActiveValues(session);
		return items;		
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) {
		DropItem it = DropItemFactory.INSTANCE.createPatrolGroupByDropItem(option);
		try {
			if (items != null){
				ListItem[] initItems = new ListItem[items.length];
				for (int i = 0; i < initItems.length; i++) {
					byte[] uuid = SmartUtils.decodeHex(items[i]);
					initItems[i] = new ListItem(uuid, option.getName(session, uuid));
				}
				it.initializeData(initItems);
			}
		} catch (Exception ex) {
			QueryPlugIn.displayLog("Could not parse query." + ex.getMessage(),
					ex);
		}
		return it;
	}

}
