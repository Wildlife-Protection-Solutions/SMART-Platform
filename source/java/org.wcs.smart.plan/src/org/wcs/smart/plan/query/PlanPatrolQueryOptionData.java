package org.wcs.smart.plan.query;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;

public class PlanPatrolQueryOptionData implements IPatrolOptionData{

	private IPatrolQueryOption option;
	public static final ListItem ANY_PATROL_ITEM = new ListItem(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR), 
			Messages.PlanPatrolQueryOption_AnyPlan); 
	
	public PlanPatrolQueryOptionData(){
		
	}
	
	public PlanPatrolQueryOptionData(IPatrolQueryOption option){
		this.option = option;
	}
	
	@Override
	public void setPatrolOption(IPatrolQueryOption option) {
		this.option = option;
	}

	@Override
	public List<ListItem> getValues(Session session, String[] keys) {
		return getAllActiveValues(session);
	}

	@Override
	public List<ListItem> getAllActiveValues(Session session) {
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

	@Override
	public ListItem getDefaultListItem() {
		return ANY_PATROL_ITEM;
	}

	@Override
	public Image getImage() {
		if (option instanceof PlanPatrolQueryOption){
			return SmartPlanPlugIn.getDefault().getImageRegistry().get(SmartPlanPlugIn.PLAN_ICON);
		}
		return null;
	}

}
