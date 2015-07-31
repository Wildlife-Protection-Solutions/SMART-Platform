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
package org.wcs.smart.er.query.ui.filter.summary;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.summary.SamplingUnitGroupBy;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Sampling unit group option.
 * 
 * @author Emily
 *
 */
public class SamplingUnitGroupByViewer extends AbstractGroupByViewer<SamplingUnitGroupBy> implements ISurveyGroupByViewer{

	public SamplingUnitGroupByViewer(SamplingUnitGroupBy gb) {
		super(gb);
	}


	@Override
	public List<ListItem> getItems(Session session) {
		String[] items = groupBy.getRawItems();
		List<ListItem> listItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				try{
					if (it.equals(SamplingUnitFilter.NONE_KEY)){
						listItems.add(new ListItem(null, SamplingUnitFilter.NONE.getId(), null));
					}else{
						SamplingUnit su = (SamplingUnit) session.get(SamplingUnit.class, UuidUtils.stringToUuid(it));
						if (su != null){
							listItems.add(new ListItem(su.getUuid(), su.getId()));
						}
					}
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				}
			}
		}else{
			//all sampling units for associated design
			return null;
		}
		return listItems;
	}
	

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = SurveyDropItemFactory.INSTANCE.createSamplingUnitGroupByDropItem();
		List<ListItem> it = getItems(session);
		if (it != null){
			di.initializeData(it.toArray(new ListItem[it.size()]));
		}
		return di;
	}

	public List<ListItem> getItems(Session session, SurveyDesignFilter filter) {
		if (filter == null){
			//we don't support this
			return null;
		}else{
			if (groupBy.getRawItems() != null){
				return getItems(session);
			}
			
			List<ListItem> items = new ArrayList<ListItem>();
			//get all sampling units associated with survey design
	
			SurveyDesign filterDesign = SurveyHibernateManager.getInstance().getSurveyDesign(filter.getKey(), session);
			if (filterDesign == null){
				return null;
			}
			List<SamplingUnit> objects = SurveyHibernateManager.getInstance()
						.getSamplingUnits(filterDesign, session, null);
			for (SamplingUnit o : objects){
				SamplingUnit su = (SamplingUnit) o;
				items.add(new ListItem(su.getUuid(), su.getId(), null));
			}
			items.add(new ListItem(null, SamplingUnitFilter.NONE.getId(), null));
			return items;
		}
	}

}
