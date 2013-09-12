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

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.util.SmartUtils;

/**
 * Smart plan target data source factory.  This is a read only data source.
 * Sources spatial plan target points from a patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PlanTargetDataSourceFactory implements DataStoreFactorySpi{

	public static final Param PLAN_UUID = new Param("planuuid", String.class, Messages.PlanTargetDataSourceFactory_PlanUuidParameterName, true);  //$NON-NLS-1$
	public static final Param SUB_PLANS = new Param("children", Boolean.class, Messages.PlanTargetDataSourceFactory_PlanSubPlanParameterName, true);  //$NON-NLS-1$
	  
	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#canProcess(java.util.Map)
	 */
	@Override
	public boolean canProcess(Map<String, Serializable> params) {
		if (params.containsKey(PLAN_UUID.key) && params.containsKey(SUB_PLANS.key)){
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getDescription()
	 */
	@Override
	public String getDescription() {
		return Messages.PlanTargetDataSourceFactory_DataSourceDescription;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return Messages.PlanTargetDataSourceFactory_DataSourceName;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getParametersInfo()
	 */
	@Override
	public Param[] getParametersInfo() {
		return new Param[]{PLAN_UUID, SUB_PLANS };
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.geotools.factory.Factory#getImplementationHints()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<Key, ?> getImplementationHints() {
		return Collections.EMPTY_MAP;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataStoreFactorySpi#createDataStore(java.util.Map)
	 */
	@Override
	public DataStore createDataStore(Map<String, Serializable> params)
			throws IOException {
		
		List<PlanTarget> pts = new ArrayList<PlanTarget>();
		
		String planUuid = (String)params.get(PLAN_UUID.key);
		Boolean subPlans = (Boolean)params.get(SUB_PLANS.key);;
		
		if (planUuid == null){
			return new PlanTargetDataSource(null, subPlans);
		}
		Session session = HibernateManager.openSession();
		Plan plan = null;
		
		try{
			plan = (Plan)session.load(Plan.class, SmartUtils.decodeHex((String)params.get(PLAN_UUID.key)));
			if (plan == null ){
				throw new IOException(Messages.PlanTargetDataSourceFactory_PlanNotFound);
			}
			
			if (!subPlans){
				//load plan targets
				for(PlanTarget pt : plan.getTargets()){
					pts.add(pt);
				}
			}else{
				List<Plan> toProcess = new ArrayList<Plan>();
				toProcess.addAll(plan.getChildren());
				while(toProcess.size() > 0){
					Plan kid = toProcess.remove(0);
					for(PlanTarget pt : kid.getTargets()){
						pts.add(pt);
					}
					toProcess.addAll(kid.getChildren());
				}
			}
			
		}catch (Exception ex){
			throw new IOException(ex);
		}finally{
			session.close();
		}
		for(PlanTarget pt : pts){
			pt.refreshStatus();
		}
		
		return new PlanTargetDataSource(plan, subPlans);
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataStoreFactorySpi#createNewDataStore(java.util.Map)
	 */
	@Override
	public DataStore createNewDataStore(Map<String, Serializable> arg0)
			throws IOException {
		throw new UnsupportedOperationException(Messages.PlanTargetDataSourceFactory_ReadOnly);
	}

}
