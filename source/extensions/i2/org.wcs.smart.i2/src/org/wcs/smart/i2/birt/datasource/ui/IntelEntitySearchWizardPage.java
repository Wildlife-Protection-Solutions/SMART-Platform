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
package org.wcs.smart.i2.birt.datasource.ui;

import java.text.Collator;
import java.util.Collections;
import java.util.List;

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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.IntelBirtDataSource;
import org.wcs.smart.i2.birt.entity.search.EntitySearchDataset;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Wizard page for saved entity searches
 * @author Emily
 *
 */
public class IntelEntitySearchWizardPage extends DataSetWizardPage {

	private TableViewer lstSearches;
	private String message = Messages.IntelQueryWizardPage_message;
	
	/**
	 * Constructor
	 * 
	 * @param pageName
	 */
	public IntelEntitySearchWizardPage(String pageName) {
		super(pageName);
		setTitle(pageName);
		setMessage(message);
	}

	/**
	 * Constructor
	 * 
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public IntelEntitySearchWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setTitle(pageName);
		setMessage(message);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#createPageCustomControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageCustomControl(Composite parent) {
		setControl(createPageControl(parent));
		initializeControl();
	}

	/**
	 * Creates custom control for user-defined query text.
	 */
	private Control createPageControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_FILL);

		composite.setLayoutData(gridData);

		if (!IntelSecurityManager.INSTANCE.canViewEntities()) {
			setPageComplete(false);
			Label l = new Label(composite, SWT.NONE);
			l.setText(Messages.IntelEntitySearchWizardPage_unauthorized); 
			setControl(composite);
			return composite;
		}
		
		lstSearches = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		lstSearches.setLabelProvider(new LabelProvider() {
			public String getText(Object x) {
				if (x instanceof IntelEntitySearch) {
					return ((IntelEntitySearch) x).getName();
				}
				return super.getText(x);
			}
		});
		lstSearches.setContentProvider(ArrayContentProvider.getInstance());
		

		lstSearches.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstSearches.getControl().getLayoutData()).heightHint = 300;
		lstSearches.getControl().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validateData();
			}
		});
		List<IntelEntitySearch> types = null;
		try(Session s = HibernateManager.openSession()){
			types = QueryFactory.buildQuery(s, IntelEntitySearch.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
		}
		
		Collections.sort(types, (a,b) -> Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
		lstSearches.setInput(types);
		
		setPageComplete(false);
		return composite;
	}

	/**
	 * Initializes the page control with the last edited data set design.
	 */
	@SuppressWarnings("unchecked")
	private void initializeControl() {
		/*
		 * To optionally restore the designer state of the previous design
		 * session, use getInitializationDesignerState();
		 */

		// Restores the last saved data set design
		DataSetDesign dataSetDesign = getInitializationDesign();
		if (dataSetDesign == null)
			return; // nothing to initialize

		String queryText = dataSetDesign.getQueryText();
		if (queryText != null && !queryText.isEmpty()){
			List<IntelEntitySearch> types = (List<IntelEntitySearch>) lstSearches.getInput();
			IntelEntitySearch selection = null;
			
			for(IntelEntitySearch t : types){
				String queryKey = UuidUtils.uuidToString(t.getUuid());
				if(queryKey.equals(queryText)){
					selection = t;
					break;
				}
			}
			
			if (selection != null) lstSearches.setSelection(new StructuredSelection(selection));
		}
		validateData();
		setMessage(message);
	}

	/**
	 * 
	 * @return the user selected query
	 */
	public IntelEntitySearch getSelectedSearch() {
		return (IntelEntitySearch) ((IStructuredSelection) lstSearches.getSelection()).getFirstElement();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage
	 * #collectDataSetDesign(org.eclipse.datatools.connectivity.oda.design.
	 * DataSetDesign)
	 */
	protected DataSetDesign collectDataSetDesign(DataSetDesign design) {
		if (getControl() == null) // page control was never created
			return design; // no editing was done
		if (!hasValidData())
			return null; // to trigger a design session error status
		savePage(design);
		return design;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage
	 * #collectResponseState()
	 */
	protected void collectResponseState() {
		super.collectResponseState();
		/*
		 * To optionally assign a custom response state, for inclusion in the
		 * ODA design session response, use setResponseSessionStatus(
		 * SessionStatus status ); setResponseDesignerState( DesignerState
		 * customState );
		 */
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage
	 * #canLeave()
	 */
	protected boolean canLeave() {
		return isPageComplete();
	}

	/**
	 * Validates the user-defined value in the page control exists and not a
	 * blank text. Set page message accordingly.
	 */
	private void validateData() {
		if (lstSearches == null) return;
		boolean isValid = true;
		if (lstSearches.getSelection().isEmpty()) {
			isValid = false;
		} else {
			Object selection = ((IStructuredSelection) lstSearches.getSelection()).getFirstElement();
			if (!(selection instanceof IntelEntitySearch)) {
				isValid = false;
			}
		}

		if (isValid) {
			setMessage(message);
		} else {
			setMessage(Messages.IntelQueryWizardPage_queryRequired, ERROR);
		}

		setPageComplete(isValid);
	}

	/**
	 * Indicates whether the custom page has valid data to proceed with defining
	 * a data set.
	 */
	private boolean hasValidData() {
		validateData();
		return canLeave();
	}

	private String getQueryText() {
		return UuidUtils.uuidToString(getSelectedSearch().getUuid());
	}
	/**
	 * Saves the user-defined value in this page, and updates the specified
	 * dataSetDesign with the latest design definition.
	 */
	private void savePage(DataSetDesign dataSetDesign) {
		// save user-defined query text
		
		dataSetDesign.setQueryText(getQueryText());

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
			updateDesign(dataSetDesign, customConn, getSelectedSearch());

		} catch (OdaException e) {
			// not able to get current metadata, reset previous derived metadata
			dataSetDesign.setResultSets(null);
			dataSetDesign.setParameters(null);

			e.printStackTrace();
		} finally {
			closeConnection(customConn);
		}
	}

	/**
	 * 
	 * @return the type of dataset to create through the birt connection
	 */
	protected String getDatasetType() {
		return EntitySearchDataset.DATASET_TYPE;
	}
	
	/**
	 * The display name for the provided entity type.
	 * @param type
	 * @return
	 */
	protected String getDatasetName(IntelEntitySearch type){
		return type.getName();
	}
	
	/**
	 * Updates the given dataSetDesign with the queryText and its derived
	 * metadata obtained from the ODA runtime connection.
	 */
	private void updateDesign(DataSetDesign dataSetDesign, 
			IConnection conn,
			IntelEntitySearch entityType) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(getDatasetType());
		query.prepare(getQueryText());
		dataSetDesign.setQueryText(getQueryText());
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
		dataSetDesign.setDisplayName(getDatasetName(entityType));
		dataSetDesign.setName(getDatasetName(entityType));
		
		
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
				param.setDefaultScalarValue(Messages.IntelQueryWizardPage_linktoreport1);
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