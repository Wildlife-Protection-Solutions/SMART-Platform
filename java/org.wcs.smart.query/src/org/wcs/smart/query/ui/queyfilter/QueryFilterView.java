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
package org.wcs.smart.query.ui.queyfilter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryDefinitionType;

/**
 * A view that display the query filter options.
 * <p>
 * The Source Provider is updated when a query filter
 * option is chosen.
 * </p>
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFilterView extends ViewPart {

	public static final String ID ="org.wcs.smart.query.ui.QueryFilter";
	
	//TODO: refresh when data model changes
	private TreeViewer filterTreeViewer;
	private TreeViewer summaryTreeViewer;

	
	private Composite filterComp;
	private Composite summaryComp;

	private Composite main;
	
	private IDataModelListener dataModelChangeListener = new IDataModelListener() {
		@Override
		public void modified() {
			filterTreeViewer.setInput("Loading");
			summaryTreeViewer.setInput("Loading");
			initialize();
		}
	};
	
	public QueryFilterView() {
	}


	/**
	 * A job that initializes the query 
	 * filter options
	 */
	private void initialize(){
		Job j = new Job("initialize query filter tree"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final HashMap<QueryFilterContentProvider.RootNodeType, Object> input = new HashMap<QueryFilterContentProvider.RootNodeType, Object>();
				DataModel dm = null;
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try{
					dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
					//load into memory; no-lazy loading here.
					for (Category cat: dm.getCategories()){
						visitCategory(cat);
					}
					for (Attribute att: dm.getAttributes()){
						att.getAggregations().size();
					}
				}finally{
					session.getTransaction().rollback();
					session.close();
				}
				
				input.put(QueryFilterContentProvider.RootNodeType.DATA_MODEL_FILTERS, dm);
				input.put(QueryFilterContentProvider.RootNodeType.PATROL_FILTERS, PatrolQueryOptions.PATROL_FILTER_OPTIONS);
//				input.put(QueryFilterContentProvider.RootNodeType.AREA_FILTERS,"");
				
				
				//final HashMap<QueryFilterContentProvider.RootNodeType, Object> summaryInput = new HashMap<SummaryContentProvider.RootNodeType, Object>();
				final HashMap<SummaryQueryContentProvider.NodeType, Object> summaryInput = new HashMap<SummaryQueryContentProvider.NodeType, Object> ();
				summaryInput.put(SummaryQueryContentProvider.NodeType.PATROL_VALUES, PatrolValueOption.values());
				summaryInput.put(SummaryQueryContentProvider.NodeType.PATROL_GROUPBYS, PatrolQueryOptions.PATROL_GROUBY_OPTIONS);
				summaryInput.put(SummaryQueryContentProvider.NodeType.PATROL_DATE_GROUPBYS, PatrolQueryOptions.DateGroupByOption.values());
				summaryInput.put(SummaryQueryContentProvider.NodeType.GROUP_BY_NODE, dm);
				
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						filterTreeViewer.setInput(input);
						summaryTreeViewer.setInput(summaryInput);
					}});
					

				return Status.OK_STATUS;
			}};
		j.schedule();
		
	}
	
	/**
	 * visits a child and gets all attributes.
	 * <p>This is to ensure all data model elements
	 * are loaded in the hibernate session.  Circumvents
	 * the hibernate lazy-loading.</p>
	 * @param cat
	 */
	private void visitCategory(Category cat){
		for (Category child : cat.getChildren()){
			visitCategory(child);
			child.getName();
		}
		for (CategoryAttribute ca: cat.getAttributes()){
			ca.getAttribute().getName();
		}
	
	}
	
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		Composite outer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 2;
		layout.verticalSpacing = 2;
		layout.marginWidth = 3;
		layout.marginHeight = 3;		
		outer.setLayout(layout);
		outer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		main = new Composite(outer, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		StackLayout stack = new StackLayout();
		stack.marginHeight = stack.marginWidth = 0;
		main.setLayout(stack);
		

		//search tree
		final PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((ITreeContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((LabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
		};
		
		filterComp = new Composite(main, SWT.NONE);
		((StackLayout)main.getLayout()).topControl = filterComp;
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginWidth = 0;
		filterComp.setLayout(gl);
		
		summaryComp = new Composite(main, SWT.NONE);
		gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginWidth = 0;
		summaryComp.setLayout(gl);
		
		
		FilteredTree fTree = new FilteredTree(filterComp,  SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		fTree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		filterTreeViewer = fTree.getViewer();
		filterTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterTreeViewer.setLabelProvider(new QueryFilterLabelProvider());
		filterTreeViewer.setContentProvider(new QueryFilterContentProvider());
		filterTreeViewer.addDoubleClickListener(new IDoubleClickListener() {			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		filterTreeViewer.setAutoExpandLevel(2);
		filterTreeViewer.setInput("Loading...");
		
		fTree = new FilteredTree(summaryComp,  SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		fTree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		summaryTreeViewer = fTree.getViewer();
		summaryTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
		summaryTreeViewer.setLabelProvider(new SummaryQueryLabelProvider());
		summaryTreeViewer.setContentProvider(new SummaryQueryContentProvider());
		summaryTreeViewer.addDoubleClickListener(new IDoubleClickListener() {			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		summaryTreeViewer.setAutoExpandLevel(2);
		summaryTreeViewer.setInput("Loading...");
		
		
		Button btnAdd = new Button(outer, SWT.PUSH);
		btnAdd.setText("Add to Query");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addItem();
			}
		});
	
		initialize();
		
		
		ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_DEFINITION_TYPE);
		provider.addSourceProviderListener(new ISourceProviderListener() {
			
			@Override
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				if (sourceName.equals(SourceProvider.QUERY_DEFINITION_TYPE)){
					if (sourceValue == QueryDefinitionType.QUERY_FILTER){
						((StackLayout)main.getLayout()).topControl = filterComp;
					}else if (sourceValue == QueryDefinitionType.QUERY_SUMMARY){
						((StackLayout)main.getLayout()).topControl = summaryComp;
					}else{
						//default filter
						((StackLayout)main.getLayout()).topControl = filterComp;
					}
					main.layout();
					patternFilter.setPattern(null);
				}
			}
			
			@Override
			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {
				// TODO Auto-generated method stub
				
			}
		});
		
		DataModelManager.getInstance().addChangeListener(dataModelChangeListener);
	}

	@Override
	public void dispose(){
		super.dispose();
		DataModelManager.getInstance().removeChangeListener(dataModelChangeListener);
	}
	
	@Override
	public void setFocus() {
	}

	/**
	 * Updates the source provider with the 
	 * selection from the data tree.
	 */
	private void addItem(){
		SourceProvider provider = (SourceProvider) ((ISourceProviderService)getSite().getService(ISourceProviderService.class)).getSourceProvider(SourceProvider.SELECTED_FILTERS);
		IStructuredSelection selection =  null;
		if (filterTreeViewer.getTree().isVisible()){
			selection = new QueryFilterSelection((IStructuredSelection)filterTreeViewer.getSelection(), QueryFilterSelection.FilterType.FILTER);
		}else{
			selection = new QueryFilterSelection((IStructuredSelection)summaryTreeViewer.getSelection(), QueryFilterSelection.FilterType.SUMMARY);
		}
		provider.setFilterSelection(selection);
	}
}
