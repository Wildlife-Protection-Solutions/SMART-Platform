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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.summary.MissionIdGroupBy;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.summary.AbstractGroupByViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Mission id group by element.
 * 
 * @author Emily
 *
 */
public class MissionIdGroupByViewer extends AbstractGroupByViewer<MissionIdGroupBy> implements ISurveyGroupByViewer{

	public MissionIdGroupByViewer(MissionIdGroupBy gb) {
		super(gb);
	}

	public MissionIdGroupByViewer(MissionIdGroupBy gb, SurveyDesignFilter filter) {
		super(gb);
	}
	
	@Override
	public List<ListItem> getItems(Session session) {
		String[] items = groupBy.getRawItems();
		List<ListItem> allItems = new ArrayList<ListItem>();
		if (items != null && items.length > 0){
			for (String it : items){
				try{
					Mission m = (Mission) session.load(Mission.class, UuidUtils.stringToUuid(it));
					if (m != null){
						allItems.add(new ListItem(m.getUuid(), m.getId()));
					}
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return allItems;
	}

	@SuppressWarnings("unchecked")
	public List<ListItem> getItems(Session session, SurveyDesignFilter filter) {
		String[] litems = groupBy.getRawItems();
		if (litems != null){
			return getItems(session);
		}
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		if (filter == null){
			//get all surveys for the current ca
			
			List<Mission> missions = session.createCriteria(Mission.class, "m") //$NON-NLS-1$
					.createAlias("m.survey", "s") //$NON-NLS-1$ //$NON-NLS-2$
					.createAlias("survey.surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
					.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.addOrder(Order.asc("sd.keyId")) //$NON-NLS-1$
					.addOrder(Order.desc("startDate")) //$NON-NLS-1$
					.addOrder(Order.desc("sd.startDate")) //$NON-NLS-1$ 
					.list();
			for (Mission m : missions){
				ListItem li = new ListItem(m.getUuid(), m.getId() + " [" + m.getSurvey().getId() + " - " + m.getSurvey().getSurveyDesign().getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				items.add(li);
			}			
		}else{
			List<Mission> missions = session.createCriteria(Mission.class, "m") //$NON-NLS-1$
				.createAlias("m.survey", "s") //$NON-NLS-1$ //$NON-NLS-2$
				.createAlias("survey.surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("sd.keyId", filter.getKey())) //$NON-NLS-1$
				.addOrder(Order.asc("sd.keyId")) //$NON-NLS-1$
				.addOrder(Order.desc("startDate")) //$NON-NLS-1$
				.addOrder(Order.desc("sd.startDate")) //$NON-NLS-1$ 
				.list();
			for (Mission m : missions){
				ListItem li = new ListItem(m.getUuid(), m.getId() + " [" + m.getSurvey().getId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				items.add(li);
			}			
		}
		
		return items;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = SurveyDropItemFactory.INSTANCE.createMissionIdGroupByDropItem();
		List<ListItem> items = getItems(session);
		di.initializeData(items.toArray(new ListItem[items.size()]));
		return di;
	}
}
