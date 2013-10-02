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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.parser.filter.FilterValidator;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.ui.formulaDnd.ErrorDropItem;
import org.wcs.smart.query.xml.model.UuidItemType;

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

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterkeys != null && filterkeys.length > 0){
			for (int i = 0; i < filterkeys.length; i++){
				Area match = HibernateManager.findArea(areaType, filterkeys[i], session);
				if (match != null){
					items.add(new ListItem(null, match.getName(), match.getKeyId()));
				}		
			}
		}else{
			for (Area a : HibernateManager.loadAreas(areaType, session)){
				items.add(new ListItem(null, a.getName(), a.getKeyId()));
			}
		}
		
		return items;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		try{
			DropItem it = DropItemFactory.INSTANCE.createAreaGroupByDropItem(areaType);
			if (filterkeys != null){
				ArrayList<ListItem> inits = new ArrayList<ListItem>();
				for (int i = 0; i < filterkeys.length; i ++){
					Area a = HibernateManager.findArea(areaType, filterkeys[i], session);
					if (a == null){
						throw new Exception(MessageFormat.format(Messages.AreaGroupBy_AreaGroupByNotFound, new Object[]{filterkeys[i], areaType.getGuiName()}));
					}
					inits.add(new ListItem(null, a.getName(), a.getKeyId()));
				}
				it.initializeData(inits);
			}
			return it;
		}catch (Exception ex){
			return new ErrorDropItem(ex.getMessage());
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#hasCategory()
	 */
	public boolean hasCategory(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#hasAttribute()
	 */
	public boolean hasAttribute(){
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#validateAndImport(org.hibernate.Session)
	 */
	public List<String> validateAndImport(String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception{
		//ensure category key exists
		for (int i = 0; i < filterkeys.length; i ++){
			FilterValidator.validateArea(areaType, filterkeys[i], session);
		}
		return null;
		
	}
}
