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
package org.wcs.smart.patrol.query.parser.internal.summary;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
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

	private PatrolQueryOption option;
	private String[] items;
	
	/**
	 * Creates a new patrol group by option
	 * @param key patrol group by item key of the format "patrol:key:<uuid>:<uuid>..."
	 */
	public PatrolGroupBy(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		option = PatrolQueryOptions.findPatrolQueryOption(bits[1]);
		if (bits.length > 2){
			items = new String[bits.length - 2];
			for (int i = 2; i < bits.length; i ++){
				if (option.getType() == PatrolQueryOptionType.UUID){
					items[i-2] = bits[i];
				}else{
					items[i-2] = SmartUtils.stripQuotes(bits[i]);
				}
			}
		}else{
			items = null;
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("patrol:"); //$NON-NLS-1$
		sb.append(option.getKey());
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
		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				if (option.getType() == PatrolQueryOptionType.STRING){
					sb.append("\""); //$NON-NLS-1$
					sb.append(items[i]);
					sb.append("\""); //$NON-NLS-1$
				}else{
					sb.append(items[i]);
				}
				if (i < items.length - 1) {
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
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
		}else if (option.getType() == PatrolQueryOptionType.KEY){
			return GroupByType.KEY;
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
	public DropItem asDropItem(Session session) throws Exception{
		try {
			DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolGroupByDropItem(option);
			if (items != null){
				ListItem[] initItems = new ListItem[items.length];
				for (int i = 0; i < initItems.length; i++) {
					if (option.getType() == PatrolQueryOptionType.UUID){
						byte[] uuid = SmartUtils.decodeHex(items[i]);
						String name = option.getName(session, uuid);
						if (name == null){
							throw new Exception(MessageFormat.format(Messages.PatrolGroupBy_PatrolOptionParseError, new Object[]{option.getGuiName()}));
						}
						initItems[i] = new ListItem(uuid, option.getName(session, uuid));
					}else{
						initItems[i] = new ListItem(null, items[i], items[i]);
					}
				}
				it.initializeData(initItems);
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(Messages.PatrolGroupBy_CouldNotParse + ex.getLocalizedMessage());
		}
		
	}
	
	/**
	 * 
	 * @return the patrol group by items
	 */
	public String[] getItems(){
		return this.items;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);
	}
}
