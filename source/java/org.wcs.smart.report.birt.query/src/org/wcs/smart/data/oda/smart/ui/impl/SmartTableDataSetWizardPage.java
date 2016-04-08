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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTableUtils;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.data.oda.smart.impl.table.TableCategory;
import org.wcs.smart.data.oda.smart.ui.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.birt.query.Activator;

import com.ibm.icu.text.Collator;

/**
 * Wizard table for smart birt table datasets
 * @author egouge
 * @since 1.0.0
 */
public class SmartTableDataSetWizardPage extends DataSetWizardPage {

	public static final String DEFAULT_MESSAGE = Messages.SmartTableDataSetWizardPage_PickSmartTable_Message;

	private TreeViewer smartTables;
	/**
	 * Constructor
	 * 
	 * @param pageName
	 */
	public SmartTableDataSetWizardPage(String pageName) {
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
	public SmartTableDataSetWizardPage(String pageName, String title,
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
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_FILL);

		composite.setLayoutData(gridData);

		Label fieldLabel = new Label(composite, SWT.NONE);
		fieldLabel.setText(Messages.SmartTableDataSetWizardPage_SelectTableName_Label);

		smartTables = new TreeViewer(composite, SWT.BORDER);
		smartTables.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof SmartBirtTable){
					return ((SmartBirtTable)element).getTableShortName(Locale.getDefault());
				}else if (element instanceof TableCategory){
					return ((TableCategory) element).getName();
				}
				return element.toString();
			}
			
			public Image getImage(Object element){
				if (element instanceof TableCategory){
					if (((TableCategory)element).getImage() != null){
						return ((TableCategory) element).getImage().createImage();
					}
				}else if (element instanceof SmartBirtTable){
					Image img = SmartBirtTableUtils.getInstance().getImage(((SmartBirtTable)element));
					if (img == null){
						return Activator.getDefault().getImageRegistry().get(Activator.TABLE_ICON);
					}
					return img;
				}
				return null;
			}
			
		});
		
		smartTables.setContentProvider(new ITreeContentProvider() {
			
			private HashMap<TableCategory, List<SmartBirtTable>> tables;
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof HashMap){
					this.tables = (HashMap<TableCategory, List<SmartBirtTable>>) newInput;
				}else{
					this.tables = null;
				}
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof TableCategory){
					return true;
				}
				return false;
			}
			
			@Override
			public Object getParent(Object element) {
				return null;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				Object[] data = tables.keySet().toArray();
				Arrays.sort(data, new Comparator<Object>() {

					@Override
					public int compare(Object o1, Object o2) {
						return Collator.getInstance().compare(
								((TableCategory)o1).getName(),
								((TableCategory)o2).getName());
					}
				});
				return data;
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof TableCategory){
					List<SmartBirtTable> items = tables.get(parentElement);
					
					Collections.sort(items, new Comparator<SmartBirtTable>() {
						@Override
						public int compare(SmartBirtTable o1, SmartBirtTable o2) {
							return Collator.getInstance().compare(o1.getTableFullName(Locale.getDefault()), o2.getTableFullName(Locale.getDefault()));
						}
					});
					return items.toArray();
				}
				return null;
			}
		});
		

		smartTables.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)smartTables.getControl().getLayoutData()).heightHint = 400;
		smartTables.getControl().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validateData();
			}
		});
		try{
			SmartConnection tempConnection = new TempConnection(HibernateManager.openSession());
			Map<TableCategory, List<SmartBirtTable>> tables = SmartBirtTableUtils.getInstance().getBirtTables(tempConnection);
			smartTables.setInput(tables);
			smartTables.expandAll();
		}catch (Exception ex){
			Activator.log(ex.getLocalizedMessage(), ex);
		}
		
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
	public SmartBirtTable getTable() {
		return (SmartBirtTable) ((IStructuredSelection) smartTables.getSelection())
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
		boolean isValid = true;
		if (smartTables.getSelection().isEmpty()) {
			isValid = false;
		} else {
			Object selection = ((IStructuredSelection) smartTables.getSelection())
					.getFirstElement();
			if (!(selection instanceof SmartBirtTable)) {
				isValid = false;
			}
		}

		if (isValid) {
			setMessage(DEFAULT_MESSAGE);
		} else {
			setMessage(Messages.SmartTableDataSetWizardPage_Error_MustSelectTable, ERROR);
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
	private void savePage(DataSetDesign dataSetDesign) {
		// save user-defined query text
		
		dataSetDesign.setQueryText(getTable().getTableKey());

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
			updateDesign(dataSetDesign, customConn, getTable());

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
	 * Updates the given dataSetDesign with the queryText and its derived
	 * metadata obtained from the ODA runtime connection.
	 */
	private void updateDesign(DataSetDesign dataSetDesign, 
			IConnection conn,
			SmartBirtTable reportTable) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(SmartTableQuery.SMART_DATASET_TYPE);
		query.prepare(reportTable.getTableKey());
		dataSetDesign.setQueryText(reportTable.getTableKey());

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
		dataSetDesign.setDisplayName(reportTable.getTableFullName(Locale.getDefault()));
		dataSetDesign.setName(reportTable.getTableKey());
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
				param.setDefaultScalarValue(Messages.SmartTableDataSetWizardPage_Error_MustLinkReportParameters);
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
			Activator.log(Messages.SmartTableDataSetWizardPage_Error_CouldNoClose + e.getLocalizedMessage(), e);
		}
	}

	
	private class TempConnection extends SmartConnection{
		
		public TempConnection(Session session){
			this.localSession = session;
			appContext = new HashMap<Object, Object>();
			appContext.put(SmartConnection.LOCAL_CONTEXT_VAR, Locale.getDefault());
		}
			
		@Override
		public void openSession() {
		}

		@Override
		public Collection<ConservationArea> getConservationAreas() {
			return SmartDB.getConservationAreaConfiguration()
					.getConservationAreas();
		}

		@Override
		public SmartBirtTable findSmartBirtTable(String queryText)
				throws OdaException {
			return null;
		}

		@Override
		public IQueryResult executeQuery(Query query) throws Exception {
			return null;
		}

		@Override
		protected AbstractSmartBirtQuery createQuery() {
			return null;
		}

		@Override
		public void closeSession() {
			// closed by wizard
		}

		@Override
		public String getDataSourceProductName() {
			return ""; //$NON-NLS-1$
		}
	}
}
