package org.wcs.smart.i2.ui.views.entity.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.search.IIntelEntitySearch;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntitySearchJob;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.views.EntitySearchResultTable;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

public class BasicEntitySearchPanel extends Composite {

	private static final String BASIC_ALLTYPES_OP = Messages.EntitySearchView_AllTypesOption;


	private EntitySearchView view;

	private FilterComposite txtSearch;
	private TableComboViewer cmbEntityType;
	
	public BasicEntitySearchPanel(Composite parent, EntitySearchView view, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.view = view;

		createContents(toolkit);
	}

	private void createContents(FormToolkit toolkit) {
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		Composite search = toolkit.createComposite(this, SWT.NONE);
		search.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		search.setLayout(new GridLayout());
		((GridLayout) search.getLayout()).marginWidth = 0;
		((GridLayout) search.getLayout()).marginHeight = 0;

		Composite core = toolkit.createComposite(search, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(core, Messages.EntitySearchView_SearchLabel);

		txtSearch = new FilterComposite(core, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				view.doBasicSearch(createBasicSearch(), -1);
			}
		});
		txtSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		toolkit.createLabel(core, Messages.EntitySearchView_EtLabel);

		cmbEntityType = new TableComboViewer(core, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityType.setInput(new String[] { DialogConstants.LOADING_TEXT });
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		SmartUiUtils.configure(cmbEntityType);

		toolkit.adapt(cmbEntityType.getTableCombo());
		cmbEntityType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				view.doBasicSearch(createBasicSearch(),500);
			}
		});
		cmbEntityType.getControl().setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());

		Composite bottom = toolkit.createComposite(search, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout) bottom.getLayout()).marginWidth = 0;
		((GridLayout) bottom.getLayout()).marginHeight = 0;
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button btnSearch = toolkit.createButton(bottom, Messages.EntitySearchView_SerachButton, SWT.PUSH);
		btnSearch.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				view.doBasicSearch(createBasicSearch(), 0);
			}
		});
		btnSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());

		Hyperlink saveSearch = toolkit.createHyperlink(bottom, Messages.EntitySearchView_SaveSearchButton, SWT.NONE);
		saveSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		saveSearch.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				view.saveSearch(createBasicSearch());
			}
		});
		saveSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		refresh();
	}
	
	public void refresh() {
		(new LoadEntityTypeJob()).schedule();
	}

	public void doSearch() {
		view.doBasicSearch(createBasicSearch(), 0);
	}

	public void setSearch(BasicEntitySearch search){
		if (search.getSearchString() != null ){
			txtSearch.setText(search.getSearchString());
			List<Object> types = (List<Object>) cmbEntityType.getInput();
			List<Object> selections = new ArrayList<>();
			if (search.getEntityTypes() != null){
				for (Object t : types){
					if (t instanceof IntelEntityType && search.getEntityTypes().contains(((IntelEntityType)t).getKeyId()))
						selections.add((IntelEntityType)t);
				}
			}
			if (selections.isEmpty()) selections.add(BASIC_ALLTYPES_OP);
			cmbEntityType.setSelection(new StructuredSelection(selections));
		}
	}
	
	/*
	 * Creates a basic search object from the basic search panel
	 */
	private BasicEntitySearch createBasicSearch() {
		List<IntelEntityType> filters = new ArrayList<IntelEntityType>();
		for (Iterator<?> iterator = ((IStructuredSelection) cmbEntityType.getSelection()).iterator(); iterator
				.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelEntityType) {
				filters.add((IntelEntityType) x);
			}

		}
		BasicEntitySearch search = new BasicEntitySearch(txtSearch.getPatternFilter(), filters,
				SmartDB.getCurrentConservationArea());
		return search;
	}

	/*
	 * job for loading entity types
	 */
	private class LoadEntityTypeJob extends Job {

		public LoadEntityTypeJob() {
			super(Messages.EntitySearchView_RefreshJobName);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> types = new ArrayList<Object>();
			try (Session session = HibernateManager.openSession()) {
				types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(session, SmartDB.getCurrentConservationArea()));
			}

			types.add(0, BASIC_ALLTYPES_OP);
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					cmbEntityType.setInput(types);
				}
			});
			return Status.OK_STATUS;
		}
	}
}
