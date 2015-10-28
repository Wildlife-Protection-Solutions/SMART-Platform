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
package org.wcs.smart.er.query.ui.panels.definition;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.Session;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.filter.QueryFilter;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;
import org.wcs.smart.query.ui.definition.ValueRateFilterDeifnitionPanel;
import org.wcs.smart.query.ui.itempanel.IQueryItemPanel;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Definition panel that allows for the definition of both
 * value and rate filters.  Used for summary and gridded queries.
 * @author Emily
 *
 */
public class SimpleValueRateFilterPanel extends ValueRateFilterDeifnitionPanel {

	public static String ID = "org.wcs.smart.er.survey.definition.filter.valuerate"; //$NON-NLS-1$
	
	@Inject private DefinitionPanelManager pnlManager; 
	
	
	public SimpleValueRateFilterPanel() {
		super();
	}

	@Override
	public String getId(){
		return ID;
	}
	
	@Override
	protected BasicFilterDefintionPanel createFilterPanel(final PanelType type) {
		return new FilterDefintionPanel(false){
			@Override
			public String getId(){
				return SimpleValueRateFilterPanel.ID + "." + type.name(); //$NON-NLS-1$
			}
			
			@Override
			public void refreshPanel(SurveyDesign currentDesign) {
				super.refreshPanel(currentDesign);
				IQueryItemPanel pnl = pnlManager.getQueryItemPanel(getId(), currentQuery.getQueryType());
				if (pnl instanceof ISurveyPanel){
					((ISurveyPanel) pnl).refreshPanel(currentDesign);
				}
			}
		};
	}
	
	
	@Override
	protected void copyValueFilterToRateFilter() {
		String queryString = valueFilter.getQueryPart();
		if (queryString == null || queryString.trim().isEmpty()){
			return;
		}
		Session session = null;
		try{
			QueryFilter filterPart = null;
			try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
				Parser parser = new Parser(is);
				filterPart = parser.QueryFilter();
			}
			//---- generate drop items for value filter
			session = HibernateManager.openSession();
			session.beginTransaction();
			List<DropItem> copies = new ArrayList<DropItem>();
			try{
				if (filterPart != null){
					DropItem[] filterItems = SurveyDropItemFactory.INSTANCE.filterToDropItem(filterPart.getFilter(), (session));
					for (int i = 0; i < filterItems.length; i ++){
						copies.add(filterItems[i]);
					}	
				}
			}finally{
				session.getTransaction().rollback();
			}
			
			//update rate filter
			rateFilter.addItems(copies);
			if (filterPart != null){
				rateFilter.setFilterType(filterPart.getFilterType());
			}
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.SimpleValueRateFilterPanel_CopyError, ex);
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
