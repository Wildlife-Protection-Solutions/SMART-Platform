/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.intelligence.query.IntelligencePatrolGroupBy;
import org.wcs.smart.intelligence.query.IntelligencePatrolQueryFilter;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.patrol.query.ext.IPatrolContributionFinder;
import org.wcs.smart.plan.query.PlanPatrolQueryFilter;

/**
 * Provides query contributions.  This is byproduct of the desktop
 * rcp extension options.
 * 
 */
public class PatrolContributionFinder implements IPatrolContributionFinder{

	public List<IExtensionFilter> getFilterContributions() {
		ArrayList<IExtensionFilter> items = new ArrayList<IExtensionFilter>();
		items.add(new IntelligencePatrolQueryFilter());
		items.add(new PlanPatrolQueryFilter());
		return items;
	}
	
	public List<IExtensionGroupBy> getGroupByContributions() {
		ArrayList<IExtensionGroupBy> items = new ArrayList<IExtensionGroupBy>();
		items.add(new IntelligencePatrolGroupBy());
		return items;
	}
}
