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
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Sampling unit group option.
 * 
 * @author Emily
 *
 */
public class SamplingUnitGroupBy implements ISurveyGroupBy{
	
	/**
	 *  s:samplingunit:" (< UUID >)? (":" < UUID > )* > 
	 * @param key
	 * @return
	 */
	public static SamplingUnitGroupBy createGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
		if (bits.length > 2){
			String items[] = new String[bits.length - 2];
			for (int i = 2; i < bits.length; i ++){
				items[i-2] = bits[i];
			}
			return new SamplingUnitGroupBy(items);
		}else{
			return new SamplingUnitGroupBy(null);
		}
	}
		
	private String[] items;
	
	private SamplingUnitGroupBy(String[] items){
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
		return "sgb:samplingunit:"; //$NON-NLS-1$
	}

	@Override
	public GroupByType getType() {
		return GroupByType.BYTE;
	}

	@Override
	public List<ListItem> getItems(Session session) {
		List<ListItem> listItems = new ArrayList<ListItem>();
		if (items != null){
			for (String it : items){
				try{
					SamplingUnit su = (SamplingUnit) session.load(SamplingUnit.class, SmartUtils.decodeHex(it));
					listItems.add(new ListItem(su.getUuid(), su.getId()));
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
		di.initializeData(it.toArray(new ListItem[it.size()]));
		return di;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);		
	}

	@Override
	public List<ListItem> getItems(Session session, SurveyDesignFilter filter) {
		if (filter == null){
			//we don't support this
			return null;
		}else{
			if (items != null){
				return getItems(session);
			}
			
			List<ListItem> items = new ArrayList<ListItem>();
			//get all sampling units associated with survey
			@SuppressWarnings("unchecked")
			List<SamplingUnit> units = session.createCriteria(SamplingUnit.class, "su") //$NON-NLS-1$
				.createAlias("su.surveyDesign", "sd") //$NON-NLS-1$ //$NON-NLS-2$
				.add(Restrictions.eq("sd.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("sd.keyId", filter.getKey())) //$NON-NLS-1$
				.addOrder(Order.asc("sd.keyId")) //$NON-NLS-1$
				.addOrder(Order.asc("su.id")) //$NON-NLS-1$
				.list();
			for (SamplingUnit unit : units){
				ListItem li = new ListItem(unit.getUuid(), unit.getId());
				items.add(li);
			}			
			return items;
		}
	}

}
