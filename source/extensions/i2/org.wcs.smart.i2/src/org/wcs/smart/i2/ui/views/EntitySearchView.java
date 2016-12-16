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
package org.wcs.smart.i2.ui.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.search.IIntelEntitySearch;
import org.wcs.smart.i2.search.IntelSearchResultItem;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.ui.EntitySearchJob;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * View for entity search and results
 * @author Emily
 *
 */
public class EntitySearchView {

	/**
	 * context location of entity search results
	 */
	public static final String ENTITY_SEARCH_RESULTS_KEY = "org.wcs.smart.i2.entity.search";
	
	public static final String ID = "org.wcs.smart.i2.view.entitysearch";
	
	private static final int searchDelay = 500;
	
	@Inject
	private IEclipseContext context;
	
	private EntitySearchResultTable entityList;
	private FormToolkit toolkit;
	
	
	private ComboViewer cmbSavedSearch;
	private TableComboViewer cmbEntityType;
	private FilterComposite txtSearch;
	private Font boldFont;
	private Font hlFont;
	
	private LoadEntityTypeJob entityTypeJob = new LoadEntityTypeJob();
	private LoadSavedSearchJob savedSearches = new LoadSavedSearchJob();

	
	private Hyperlink basicSearch;
	private Hyperlink advancedSearch;
	private Hyperlink savedSearch;
	
	private StackLayout searchStack;
	private Composite searchArea ;
	
