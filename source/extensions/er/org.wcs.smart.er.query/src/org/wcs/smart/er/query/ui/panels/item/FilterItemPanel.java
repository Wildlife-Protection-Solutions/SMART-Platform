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
package org.wcs.smart.er.query.ui.panels.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.MissionTrackQueryType;
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.ui.itempanel.AreaTreeNode;
import org.wcs.smart.query.common.ui.itempanel.DataModelTreeNode;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.ItemTreeNodeContentProvider;
import org.wcs.smart.query.common.ui.itempanel.ItemTreeNodeTree;
import org.wcs.smart.query.common.ui.itempanel.OperatorsTreeNode;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.itempanel.AbstractQueryItemPanel;

/**
 * The filter item panel for survey queries.
 * 
 * @author Emily
 *
 */
public class FilterItemPanel extends AbstractQueryItemPanel implements ISurveyPanel {
	
	public static final String ID = "org.wcs.smart.er.query.survey.filterItemPanel"; //$NON-NLS-1$

	private Composite main = null;
	private TreeViewer filterTreeViewer;
	
	private AreaTreeNode areaNode;
	private FiltersTreeNode surveyNode;
	private SurveyDesign currentDesign;
	
	private IQueryType qType;
	/*
	 * listener for refreshing areas
	 */
	private IAreaModifiedListener areaListener = new IAreaModifiedListener() {
		@Override
		public void areasUpdated(AreaType type) {
			//clear areas from content provider & refresh tree
			if (areaNode != null){
				areaNode.clearAreas();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						filterTreeViewer.refresh();
					}});
			}
		}
	};
	public FilterItemPanel() {
		this.qType = QueryTypeManager.getInstance().findQueryType(SurveyObservationQueryType.KEY);
	}
	public FilterItemPanel(IQueryType qType) {
		this.qType = qType;
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
		
		List<IItemTreeNode> nodes = new ArrayList<IItemTreeNode>();
		surveyNode =  new FiltersTreeNode(qType);
		nodes.add(surveyNode);
		
		if (!qType.getKey().equals(MissionTrackQueryType.KEY)){
			nodes.add(new DataModelTreeNode(DataModelTreeNode.Type.FILTER));	
		}
		
		if (!SmartDB.isMultipleAnalysis()){
			areaNode = new AreaTreeNode(Messages.SurveyFilterItemPanel_AreaFilterTreeNode);
			nodes.add(areaNode);
		}
		nodes.add(new OperatorsTreeNode());
		
		ItemTreeNodeTree tree = new ItemTreeNodeTree(main, SWT.NONE, nodes);
		filterTreeViewer = tree.getTreeViewer();

		filterTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		filterTreeViewer.setAutoExpandLevel(2);
		filterTreeViewer.setInput(LOADING_TEXT);
		Button btnAdd = new Button(main, SWT.PUSH);
		btnAdd.setText(Messages.SurveyFilterItemPanel_AddToQueryButton);
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
		addQueryItem( ItemTreeNodeContentProvider.unwrapSelection((IStructuredSelection) filterTreeViewer.getSelection()));
	}
	
	/**
	 * Refreshes the panel contents
	 * 
	 * @param currentDesign the current survey design 
	 */
	@SuppressWarnings("unchecked")
	public void refreshPanel(SurveyDesign currentDesign){
		this.currentDesign = currentDesign;
		if (filterTreeViewer != null){
			Object input = filterTreeViewer.getInput();
			if (input instanceof HashMap){
				HashMap<Object, Object> input2 = (HashMap<Object, Object>) input;
				input2.put(FiltersTreeNode.KEY, new Object[]{currentDesign, surveyNode});
		
				filterTreeViewer.setInput(input2);
				filterTreeViewer.refresh();
			}
		}
	}
	
	
	@Override
	public void refreshPanel(){
		if (filterTreeViewer != null){
			filterTreeViewer.setInput(LOADING_TEXT);
			filterTreeViewer.refresh();
			refreshJob.cancel();
			refreshJob.schedule();
		}
	}

	
	private Job refreshJob = new Job(Messages.SurveyFilterItemPanel_JobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Object, Object> input = new HashMap<Object, Object> ();

			List<Operator> ops = new ArrayList<Operator>();
			ops.add(Operator.NOT);
			ops.add(Operator.BRACKETS);
			
			input.put(OperatorsTreeNode.KEY, ops);
			input.put(DataModelTreeNode.KEY,  QueryDataModelManager.getInstance().getDataModel());
			input.put(FiltersTreeNode.KEY, new Object[]{currentDesign, surveyNode});
			
//			if (SmartDB.isMultipleAnalysis()){
//				List<Object> options = new ArrayList<Object>();
//				options.addAll(Arrays.asList(PatrolQueryOptions.SHARED_PATROL_FILTER_OPTIONS));
//				options.addAll(findContributedPatrolQueryOptions());
//				input.put(PatrolFilterTreeItem.KEY, options.toArray());
//			}else{
//				List<Object> options = new ArrayList<Object>();
//				options.addAll(Arrays.asList(PatrolQueryOptions.PATROL_FILTER_OPTIONS));
//				options.addAll(findContributedPatrolQueryOptions());
//				input.put(PatrolFilterTreeItem.KEY, options.toArray());
//			}

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

	@Override
	public Query getQuery() {
		return null;
	}
	
	@Override
	public SurveyDesign getSurveyDesign() {
		return null;
	}
}

