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
package org.wcs.smart.plan.report.oda.ui;

import java.util.HashMap;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DataSetParameters;
import org.eclipse.datatools.connectivity.oda.design.DesignFactory;
import org.eclipse.datatools.connectivity.oda.design.ResultSetColumns;
import org.eclipse.datatools.connectivity.oda.design.ResultSetDefinition;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.report.oda.PlanTargetQuery;
import org.wcs.smart.plan.report.oda.SmartPlanDriver;
import org.wcs.smart.report.execute.SmartReportRunner;

/**
 * SMART Plan targets dataset wizard page.
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class SmartPlanTargetDatasetWizardPage extends DataSetWizardPage {

	private Button btnIncludeKids = null;
	
	public SmartPlanTargetDatasetWizardPage(String pageName) {
		super(pageName);
		
	}

	public SmartPlanTargetDatasetWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		
	}

	@Override
	public void createPageCustomControl(Composite arg0) {
		Composite all = new Composite(arg0, SWT.NONE);
		all.setLayout(new GridLayout());
		
		btnIncludeKids = new Button(all, SWT.CHECK);
		btnIncludeKids.setText(Messages.SmartPlanTargetDatasetWizardPage_SubPlansOnlyButtonName);
		btnIncludeKids.setToolTipText(Messages.SmartPlanTargetDatasetWizardPage_SubPlansOnlyButtonTooltip);
		btnIncludeKids.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		
		super.setControl(all);
	}
	
	@Override
	protected DataSetDesign collectDataSetDesign(DataSetDesign design) {
		if (getControl() == null) // page control was never created
			return design; // no editing was done
		
		if (!savePage(design)){
			return null;
		}
		return design;
	}
	
	/**
	 * Saves the user-defined value in this page, and updates the specified
	 * dataSetDesign with the latest design definition.
	 */
	private boolean savePage(DataSetDesign dataSetDesign) {
		// obtain query's current runtime metadata, and maps it to the
		// dataSetDesign
		IConnection customConn = null;
		try {
			// instantiate your custom ODA runtime driver class
			IDriver customDriver = new SmartPlanDriver();

			// obtain and open a live connection
			customConn = customDriver.getConnection(null);
			java.util.Properties connProps = DesignSessionUtil
					.getEffectiveDataSourceProperties(getInitializationDesign()
							.getDataSourceDesign());
			

			// update the data set design with the
			// query's current runtime metadata
			Session s = HibernateManager.openSession();
			try{
				HashMap<String, Object> ctx = new HashMap<String, Object>();
				ctx.put(SmartReportRunner.SESSION_PARAM, s);
				
				customConn.setAppContext(ctx);
				customConn.open(connProps);
				
				updateDesign(dataSetDesign, customConn);	
			}finally{
				s.close();
			}
			
			return true;
			
		} catch (OdaException e) {
			// not able to get current metadata, reset previous derived metadata
			dataSetDesign.setResultSets(null);
			dataSetDesign.setParameters(null);
			SmartPlanPlugIn.displayLog(e.getMessage(), e);
			return false;
		} finally {
			closeConnection(customConn);
		}
	}
	
	
	/**
	 * Updates the given dataSetDesign with the queryText and its derived
	 * metadata obtained from the ODA runtime connection.
	 */
	private void updateDesign(DataSetDesign dataSetDesign, IConnection conn) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(PlanTargetQuery.SMART_PLAN_TARGET_ID);
		String queryText = btnIncludeKids.getSelection()? PlanTargetQuery.SUBPLAN_ONLY:PlanTargetQuery.PLAN_ONLY;
		query.prepare(queryText);
		dataSetDesign.setQueryText(queryText);

		try {
			IResultSetMetaData md = query.getMetaData();
			updateResultSetDesign(md, dataSetDesign);
		} catch (OdaException e) {
			// no result set definition available, reset previous derived
			// metadata
			dataSetDesign.setResultSets(null);
			SmartPlanPlugIn.displayLog(e.getMessage(), e);
		}

		// proceed to get parameter design definition
		try {
			IParameterMetaData paramMd = query.getParameterMetaData();
			updateParameterDesign(paramMd, dataSetDesign);
		} catch (OdaException ex) {
			// no parameter definition available, reset previous derived
			// metadata
			dataSetDesign.setParameters(null);
			SmartPlanPlugIn.displayLog(ex.getMessage(), ex);
		}

		dataSetDesign.setDisplayName("Smart Plan Targets"); //$NON-NLS-1$ 
		dataSetDesign.setName("Smart Plan Targets"); //$NON-NLS-1$
	}
	
	/**
	 * Updates the specified data set design's result set definition based on
	 * the specified runtime metadata.
	 * 
	 * @param md
	 *            runtime result set metadata instance
	 * @param dataSetDesign
	 *            data set design instance to update
	 * @throws OdaException
	 */
	private void updateResultSetDesign(IResultSetMetaData md,
			DataSetDesign dataSetDesign) throws OdaException {
		ResultSetColumns columns = DesignSessionUtil
				.toResultSetColumnsDesign(md);

		ResultSetDefinition resultSetDefn = DesignFactory.eINSTANCE
				.createResultSetDefinition();
		// resultSetDefn.setName( value ); // result set name
		resultSetDefn.setResultSetColumns(columns);

		// no exception in conversion; go ahead and assign to specified
		// dataSetDesign
		dataSetDesign.setPrimaryResultSet(resultSetDefn);
		dataSetDesign.getResultSets().setDerivedMetaData(true);
	}

	/**
	 * Updates the specified data set design's parameter 
	 * definition based on the
	 * specified runtime metadata.
	 * 
	 * @param paramMd
	 *            runtime parameter metadata instance
	 * @param dataSetDesign
	 *            data set design instance to update
	 * @throws OdaException
	 */
	private void updateParameterDesign(IParameterMetaData paramMd,
			DataSetDesign dataSetDesign) throws OdaException {
		
		DataSetParameters paramDesign = DesignSessionUtil
				.toDataSetParametersDesign(
						paramMd,
						DesignSessionUtil
								.toParameterModeDesign(IParameterMetaData.parameterModeIn));

		// no exception in conversion; go ahead and assign to specified
		// dataSetDesign
		dataSetDesign.setParameters(paramDesign);
		if (paramDesign == null)
			return; // no parameter definitions; done with update
		
		paramDesign.setDerivedMetaData(true);
	}

	/**
	 * Attempts to close given ODA connection.
	 */
	private void closeConnection(IConnection conn) {
		try {
			if (conn != null && conn.isOpen())
				conn.close();
		} catch (OdaException e) {
			SmartPlanPlugIn.log(e.getMessage(), e);
		}
	}

}
