/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.birt.datasource.ui;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DataSetParameters;
import org.eclipse.datatools.connectivity.oda.design.DesignFactory;
import org.eclipse.datatools.connectivity.oda.design.ParameterDefinition;
import org.eclipse.datatools.connectivity.oda.design.ResultSetColumns;
import org.eclipse.datatools.connectivity.oda.design.ResultSetDefinition;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.IntelBirtDataSource;
import org.wcs.smart.i2.birt.record.RecordDataset;
import org.wcs.smart.i2.internal.Messages;

/**
 * Intelligence record wizard page.  A wizard page with no options. 
 * 
 * @author Emily
 *
 */
public class IntelRecordDetailsWizardPage extends DataSetWizardPage {

	public IntelRecordDetailsWizardPage(String pageName) {
		super(pageName);
	}

	public IntelRecordDetailsWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	@Override
	public void createPageCustomControl(Composite parent) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(Messages.IntelRecordDetailsWizardPage_NoConfOptions);
		setControl(parent);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage
	 * #collectDataSetDesign(org.eclipse.datatools.connectivity.oda.design.
	 * DataSetDesign)
	 */
	@Override
	protected DataSetDesign collectDataSetDesign(DataSetDesign design) {
		if (getControl() == null) // page control was never created
			return design; // no editing was done
		savePage(design);
		return design;
	}
	
	
	/**
	 * Saves the user-defined value in this page, and updates the specified
	 * dataSetDesign with the latest design definition.
	 */
	private void savePage(DataSetDesign dataSetDesign) {
		// obtain query's current runtime metadata, and maps it to the
		// dataSetDesign
		IConnection customConn = null;
		try {
			// instantiate your custom ODA runtime driver class
			IDriver customDriver = new IntelBirtDataSource();

			// obtain and open a live connection
			customConn = customDriver.getConnection(null);
			java.util.Properties connProps = DesignSessionUtil
					.getEffectiveDataSourceProperties(getInitializationDesign()
							.getDataSourceDesign());
			customConn.open(connProps);

			// update the data set design with the
			// query's current runtime metadata
			updateDesign(dataSetDesign, customConn);

		} catch (OdaException e) {
			// not able to get current metadata, reset previous derived metadata
			dataSetDesign.setResultSets(null);
			dataSetDesign.setParameters(null);

			e.printStackTrace();
		} finally {
			closeConnection(customConn);
		}
	}

	public String getDatasetType(){
		return RecordDataset.DATASET_TYPE;
	}
	
	public String getName(){
		return Messages.IntelRecordDetailsWizardPage_Name;
	}
	
	/**
	 * Updates the given dataSetDesign with the queryText and its derived
	 * metadata obtained from the ODA runtime connection.
	 */
	private void updateDesign(DataSetDesign dataSetDesign, IConnection conn) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(getDatasetType());

		try {
			IResultSetMetaData md = query.getMetaData();
			updateResultSetDesign(md, dataSetDesign);
		} catch (OdaException e) {
			// no result set definition available, reset previous derived
			// metadata
			dataSetDesign.setResultSets(null);
			e.printStackTrace();
		}

		// proceed to get parameter design definition
		try {
			IParameterMetaData paramMd = query.getParameterMetaData();
			updateParameterDesign(paramMd, dataSetDesign);
		} catch (OdaException ex) {
			// no parameter definition available, reset previous derived
			// metadata
			dataSetDesign.setParameters(null);
			ex.printStackTrace();
		}

		/*
		 * See DesignSessionUtil for more convenience methods to define a data
		 * set design instance.
		 */
		dataSetDesign.setDisplayName(getName());
		dataSetDesign.setName(getName());
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

		if (paramDesign.getParameterDefinitions().size() > 0) {

			for (ParameterDefinition param : paramDesign
					.getParameterDefinitions()) {
				param.setDefaultScalarValue(Messages.IntelRecordDetailsWizardPage_LinkParameters);
			}
		}
	}

	/**
	 * Attempts to close given ODA connection.
	 */
	private void closeConnection(IConnection conn) {
		try {
			if (conn != null && conn.isOpen())
				conn.close();
		} catch (OdaException e) {
			Intelligence2PlugIn.log(e.getMessage(), e);
		}
	}
}
