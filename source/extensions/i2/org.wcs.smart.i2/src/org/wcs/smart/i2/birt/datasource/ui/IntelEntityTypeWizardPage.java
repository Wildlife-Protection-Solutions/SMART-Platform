package org.wcs.smart.i2.birt.datasource.ui;

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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.datasource.IntelBirtDataSource;
import org.wcs.smart.i2.birt.entity.EntityDataset;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;

public class IntelEntityTypeWizardPage extends DataSetWizardPage {

	private static final String SELECT_ENTITY_TYPE = "Select entity type";
	private TableViewer lstEntityTypes;
	/**
	 * Constructor
	 * 
	 * @param pageName
	 */
	public IntelEntityTypeWizardPage(String pageName) {
		super(pageName);
		setTitle(pageName);
		setMessage(SELECT_ENTITY_TYPE);
	}

	/**
	 * Constructor
	 * 
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public IntelEntityTypeWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setMessage(SELECT_ENTITY_TYPE);
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

		lstEntityTypes = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		lstEntityTypes.setLabelProvider(EntityTypeLabelProvider.INSTANCE);
		lstEntityTypes.setContentProvider(ArrayContentProvider.getInstance());
		

		lstEntityTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstEntityTypes.getControl().getLayoutData()).heightHint = 300;
		lstEntityTypes.getControl().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validateData();
			}
		});
		List<IntelEntityType> types = null;
		Session s = HibernateManager.openSession();
		try{
			types = s.createCriteria(IntelEntityType.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.list();
		}finally{
			s.close();
		}
		lstEntityTypes.setInput(types);
		
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
			List<IntelEntityType> types = (List<IntelEntityType>) lstEntityTypes.getInput();
			IntelEntityType selection = null;
			for(IntelEntityType t : types){
				if(t.getKeyId().equals(queryText)){
					selection = t;
					break;
				}
			}
			
			if (selection != null) lstEntityTypes.setSelection(new StructuredSelection(selection));
		}
		validateData();
		setMessage(SELECT_ENTITY_TYPE);
	}

	/**
	 * 
	 * @return the user selected query
	 */
	public IntelEntityType getSelectedEntityType() {
		return (IntelEntityType) ((IStructuredSelection) lstEntityTypes.getSelection())
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
		if (lstEntityTypes.getSelection().isEmpty()) {
			isValid = false;
		} else {
			Object selection = ((IStructuredSelection) lstEntityTypes.getSelection())
					.getFirstElement();
			if (!(selection instanceof IntelEntityType)) {
				isValid = false;
			}
		}

		if (isValid) {
			setMessage(SELECT_ENTITY_TYPE);
		} else {
			setMessage("Must select an entity type", ERROR);
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
		
		dataSetDesign.setQueryText(getSelectedEntityType().getKeyId());

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
			updateDesign(dataSetDesign, customConn, getSelectedEntityType());

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
			IntelEntityType entityType) throws OdaException {

		//create dataests
		IQuery query = conn.newQuery(EntityDataset.DATASET_TYPE);
		query.prepare(entityType.getKeyId());
		dataSetDesign.setQueryText(entityType.getKeyId());

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
		dataSetDesign.setDisplayName(entityType.getName());
		dataSetDesign.setName(entityType.getKeyId());
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
				param.setDefaultScalarValue("TODO: Link to Report Parameter");
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

//	
//	private class TempConnection extends SmartConnection{
//		
//		public TempConnection(Session session){
//			this.localSession = session;
//			appContext = new HashMap<Object, Object>();
//			appContext.put(SmartConnection.LOCALE_CONTEXT_VAR, Locale.getDefault());
//		}
//			
//		@Override
//		public void openSession() {
//		}
//
//		@Override
//		public Collection<ConservationArea> getConservationAreas() {
//			return SmartDB.getConservationAreaConfiguration()
//					.getConservationAreas();
//		}
//
//		@Override
//		public SmartBirtTable findSmartBirtTable(String queryText)
//				throws OdaException {
//			return null;
//		}
//
//		@Override
//		public IQueryResult executeQuery(Query query) throws Exception {
//			return null;
//		}
//
//		@Override
//		protected AbstractSmartBirtQuery createQuery() {
//			return null;
//		}
//
//		@Override
//		public void closeSession() {
//			// closed by wizard
//		}
//
//		@Override
//		public String getDataSourceProductName() {
//			return ""; //$NON-NLS-1$
//		}
//	}
}
