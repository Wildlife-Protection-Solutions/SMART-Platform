package org.wcs.smart.query.ui.queryfilter;

import java.util.HashMap;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;

public class SummaryFilterPanel extends AbstractQueryItemPanel{
	
	private TreeViewer filterTreeViewer;
	
	public  SummaryFilterPanel(){
	}
	@Override
	public QueryPartPanelType getValidType(){
		return QueryPartPanelType.SUMMARY_ITEM;
	}
	
	@Override
	protected Composite createPanel(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		main.setLayout(gl);
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

		
		FilteredTree fTree = new FilteredTree(main,  SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		fTree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		filterTreeViewer = fTree.getViewer();
		filterTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterTreeViewer.setLabelProvider(new SummaryQueryLabelProvider());
		filterTreeViewer.setContentProvider(new SummaryQueryContentProvider());
		filterTreeViewer.addDoubleClickListener(new IDoubleClickListener() {			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		filterTreeViewer.setAutoExpandLevel(2);
		filterTreeViewer.setInput("Loading");
		Button btnAdd = new Button(main, SWT.PUSH);
		btnAdd.setText(Messages.QueryFilterView_AddToQueryButton);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addItem();
			}
		});
		return main;
	}
	
	private void addItem(){
		addQueryItem((IStructuredSelection) filterTreeViewer.getSelection());
	}
	
	private Job refreshJob = new Job("RefreshSummaryTree"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Object, Object> input = new HashMap<Object, Object> ();
			
			if (SmartDB.isMultipleAnalysis()){
				input.put(SummaryQueryContentProvider.NodeType.PATROL_GROUPBYS, PatrolQueryOptions.SHARED_PATROL_GROUBY_OPTIONS);
			}else{
				input.put(SummaryQueryContentProvider.NodeType.PATROL_GROUPBYS, PatrolQueryOptions.PATROL_GROUBY_OPTIONS);
				
			}
			input.put(SummaryQueryContentProvider.NodeType.PATROL_VALUES, PatrolValueOption.values());
			input.put(SummaryQueryContentProvider.NodeType.PATROL_DATE_GROUPBYS, PatrolQueryOptions.DateGroupByOption.values());
			input.put(SummaryQueryContentProvider.NodeType.GROUP_BY_NODE, QueryDataModelManager.getInstance().getDataModel());
			
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
	public void refreshPanel(){
		filterTreeViewer.setInput(LOADING_TEXT);
		filterTreeViewer.refresh();
		refreshJob.schedule();
	}
}
