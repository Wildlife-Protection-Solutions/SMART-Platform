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
package org.wcs.smart.plan.map.geotools;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.plan.model.Plan;

/**
 * Geotools data store for SMART plan target layers.
 * <p>Supports both displaying only plan targets associated with the
 * current plan or displaying only plan targets associated with children
 * plans.</p>
 *  
 * @author Emily
 * @since 1.0.0
 */
public class PlanTargetDataSource extends ContentDataStore{

	public static final String PLAN_TARGET_TYPE = "PlanTarget"; //$NON-NLS-1$
	
	private Plan plan;
	private Boolean subPlans;
	
	public PlanTargetDataSource(Plan plan, Boolean subPlans){
		this.plan = plan;
		this.subPlans = subPlans;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	@Override
	public List<Name> createTypeNames(){
		return Collections.singletonList(new NameImpl(PLAN_TARGET_TYPE));
	}
	

	public Plan getPlan(){
		return this.plan;
	}
	
	public void updatePlan(Plan plan){
		this.plan = plan;
	}
	
	public boolean includeSubPlans() {
		return this.subPlans;
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new PlanTargetFeatureSource(entry);
	}
	
}
