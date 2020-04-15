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
package org.wcs.smart.i2.ui.views.entity.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
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
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Basic entity search panel for entity search view.
 * 
 * @author Emily
 *
 */
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
		txtSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntityAny());
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
		cmbEntityType.getControl().setEnabled(IntelSecurityManager.INSTANCE.canViewEntityAny());

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
		btnSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntityAny());

		Hyperlink saveSearch = toolkit.createHyperlink(bottom, Messages.EntitySearchView_SaveSearchButton, SWT.NONE);
		saveSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		saveSearch.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				view.saveSearch(createBasicSearch());
			}
		});
		saveSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntityAny());
		
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
			@SuppressWarnings("unchecked")
			List<Object> types = (List<Object>) cmbEntityType.getInput();
			List<Object> selections = new ArrayList<>();
			if (search.getEntityTypes() != null){
				
				for (String keyid : search.getEntityTypes()) {
					IntelEntityType t = null;
					for (Object x : types) {
						if ( x instanceof IntelEntityType && ((IntelEntityType)x).getKeyId().equals(keyid)) {
							t = (IntelEntityType) x;
							break;
						}
					}
					if (t != null) {
						selections.add(t);
					}else {
						MessageDialog.openInformation(getShell(), Messages.BasicEntitySearchPanel_NotFound, 
								MessageFormat.format(Messages.BasicEntitySearchPanel_EntityKeyNotFound, 
										keyid ));
					}
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
				types.addAll(EntityTypeManager.INSTANCE.getViewableEntityTypesActiveProfiles(session));
			}

			types.add(0, BASIC_ALLTYPES_OP);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Object currentSelection = cmbEntityType.getStructuredSelection().getFirstElement();
					cmbEntityType.setInput(types);
					if (currentSelection != null && types.contains(currentSelection)) {
						cmbEntityType.setSelection(new StructuredSelection(currentSelection));
					}else {
						cmbEntityType.setSelection(new StructuredSelection(BASIC_ALLTYPES_OP));
					}
				}
			});
			return Status.OK_STATUS;
		}
	}
}
