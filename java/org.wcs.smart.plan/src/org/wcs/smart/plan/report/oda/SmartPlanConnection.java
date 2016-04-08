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
package org.wcs.smart.plan.report.oda;

import java.text.MessageFormat;

import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.data.oda.smart.impl.DesktopSmartConnection;
import org.wcs.smart.data.oda.smart.impl.SmartDatasetMetadata;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.report.execute.SmartReportRunner;
/**
 * Connection for the SMART plan driver.
 * 
 * Extends the query SmartConnection;
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class SmartPlanConnection extends DesktopSmartConnection {

	
	@Override
	public void openSession(){
		localSession = (Session) appContext.get(SmartReportRunner.SESSION_PARAM);
	}
	
	@Override
	public void closeSession(){
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
	 */
	@Override
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		if (dataSetType.equals(PlanTargetQuery.SMART_PLAN_TARGET_ID)) {
			return new SmartPlanDatasetMetadata(this);
		}else if(dataSetType.equals(PlanPatrolQuery.SMART_DATASET_TYPE)){
			return new SmartDatasetMetadata(this);
		}
		
		throw new OdaException(
				MessageFormat.format(Messages.SmartPlanConnection_UnsupportedDataset,
						new Object[]{dataSetType}));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	@Override
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(PlanTargetQuery.SMART_PLAN_TARGET_ID)) {
				return new PlanTargetQuery(this);
			}else if (dataSetType.equals(PlanPatrolQuery.SMART_DATASET_TYPE)){
				return new PlanPatrolQuery(this);
			}
			throw new OdaException(MessageFormat.format(Messages.SmartPlanConnection_UnsupportedDataset,new Object[]{dataSetType}));
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}

}
