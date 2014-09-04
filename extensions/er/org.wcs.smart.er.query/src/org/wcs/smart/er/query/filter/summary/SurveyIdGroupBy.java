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
package org.wcs.smart.er.query.filter.summary;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Survey id group by option.
 * 
 * @author egouge
 * 
 */
public class SurveyIdGroupBy implements ISurveyGroupBy {

	public static SurveyIdGroupBy createGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		if (bits.length > 3){
			String items[] = new String[bits.length - 3];
			for (int i = 3; i < bits.length; i ++){
				items[i-3] = bits[i];
			}
			return new SurveyIdGroupBy(items);
		}else{
			return new SurveyIdGroupBy(null);
		}
	}
	
	private String[] items = null;;
	private List<ListItem> allItems;
	
	private SurveyIdGroupBy(String[] items){
		this.items = items;
	}
	
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		if (items != null){
			for (String it : items){
				sb.append(it);
				sb.append(":"); //$NON-NLS-1$
			}
			if (items.length > 0){
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb.toString();
	}

	@Override
	public String getKeyPart() {
		return "sgb:survey:id:"; //$NON-NLS-1$
	}

	@Override
	public GroupByType getType() {
		return GroupByType.BYTE;
	}

	@Override
	public List<ListItem> getItems(Session session) {
		if (allItems != null){
			return allItems;
		}
		
		allItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				try{
					Survey survey = (Survey) session.load(Survey.class, SmartUtils.decodeHex(it));
					if (survey != null){
						allItems.add(new ListItem(survey.getUuid(), survey.getId()));
					}
				}catch (Exception ex){
					EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return allItems;
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		DropItem di = SurveyDropItemFactory.INSTANCE.createSurveyIdGroupByDropItem();
		List<ListItem> items = getItems(session);
		di.initializeData(items.toArray(new ListItem[items.size()]));
		return di;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ListItem> getItems(Session session, SurveyDesignFilter filter) {
		
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		if (filter == null){
			//get all surveys for the current ca
			
			List<Survey> surveys = session.createCriteria(Survey.class, "survey") //$NON-NLS-1$
					.createAlias("survey.surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
					.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.addOrder(Order.asc("sd.keyId")) //$NON-NLS-1$
					.list();
			for (Survey s : surveys){
				ListItem li = new ListItem(s.getUuid(), s.getId() + " [" + s.getSurveyDesign().getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				items.add(li);
			}			
		}else{
			List<Survey> surveys = session.createCriteria(Survey.class, "survey") //$NON-NLS-1$
				.createAlias("survey.surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("sd.keyId", filter.getKey())) //$NON-NLS-1$
				.addOrder(Order.asc("sd.keyId")) //$NON-NLS-1$
				.list();
			for (Survey s : surveys){
				ListItem li = new ListItem(s.getUuid(), s.getId() );
				items.add(li);
			}			
		}
		
		return items;
	}

}
