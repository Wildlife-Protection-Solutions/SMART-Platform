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

import java.util.Locale;

import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.patrol.query.ui.IPatrolOptionData;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SharedUtils;

/**
 * Patrol intelligence extension option
 * @author Emily
 *
 */
public class PatrolPlanExtension implements IExtensionOption {

	private	IPatrolQueryOption option;
	private PlanPatrolQueryOptionData data;
	
	public PatrolPlanExtension(IPatrolQueryOption option){
		this.option = option;
		data = new PlanPatrolQueryOptionData(option);
	}
	
	@Override
	public String getName() {
		return option.getGuiName(Locale.getDefault());
	}

	@Override
	public DropItem asDropItem() {
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		
		return it;
//		String id = SharedUtils.stripQuotes((String)value);
//		ListItem listItem = isAnyPlan(id) ? PlanPatrolQueryOptionData.ANY_PATROL_ITEM : PlanHibernateManager.getPlan(session, id);
//		it.initializeData(listItem);
//		return new DropItem[]{it};
	}
	
	public IPatrolQueryOption getOption(){
		return this.option;
	}

	@Override
	public IPatrolOptionData getOptionData() {
		return data;
	}

}
