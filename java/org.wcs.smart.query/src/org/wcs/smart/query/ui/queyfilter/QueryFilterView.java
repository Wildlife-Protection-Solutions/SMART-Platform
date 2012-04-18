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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.parser.internal.PatrolFilter.PatrolFilterOption;
import org.wcs.smart.query.ui.SourceProvider;

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
	private TreeViewer tv;

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
					for (Category cat: dm.getCategories()){
						visitCategory(cat);
					}
				}finally{
					session.getTransaction().rollback();
					session.close();
				}
				
				input.put(QueryFilterContentProvider.RootNodeType.DATA_MODEL_FILTERS, dm);
				input.put(QueryFilterContentProvider.RootNodeType.PATROL_FILTERS, PatrolFilterOption.values());
//				input.put(QueryFilterContentProvider.RootNodeType.AREA_FILTERS,"");
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						tv.setInput(input);
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
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 2;
		layout.verticalSpacing = 2;
		layout.marginWidth = 3;
		layout.marginHeight = 3;
		main.setLayout(layout);
		main.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		//search tree
		PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((QueryFilterContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((QueryFilterLabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
			
		};
		FilteredTree fTree = new FilteredTree(main,  SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		fTree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		tv = fTree.getViewer();
		tv.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
		tv.setLabelProvider(new QueryFilterLabelProvider());
		tv.setContentProvider(new QueryFilterContentProvider());
		tv.addDoubleClickListener(new IDoubleClickListener() {			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		tv.setAutoExpandLevel(2);
		
		Button btnAdd = new Button(main, SWT.PUSH);
		btnAdd.setText("Add to Query");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addItem();
			}
		});
	
		initialize();
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
		IStructuredSelection selection = (IStructuredSelection)tv.getSelection();
		provider.setFilterSelection(selection);
	}
}