	final private EntitySearchJob searchJob = new EntitySearchJob() {
		
		@Override
		public void beforeSearch(IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					entityList.setEntities(null);
				}
			});
			monitor.done();
		}
		
		@Override
		public void afterSearch(IntelSearchResult entities, IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					context.getParent().set(ENTITY_SEARCH_RESULTS_KEY, entities);
					entityList.setEntities(entities);
				}
			});
			monitor.done();
			
		}
		
		@Override
		public void onError(Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					entityList.setSearchError(ex);
				}
			});
		}
	};
	
	
	
	public EntitySearchView() {
		super();
	}

	public List<IntelSearchResultItem> getEntities(){
		return this.entityList.getEntities();
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		//we seem to need this one and the one at the end to get this view to show on top of the other views in the perspective stacks
		//I am sure there must be a better way...
		context.get(EPartService.class).activate(context.get(MPart.class), true);	
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		toolkit = new FormToolkit(parent.getDisplay());
		parent = toolkit.createComposite(parent);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(new GridLayout());
		
		createHeaderOptions(parent);
		
		searchArea = toolkit.createComposite(parent);
		searchArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		searchStack = new StackLayout();
		searchArea.setLayout(searchStack);
		
		createBasicSearch(searchArea);
		createAdvancedSearch(searchArea);
		createSavedSearch(searchArea);
		
		searchStack.topControl = searchArea.getChildren()[0];
	
		//spacer
		Label l = toolkit.createLabel(parent, "", SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		entityList = new EntitySearchResultTable(parent, toolkit, context);
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		entityTypeJob.schedule();
		savedSearches.schedule();
		
		doBasicSearch(0);

		context.get(EPartService.class).activate(context.get(MPart.class), true);
	}

	
	private void updateHyperlink(HyperlinkEvent e){
		basicSearch.setFont(hlFont);
		advancedSearch.setFont(hlFont);
		savedSearch.setFont(hlFont);
		
		((Hyperlink)e.widget).setFont(boldFont);
		
		if (e.widget == basicSearch){
			searchStack.topControl = searchArea.getChildren()[0];
		}else if (e.widget == advancedSearch){
			searchStack.topControl = searchArea.getChildren()[1];
		}else if (e.widget == savedSearch){
			searchStack.topControl = searchArea.getChildren()[2];
		}
		searchArea.layout();
		
		basicSearch.getParent().layout();
	}
	
	private void createHeaderOptions(Composite parent){
		Composite header = toolkit.createComposite(parent, SWT.NONE);
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		basicSearch = toolkit.createHyperlink(header, "Basic Search", SWT.NONE);
		basicSearch.setToolTipText("searches all entity identifiers and entity type attributes flagged as include in basic search");
		advancedSearch = toolkit.createHyperlink(header, "Advanced Search", SWT.NONE);
		savedSearch = toolkit.createHyperlink(header, "Saved Search", SWT.NONE);
		IHyperlinkListener hlistener = new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				updateHyperlink(e);
			}
		};
		basicSearch.addHyperlinkListener(hlistener);
		advancedSearch.addHyperlinkListener(hlistener);
		savedSearch.addHyperlinkListener(hlistener);
		
		hlFont = basicSearch.getFont();
		FontData fd = basicSearch.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		
		basicSearch.setFont(boldFont);
	}
	
	private void createSavedSearch(Composite parent){

		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(core, "Saved Search:");
		
		cmbSavedSearch = new ComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY);
		toolkit.adapt(cmbSavedSearch.getCombo());
		cmbSavedSearch.setContentProvider(ArrayContentProvider.getInstance());
		cmbSavedSearch.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbSavedSearch.setLabelProvider(new NamedItemLabelProvider());
	}
	
	private void createAdvancedSearch(Composite parent){
		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(core, "TODO");
		//TODO: Advanced Search
	}
	private void createBasicSearch(Composite parent){
		
		Composite search = toolkit.createComposite(parent, SWT.NONE);
		search.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		search.setLayout(new GridLayout());
		((GridLayout)search.getLayout()).marginWidth = 0;
		((GridLayout)search.getLayout()).marginHeight = 0;
		
		Composite core = toolkit.createComposite(search, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(core, "Search:");
		
		txtSearch = new FilterComposite(core, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				doBasicSearch(searchDelay);
			}
		});
		
		toolkit.createLabel(core, "EntityType:");
		
		cmbEntityType = new TableComboViewer(core, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.adapt(cmbEntityType.getTableCombo());
		cmbEntityType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				doBasicSearch(500);
			}
		});
		
		Composite bottom = toolkit.createComposite(search,  SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnSearch = toolkit.createButton(bottom, "Search", SWT.PUSH);
		btnSearch.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBasicSearch(0);	
			}
		});
		Hyperlink saveSearch = toolkit.createHyperlink(bottom, "Save Search", SWT.NONE);
		saveSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
	}

	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_ALL) IntelEntity entity){
		doSearch(null, searchDelay);
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_ALL) List<IntelEntity> entity){
		doSearch(null, searchDelay);
	}
	
	@Inject
	@Optional
	private void entityTypesModified(@UIEventTopic(IntelEvents.ENTITY_TYPE_ALL) IntelEntityType type){
		entityTypeJob.schedule();
		doSearch(null, searchDelay);
	}
	
	// @Optional
	// @Inject
	// private void
	// dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object
	// data){
	// }

	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
		toolkit.dispose();
		if (boldFont != null) boldFont.dispose();
	}
	
	private void doBasicSearch(long delay){
		List<IntelEntityType> filters = new ArrayList<IntelEntityType>();
		for (Iterator<?> iterator = ((IStructuredSelection)cmbEntityType.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if(x instanceof IntelEntityType){
				filters.add((IntelEntityType) x);
			}
			
		}
		BasicEntitySearch search = new BasicEntitySearch(txtSearch.getPatternFilter(), filters);
		doSearch(search, delay);

	}
	
	private void doSearch(IIntelEntitySearch search, long delay){
		if (search != null){
			searchJob.setSearch(search);
		}
		searchJob.cancel();
		searchJob.schedule(delay);
	}
	
	public static class EntitySearchViewWrapper extends DIViewPart<EntitySearchView>{
		public EntitySearchViewWrapper() {
			super(EntitySearchView.class);
		}
	}
	
	private class LoadEntityTypeJob extends Job{

			public LoadEntityTypeJob() {
				super("Refreshing Entity Types");
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> types = new ArrayList<Object>();
				Session session = HibernateManager.openSession();
				try{
					types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(session, SmartDB.getCurrentConservationArea()));
				}finally{
					session.close();
				}
				
				types.add(0, "All Types");
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						cmbEntityType.setInput(types);
					}		
				});
				return Status.OK_STATUS;
			}
	    }
	    
	    private class LoadSavedSearchJob extends Job{

			public LoadSavedSearchJob() {
				super("Refreshing Saved Searches");
			}

			@SuppressWarnings("unchecked")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> types = new ArrayList<Object>();
				Session session = HibernateManager.openSession();
				try{
					
					types.addAll(session.createCriteria(IntelEntitySearch.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
							.list());
									
				}finally{
					session.close();
				}
				
				types.add(0, "");
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						cmbSavedSearch.setInput(types);
					}		
				});
				return Status.OK_STATUS;
			}
	    }

}