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
package org.wcs.smart.patrol.query.ui.itempanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.IExtensionOption;
import org.wcs.smart.patrol.query.parser.IQueryFilterPatrolContribution;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolContributionFactory;
import org.wcs.smart.patrol.query.ui.itempanel.QueryFilterContentProvider.RootNodeType;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.ui.itempanel.AbstractQueryItemPanel;

/**
 * Panel displaying default filter items for filtering
 * query results.
 * 
 * @author Emily
 *
 */
public class QueryFilterPanel extends AbstractQueryItemPanel {
	
	public static final String ID = "org.wcs.smart.query.patrol.filterPanel"; //$NON-NLS-1$
	
	private Composite main = null;
	private TreeViewer filterTreeViewer;
	
	/*
	 * listener for refreshing areas
	 */
	private IAreaModifiedListener areaListener = new IAreaModifiedListener() {
		@Override
		public void areasUpdated(AreaType type) {
			//clear areas from content provider & refresh tree
			((QueryFilterContentProvider)filterTreeViewer.getContentProvider()).clearAreas();
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					filterTreeViewer.refresh();
				}});
		}
	};
	
	public QueryFilterPanel() {
		
	}

	protected Composite createPanel(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		main.setLayout(gl);
		
		ConservationAreaManager.getInstance().addAreaChangeListener(areaListener);
		main.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				ConservationAreaManager.getInstance().removeAreaChangeListener(areaListener);
			}
		});
		
		// search tree
		final PatternFilter patternFilter = new PatternFilter() {
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((ITreeContentProvider) ((TreeViewer) viewer)
						.getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(
							viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((LabelProvider) ((TreeViewer) viewer)
						.getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,
						element));
			}
		};

		FilteredTree fTree = new FilteredTree(main, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		fTree.setBackground(Display.getDefault()
				.getSystemColor(SWT.COLOR_WHITE));
		filterTreeViewer = fTree.getViewer();
		filterTreeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		filterTreeViewer.setLabelProvider(new QueryFilterLabelProvider());
		filterTreeViewer.setContentProvider(new QueryFilterContentProvider(
				filterTreeViewer));
		filterTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		filterTreeViewer.setAutoExpandLevel(2);
		filterTreeViewer.setInput(LOADING_TEXT);
		Button btnAdd = new Button(main, SWT.PUSH);
		btnAdd.setText(Messages.QueryFilterView_AddToQueryButton);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addItem();
			}
		});
		refreshPanel();
		return main;
	}
	
	private void addItem(){
		addQueryItem((IStructuredSelection) filterTreeViewer.getSelection());
	}
	
	public void refreshPanel(){
		if (filterTreeViewer != null){
			filterTreeViewer.setInput(LOADING_TEXT);
			filterTreeViewer.refresh();
			refreshJob.cancel();
			refreshJob.schedule();
		}
	}

	private List<IExtensionOption> findContributedPatrolQueryOptions() {
		List<IExtensionOption> items = new ArrayList<IExtensionOption>();
		for (IQueryFilterPatrolContribution contribution : PatrolContributionFactory.getContributions()) {
			items.addAll(contribution.getOptions());
		}
		return items;
	}
	
	private Job refreshJob = new Job(Messages.QueryFilterPanel_RefreshTree_JobTitle) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Object, Object> input = new HashMap<Object, Object> ();
			
			if (SmartDB.isMultipleAnalysis()){
				input.put(QueryFilterContentProvider.ROOT_NODES,new QueryFilterContentProvider.RootNodeType[]{RootNodeType.PATROL_FILTERS, RootNodeType.DATA_MODEL_FILTERS, RootNodeType.OTHER_ITEMS}); 
				
				List<Object> options = new ArrayList<Object>();
				options.addAll(Arrays.asList(PatrolQueryOptions.SHARED_PATROL_FILTER_OPTIONS));
				options.addAll(findContributedPatrolQueryOptions());
				input.put(QueryFilterContentProvider.RootNodeType.PATROL_FILTERS, options.toArray());
			}else{
				input.put(QueryFilterContentProvider.ROOT_NODES,QueryFilterContentProvider.RootNodeType.values());
				
				List<Object> options = new ArrayList<Object>();
				options.addAll(Arrays.asList(PatrolQueryOptions.PATROL_FILTER_OPTIONS));
				options.addAll(findContributedPatrolQueryOptions());
				input.put(QueryFilterContentProvider.RootNodeType.PATROL_FILTERS, options.toArray());
			}
			input.put(QueryFilterContentProvider.RootNodeType.DATA_MODEL_FILTERS, QueryDataModelManager.getInstance().getDataModel());

			Display.getDefault().asyncExec(new Runnable(){

				@Override
				public void run() {
					filterTreeViewer.setInput(input);
					filterTreeViewer.refresh();
				}
				
			});
			return Status.OK_STATUS;
		}

	};

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Composite getComposite(Composite parent) {
		if (main == null){
			main = createPanel(parent);
		}
		return main;
	}
}
