/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.observation.query.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.query.ui.definition.WaypointCmGroupByDropItem;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.model.summary.WaypointCmGroupBy;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * @author Emily
 * @since 7.5.7
 *
 */
public class WaypointCmGroupByViewer extends AbstractGroupByViewer<WaypointCmGroupBy> {
	
	
	public WaypointCmGroupByViewer(WaypointCmGroupBy gb) {
		super(gb);
	}

	@Override
	public List<ListItem> getItems(Session session) {
		String[] keys = groupBy.getKeys();
		
		List<ListItem> items = new ArrayList<ListItem>();
		boolean addnull = false;
		if (keys == null){
			//add all items
			List<ConfigurableModel> models = 
					QueryFactory.buildQuery(session, ConfigurableModel.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
					.list();
			addnull = true;
			for(ConfigurableModel cm : models){
				items.add(new ListItem(cm.getUuid(), cm.getName()));
			}
		}else{
			for (String k : keys){
				if (k.equals(IFilter.NULL_OP)) {
					addnull = true;
				}else {
					ConfigurableModel cm = session.get(ConfigurableModel.class, UuidUtils.stringToUuid(k));
					if (cm != null) {
						items.add(new ListItem(cm.getUuid(), cm.getName()));
					}
				}
			}
		}
		Collections.sort(items);
		if (addnull) {
			items.add(0, new ListItem(null, SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(IFilter.NULL_OP, Locale.getDefault()) ,IFilter.NULL_OP));
		}
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		WaypointCmGroupByDropItem dropItem = new WaypointCmGroupByDropItem();
		dropItem.initializeData(getItems(session));
		return dropItem;
	}

}
