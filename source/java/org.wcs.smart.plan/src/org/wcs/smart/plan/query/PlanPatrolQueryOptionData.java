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
package org.wcs.smart.plan.query;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Patrol query option data.
 * 
 * @author Emily
 *
 */
public class PlanPatrolQueryOptionData implements IPatrolOptionData{

	public static final ListItem ANY_PATROL_ITEM = new ListItem(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR), 
			Messages.PlanPatrolQueryOption_AnyPlan); 
	
	public PlanPatrolQueryOptionData(){	
	}

	@Override
	public List<ListItem> getValues(Session session, String[] keys) {
		return getAllValues(session);
	}

	@Override
	public List<ListItem> getAllValues(Session session) {
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		
		List<Plan> plans = PlanHibernateManager.getPlans(SmartDB.getCurrentConservationArea(), session);
		Collections.sort(plans, new Comparator<Plan>(){

			@Override
			public int compare(Plan plan1, Plan plan2) {
				int d = -plan1.getStartDate().compareTo(plan2.getStartDate());
				if (d != 0) return d;
				return Collator.getInstance().compare(plan1.getName(), plan2.getName());
			}});
		
		for (Plan plan : plans) {
			items.add(new ListItem(plan.getUuid(), Plan.generateLabel(plan.getId(), plan.getName())));
		}
		
		items.add(0,ANY_PATROL_ITEM);
		return items;
	}

	public ListItem getDefaultListItem() {
		return ANY_PATROL_ITEM;
	}

	@Override
	public boolean isDependOnQueryConfiguration() {
		return false;
	}

}
