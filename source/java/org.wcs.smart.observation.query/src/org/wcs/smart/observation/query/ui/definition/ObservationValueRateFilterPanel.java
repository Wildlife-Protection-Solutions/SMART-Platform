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
package org.wcs.smart.observation.query.ui.definition;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.parser.internal.parser.Parser;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.ValueRateFilterDeifnitionPanel;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Definition panel that allows for the definition of both
 * value and rate filters.  Used for summary and gridded observation queries.
 * @author Emily
 *
 */
public class ObservationValueRateFilterPanel extends ValueRateFilterDeifnitionPanel {

	public static String ID = "org.wcs.smart.patrol.query.PatrolValueRateFilterPanel"; //$NON-NLS-1$
	
	public ObservationValueRateFilterPanel() {
		super();
	}

	@Override
	public String getId(){
		return ID;
	}
	
	@Override
	protected BasicFilterDefintionPanel createFilterPanel(PanelType type) {
		return new ObservationSimpleFilterPanel(ID + "." + type.name()); //$NON-NLS-1$
	}
	
	
	@Override
	protected void copyValueFilterToRateFilter() {
		String queryString = valueFilter.getQueryPart();
		if (queryString == null || queryString.trim().isEmpty()){
			return;
		}
		
		QueryFilter filterPart  = null;
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			filterPart = parser.QueryFilter();
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.GriddedFilterPanel_CopyError, ex);
			return;
		}
		
		Session session = null;
		try{
			//---- generate drop items for value filter
			session = HibernateManager.openSession();
			session.beginTransaction();
			List<DropItem> copies = new ArrayList<DropItem>();
			if (filterPart != null){
				DropItem[] filterItems = ObservationDropItemFactory.INSTANCE.filterToDropItem(filterPart.getFilter(), session);
				for (int i = 0; i < filterItems.length; i ++){
					copies.add(filterItems[i]);
				}
			}
			session.getTransaction().rollback();
			rateFilter.addItems(copies);
			if (filterPart != null) rateFilter.setFilterType(filterPart.getFilterType());
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.GriddedFilterPanel_CopyError, ex);
			if (session != null && session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}finally{
			if (session != null){
				session.close();
			}
		}
	}


}
