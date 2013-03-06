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

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.filter.Operator;
import org.wcs.smart.query.ui.IQueryFilterPatrolContribution;


/**
 * Plan contribution for the Patrol section of a "Query Filter" view.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PlanQueryFilterPatrolContribution implements IQueryFilterPatrolContribution {

	private IPatrolQueryOption planOption = new PlanPatrolQueryOption();
	
	@Override
	public List<IPatrolQueryOption> getOptions() {
		List<IPatrolQueryOption> options = new ArrayList<IPatrolQueryOption>();
		options.add(planOption);
		return options;
	}

	@Override
	public IFilter createBooleanFilter(String key) {
		return null;
	}

	@Override
	public IFilter createStringFilter(String key, Operator op, Object value) {
		if (planOption.getKey().equals(key)) {
			return new PatrolPlanQueryFilter(planOption, op, value);
		}
		return null;
	}
	
}
