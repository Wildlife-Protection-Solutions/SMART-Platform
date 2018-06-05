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
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.summary.SurveyIdGroupBy;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Survey id group by option.
 * 
 * @author egouge
 * 
 */
public class SurveyIdGroupByViewer extends AbstractGroupByViewer<SurveyIdGroupBy> implements ISurveyGroupByViewer{


	public SurveyIdGroupByViewer(SurveyIdGroupBy gb) {
		super(gb);
	}


	@Override
	public List<ListItem> getItems(Session session) {
		String[] items = groupBy.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				try{
					Survey survey = (Survey) session.load(Survey.class, UuidUtils.stringToUuid(it));
					if (survey != null){
						allItems.add(new ListItem(survey.getUuid(), survey.getId()));
					}
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		Collections.sort(allItems);
		return allItems;
	}

	
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = SurveyDropItemFactory.INSTANCE.createSurveyIdGroupByDropItem();
		List<ListItem> items = getItems(session);
		di.initializeData(items.toArray(new ListItem[items.size()]));
		return di;
	}

	public List<ListItem> getItems(Session session, SurveyDesignFilter filter) {
		if (groupBy.getRawItems() != null){
			return getItems(session);
		}
		
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Survey> c = cb.createQuery(Survey.class);
		Root<Survey> from = c.from(Survey.class);
		c.select(from);
		if (filter == null){
			//get all surveys for the current ca
			
			c.where(cb.equal(from.join("surveyDesign").get("conservationArea"), SmartDB.getCurrentConservationArea())); //$NON-NLS-1$ //$NON-NLS-2$
			c.orderBy(cb.asc(from.join("surveyDesign").get("keyId"))); //$NON-NLS-1$ //$NON-NLS-2$
			List<Survey> surveys = session.createQuery(c).getResultList();
			
			for (Survey s : surveys){
				ListItem li = new ListItem(s.getUuid(), s.getId() + " [" + s.getSurveyDesign().getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				items.add(li);
			}			
		}else{
			c.where(cb.and(
					cb.equal(from.join("surveyDesign").get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$ //$NON-NLS-2$
					cb.equal(from.join("surveyDesign").get("keyId"), filter.getKey()))); //$NON-NLS-1$ //$NON-NLS-2$
			c.orderBy(cb.asc(from.join("surveyDesign").get("keyId"))); //$NON-NLS-1$ //$NON-NLS-2$
			List<Survey> surveys = session.createQuery(c).getResultList();
			for (Survey s : surveys){
				ListItem li = new ListItem(s.getUuid(), s.getId() );
				items.add(li);
			}			
		}
		
		return items;
	}

}
