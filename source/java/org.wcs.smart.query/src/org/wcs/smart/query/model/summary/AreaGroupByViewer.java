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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Group by for area option
 * @author Emily
 *
 */
public class AreaGroupByViewer extends AbstractGroupByViewer<AreaGroupBy> {

	public AreaGroupByViewer(AreaGroupBy gb) {
		super(gb);
	}


	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		try{
			Area.AreaType areaType = groupBy.getAreaType();
			String[] filterkeys = groupBy.getAreaFilterKeys();
			DropItem it = BasicDropItemFactory.INSTANCE.createAreaGroupByDropItem(areaType);
			if (filterkeys != null){
				ArrayList<ListItem> inits = new ArrayList<ListItem>();
				for (int i = 0; i < filterkeys.length; i ++){
					Area a = HibernateManager.findArea(areaType, filterkeys[i], session);
					if (a == null){
						throw new Exception(MessageFormat.format(Messages.AreaGroupBy_AreaGroupByNotFound, new Object[]{filterkeys[i], 
								SmartLabelProvider.getAreaTypeName(areaType)}));
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
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		String[] filterkeys = groupBy.getAreaFilterKeys();
		Area.AreaType areaType = groupBy.getAreaType();
		
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

}
