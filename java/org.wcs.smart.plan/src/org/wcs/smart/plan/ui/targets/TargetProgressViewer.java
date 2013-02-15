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
package org.wcs.smart.plan.ui.targets;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.PlanTargetStatus;
import org.wcs.smart.plan.model.PlanTargetStatus.Status;



/**
 * Creates a new target list viewer.
 * 
 * @author jeffloun
 *
 */
public class TargetProgressViewer{
	
	private TableViewer v;
	private Label lbl;
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	

	public TargetProgressViewer(Composite parent) {
		
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lbl = toolkit.createLabel(container, "Total Targets Complete: 100/100"); 
		
		Composite table = new Composite(container, SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		
		v = new TableViewer(table, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		v.getTable().setHeaderVisible(true);
		v.getTable().setLinesVisible(true);
		
	
		TableViewerColumn viewerColumn = new TableViewerColumn(v,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();

		layout.setColumnData(column, new ColumnWeightData(34,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setText("Target Name");
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				String x = ((PlanTarget)element).getName();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}
			 
		});
		
		viewerColumn = new TableViewerColumn(v,SWT.NONE);
		column = viewerColumn.getColumn();
		layout.setColumnData(column, new ColumnWeightData(66,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setText("Summary");
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				String x = ((PlanTarget)element).getSummary();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}			 
		});
		
		viewerColumn = new TableViewerColumn(v,SWT.NONE);
		column = viewerColumn.getColumn();
		layout.setColumnData(column, new ColumnWeightData(66,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setText("Target Status");
		column.setToolTipText("These totals include values from all patrols associated with the Plan as well as with sub-plans.");
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				PlanTargetStatus status = ((PlanTarget)element).getCurrentStatus();
				if (status == null){
					return "Computing...";
				}else{
					return status.getDisplayString();
				}
			}			 
		});
		
		
		viewerColumn = new TableViewerColumn(v,SWT.NONE);
		column = viewerColumn.getColumn();
		layout.setColumnData(column, new ColumnWeightData(10,10, false));
		column.setText(""); //$NON-NLS-1$
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
            	PlanTargetStatus status = ((PlanTarget)cell.getElement()).getCurrentStatus();
            	if (status != null){
            		cell.setImage(status.getStatus().guiImage);
            	}
            }
		});

		v.setContentProvider(ArrayContentProvider.getInstance());

		table.setLayout(layout);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)table.getLayoutData()).heightHint = 120;
	}
		
	public TableViewer getViewer(){
		return this.v;
	}
	

	public IStructuredSelection getSelection() {
		return (IStructuredSelection) v.getSelection();
	}


	/**
	 * initialize the viewer
	 * @param targets
	 */
	public void initValues(List<PlanTarget> targets) {
		if(targets != null){
			for (PlanTarget pt : targets){
				pt.clearCurrentStatus();
			}
		
			v.setInput(targets);
			v.refresh();
			
			computeStatus.schedule();
		}
		
	}
	
	/**
	 * Refresh the status of the targets
	 */
	public void refreshStatus(){
		Object x = v.getInput();
		if (x != null && x instanceof List){
			List<PlanTarget> targets = (List<PlanTarget>)x;
			for (PlanTarget pt : targets){
				pt.clearCurrentStatus();
			}
			v.refresh();
		}
		computeStatus.schedule();
	}
	
	
	/*
	 * recomputes target status
	 */
	Job computeStatus = new Job("Compute Target Status"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Object x = v.getInput();
			int totalCompleteTargets = 0;
			if (x != null && x instanceof List){
				final List<PlanTarget> targets = (List<PlanTarget>)x;

				for (PlanTarget pt : targets){
					pt.refreshStatus();
					if (pt.getCurrentStatus().getStatus() == Status.COMPLETE){
						totalCompleteTargets ++;
					}
				}
				final int total = totalCompleteTargets;
				Display.getDefault().asyncExec(new Runnable(){
					public void run(){
						v.refresh();
						lbl.setText("Total Targets Complete: " + total + "/" + targets.size());
					}
				});
			}
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}};
	
}
