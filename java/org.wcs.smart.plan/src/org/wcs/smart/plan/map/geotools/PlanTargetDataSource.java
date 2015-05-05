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
import java.util.HashMap;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.plan.internal.Messages;
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
public class PlanTargetDataSource extends AbstractDataStore{

	public static final String PLAN_TARGET_TYPE = "PlanTarget"; //$NON-NLS-1$
	
	private Plan plan;
	private Boolean subPlans;
	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	
	public PlanTargetDataSource(Plan plan, Boolean subPlans){
		this.plan = plan;
		this.subPlans = subPlans;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		return new String[]{PLAN_TARGET_TYPE};
	}
	
	/* (non-Javadoc)
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		return new PlanTargetFeatureReader(this.plan, subPlans, getSchema(typeName));
	}

	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		SimpleFeatureType type = schemas.get(typeName);
		if (type == null){
			try {
				if (typeName.equals(PLAN_TARGET_TYPE)) {
					type = createPlanTargetSchema();
				}
			}catch(SchemaException ex){
				throw new IOException(Messages.PlanTargetDataSource_NotSupported + ex.getLocalizedMessage(), ex);
			}
			schemas.put(typeName, type);
		}
		return type;
	}

	public Plan getPlan(){
		return this.plan;
	}
	
	public void updatePlan(Plan plan){
		this.plan = plan;
	}
	
	private SimpleFeatureType createPlanTargetSchema() throws SchemaException{
		String spec = "the_geom:Point:srid=4326,fid:String,targetName:String,targetSummary:String,targetStatusDescription:String,targetStatus:String,planId:String"; //$NON-NLS-1$
		SimpleFeatureType type =  DataUtilities.createType("smart." + PLAN_TARGET_TYPE, spec); //$NON-NLS-1$
		return type;
	}
	
}
