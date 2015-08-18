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
package org.wcs.smart.data.oda.smart.ui.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;
import org.wcs.smart.data.oda.smart.ui.internal.Messages;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.QueryListContentProvider;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.query.ui.querylist.SavedQueryTree;
import org.wcs.smart.report.birt.query.Activator;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.ui.SmartReportEditorInput;
import org.wcs.smart.util.UuidUtils;

/**
 * A ODA data set designer page for an user to create a SMART ODA data set
 * design instance. This implementation allows user to select a query from the
 * query tree.
 * 
 */
public class SmartQueryDatasetWizardPage extends DataSetWizardPage {

	public static final String DEFAULT_MESSAGE = Messages.CustomDataSetWizardPage_PickQuery_Message;

	private TreeViewer queryTree;
	private boolean hideUserQueries = false;
	private Job loadQueriesJob = new Job(Messages.CustomDataSetWizardPage_LoadQueryJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(QueryListContentProvider.QUERY_KEY, SavedQueryTree
					.getInstance().getQueries());
			data.put(QueryListContentProvider.FOLDER_KEY, SavedQueryTree
					.getInstance().getFolders());

			
			if (hideUserQueries){
				//shared report should only have shared queries
				List<QueryFolder> folders = (List<QueryFolder>) data.get(QueryListContentProvider.FOLDER_KEY);
				List<QueryFolder> folders2 = new ArrayList<QueryFolder>();
				folders2.addAll(folders);
				for (Iterator<QueryFolder> iterator = folders2.iterator(); iterator.hasNext();) {
					QueryFolder type = (QueryFolder) iterator.next();
					if (type.isRootFolder() && type.getEmployee() != null){
						//remove user folder
						iterator.remove();
					}
				}
				data.put(QueryListContentProvider.FOLDER_KEY, folders2);
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					queryTree.setInput(data);
					
				}
			});

			return Status.OK_STATUS;
		}

	};

	/**
	 * Constructor
	 * 
	 * @param pageName
	 */
	public SmartQueryDatasetWizardPage(String pageName) {
		super(pageName);
		setTitle(pageName);
		setMessage(DEFAULT_MESSAGE);
		
		
	}

	/**
	 * Constructor
	 * 
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public SmartQueryDatasetWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setMessage(DEFAULT_MESSAGE);
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
		IEditorPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activePart != null && activePart.getEditorInput() instanceof SmartReportEditorInput){
			Report r = ((SmartReportEditorInput)activePart.getEditorInput()).getReport();
			hideUserQueries = r.getShared();
		}
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_FILL);

		composite.setLayoutData(gridData);

		Label fieldLabel = new Label(composite, SWT.NONE);
		fieldLabel.setText(Messages.CustomDataSetWizardPage_SelectQuery_Label);

		queryTree = new TreeViewer(composite, SWT.BORDER);
		queryTree.setLabelProvider(new QueryListLabelProvider());
		queryTree.setContentProvider(new QueryListContentProvider(true));
		queryTree.setAutoExpandLevel(2);
		queryTree.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		queryTree.getTree().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validateData();
			}
		});
		loadQueriesJob.schedule();
		setPageComplete(false);
		return composite;
	}

	/**
	 * Initializes the page control with the last edited data set design.
	 */
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
		if (queryText == null)
			return; // nothing to initialize

		// initialize control
		// m_queryTextField.setText( queryText );
		validateData();
		setMessage(DEFAULT_MESSAGE);

		/*
		 * To optionally honor the request for an editable or read-only design
		 * session, use isSessionEditable();
		 */
	}

	/**
	 * 
	 * @return the user selected query
	 */
	public QueryEditorInput getQuery() {
		return (QueryEditorInput) ((IStructuredSelection) queryTree.getSelection())
				.getFirstElement();
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
		if (!savePage(design)){
			return null;
		}
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
		boolean isValid = true;
		if (queryTree.getSelection().isEmpty()) {
			isValid = false;
		} else {
			Object selection = ((IStructuredSelection) queryTree.getSelection())
					.getFirstElement();
			if (!(selection instanceof QueryEditorInput)) {
				isValid = false;
			}
		}

		if (isValid) {
			setMessage(DEFAULT_MESSAGE);
		} else {
			setMessage(Messages.CustomDataSetWizardPage_Error_QueryMustSelected, ERROR);
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

	/**
	 * Saves the user-defined value in this page, and updates the specified
	 * dataSetDesign with the latest design definition.
	 */
	private boolean savePage(DataSetDesign dataSetDesign) {
		// save user-defined query text
		QueryEditorInput query = getQuery();

		// obtain query's current runtime metadata, and maps it to the
		// dataSetDesign
		IConnection customConn = null;
		try {
			// instantiate your custom ODA runtime driver class
			IDriver customDriver = new org.wcs.smart.data.oda.smart.impl.SmartDriver();

			// obtain and open a live connection
			customConn = customDriver.getConnection(null);
			java.util.Properties connProps = DesignSessionUtil
					.getEffectiveDataSourceProperties(getInitializationDesign()
							.getDataSourceDesign());
			customConn.open(connProps);

			// update the data set design with the
			// query's current runtime metadata
			updateDesign(dataSetDesign, customConn, query);
			return true;
			
		} catch (OdaException e) {
			// not able to get current metadata, reset previous derived metadata
			dataSetDesign.setResultSets(null);
			dataSetDesign.setParameters(null);
			Activator.displayLog(e.getMessage(), e);
			return false;
		} finally {
			closeConnection(customConn);
		}
	}

	/**
	 * Updates the given dataSetDesign with the queryText and its derived
	 * metadata obtained from the ODA runtime connection.
	 */
	private void updateDesign(DataSetDesign dataSetDesign, IConnection conn,
			QueryEditorInput smartQuery) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(SmartQuery.SMART_DATASET_TYPE);
		String queryText = getQuery().getType().getKey() + ":" + UuidUtils.uuidToString(getQuery().getUuid()); //$NON-NLS-1$
		query.prepare(queryText);
		dataSetDesign.setQueryText(queryText);

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
		// names can not contain: / \ . ! ; , 
		//See NamePropertyType.isValidName
		String lname = smartQuery.getName();
		lname = lname.replaceAll("[/\\\\.!;,]", "_");  //$NON-NLS-1$//$NON-NLS-2$
		dataSetDesign.setDisplayName(lname + " [" + smartQuery.getId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		dataSetDesign.setName(lname);
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
				param.setDefaultScalarValue(Messages.CustomDataSetWizardPage_LinkToParameters_ToReportParameters);
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
			Activator.log(Messages.CustomDataSetWizardPage_Error_CouldNotCloseConnection + e.getLocalizedMessage(), e);
		}
	}

}
