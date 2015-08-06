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
import java.util.Locale;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.internal.PlanLabelProvider;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.PlanTargetStatus;



/**
 * Creates a new target list viewer.
 * 
 * @author jeffloun
 *
 */
public class TargetProgressViewer{
	
	private enum TargetTableColumn{
		PLANNAME(Messages.TargetProgressViewer_Column_Plan,15),
		TARGETNAME(Messages.TargetProgressViewer_Column_Name, 20),
		SUMMARY(Messages.TargetProgressViewer_Column_Summary,45),
		STATUS(Messages.TargetProgressViewer_Column_Status, 30),
		STATUS_ICON("", 5); //$NON-NLS-1$
		
		public String guiName;
		public int defaultWidth;
		
		private TargetTableColumn(String name, int defaultWidth){
			this.guiName = name;
			this.defaultWidth = defaultWidth;
		}
		
		public String getValue(PlanTarget target){
			String value = null;
		
			if (this == PLANNAME){
				value = target.getPlan().getId();
			}else if (this == TARGETNAME){
				value = target.getName();
			}else if (this == SUMMARY){
				value = target.getSummary(Locale.getDefault());
			}else if (this == STATUS){
				PlanTargetStatus status = target.getCurrentStatus();
				if (status == null){
					value = Messages.TargetProgressViewer_Computing_Label;
				}else{
					value = status.getDisplayString(Locale.getDefault());
				}
			}
			if (value == null){
				return ""; //$NON-NLS-1$
			}
			return value.trim();
		}
		
	}
	private TableViewer v;
	private Label lbl;
	
	private TargetTableColumn[] myColumns = null;
	
	/**
	 * Creates a new target progress viewer with
	 * planName, targetName, summary status and status icon columns.
	 * 
	 * @param parent
	 * @param includePlan if the plan name should be included
	 * @param toolkit parent editor toolkit
	 */
	public TargetProgressViewer(Composite parent, boolean includePlan, FormToolkit toolkit){
		this(parent, TargetTableColumn.values(), toolkit);
	}
	
	/**
	 * Creates a new target progress viewer with 
	 * targetname, summary, status, and status icon columns
	 * @param parent
	 * @param toolkit parent editor toolkit
	 */
	public TargetProgressViewer(Composite parent, FormToolkit toolkit) {
		this(parent, new TargetTableColumn[]{
				TargetTableColumn.TARGETNAME, 
				TargetTableColumn.SUMMARY, 
				TargetTableColumn.STATUS, 
				TargetTableColumn.STATUS_ICON}, toolkit);
		
	}
		
	private TargetProgressViewer(Composite parent, TargetTableColumn[] columns, FormToolkit toolkit){
		this.myColumns = columns;
		create(parent, toolkit);
	}
	
	
	private void create(Composite parent, FormToolkit toolkit){
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lbl = toolkit.createLabel(container, Messages.TargetProgressViewer_TargetsComplete_Label); 
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite table = new Composite(container, SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		
		v = new TableViewer(table, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		v.getTable().setHeaderVisible(true);
		v.getTable().setLinesVisible(true);
		
	
		
		//create a blank empty column that is hidden
		//see: 
		//http://stackoverflow.com/questions/12641354/putting-an-image-in-to-a-jface-table-cell-is-causing-gap-for-image-to-appear-in
		//note this needs testing on mac.
		TableViewerColumn viewerColumn = new TableViewerColumn(v,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setText(""); //$NON-NLS-1$
		column.setResizable(false);
		column.setMoveable(false);
		column.setWidth(0);
		layout.setColumnData(column, new ColumnWeightData(0,0,false));
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				return ""; //$NON-NLS-1$
			}
		 
		});
		
		for (final TargetTableColumn c : myColumns){
			viewerColumn = new TableViewerColumn(v,SWT.NONE);
			column = viewerColumn.getColumn();

			layout.setColumnData(column, new ColumnWeightData(c.defaultWidth,ColumnWeightData.MINIMUM_WIDTH, true));
			column.setText(c.guiName);
			column.setResizable(true);
			column.setMoveable(true);
			
			
			if (c == TargetTableColumn.STATUS_ICON){
				viewerColumn.setLabelProvider(new ColumnLabelProvider() {
		            @Override
		            public void update(ViewerCell cell) {
		            	PlanTargetStatus status = ((PlanTarget)cell.getElement()).getCurrentStatus();
		            	if (status != null){
		            		cell.setImage(PlanLabelProvider.getImage(status.getStatus()));
		            	}
		            }
				});
			}else{
				viewerColumn.setLabelProvider(new ColumnLabelProvider(){
					@Override
					public String getText(Object element) {
						return c.getValue((PlanTarget)element);
					}
				 
				});
			}
		}
		
		v.setContentProvider(ArrayContentProvider.getInstance());

		table.setLayout(layout);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)table.getLayoutData()).heightHint = 80;
	}
	
	public TableViewer getViewer(){
		return this.v;
	}
	

	public IStructuredSelection getSelection() {
		return (IStructuredSelection) v.getSelection();
	}


	public void clearStatus(){
		Object x = v.getInput();
		if (x != null && x instanceof List){
			@SuppressWarnings("unchecked")
			List<PlanTarget> targets = (List<PlanTarget>)x;
			for (PlanTarget pt : targets){
				pt.clearCurrentStatus();
			}
		}
		refresh();
	}
	/**
	 * initialize the viewer
	 * @param targets
	 */
	public void initValues(List<PlanTarget> targets) {
		if(targets != null){
			v.setInput(targets);
			refresh();
		}
		
	}
	
	/**
	 * Refresh the status of the targets
	 */
	public void refresh(){
		v.refresh();
		Object x = v.getInput();
		if (x != null && x instanceof List){
			@SuppressWarnings("unchecked")
			List<PlanTarget> targets = (List<PlanTarget>)x;
			int complete = 0;
			for (PlanTarget pt : targets){
				if (pt.getCurrentStatus() != null && (pt.getCurrentStatus().getStatus() == PlanTargetStatus.Status.COMPLETE)){
					complete++;
				}
			}
			
			lbl.setText(Messages.TargetProgressViewer_TargetsComplete_Label + " " + complete + "/" + targets.size()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
	}
	

	
}
