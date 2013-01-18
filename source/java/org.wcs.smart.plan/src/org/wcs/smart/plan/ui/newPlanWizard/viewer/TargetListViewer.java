package org.wcs.smart.plan.ui.newPlanWizard.viewer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;

public class TargetListViewer {
	
	private TableViewer v;
	

	public TargetListViewer(Composite parent, Plan p) {
		v = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		v.getTable().setHeaderVisible(true);
		v.getTable().setLinesVisible(true);
		
		TableViewerColumn viewerColumn = new TableViewerColumn(v,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		TableColumnLayout layout = (TableColumnLayout) v.getTable().getParent().getLayout();
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
		layout = (TableColumnLayout) v.getTable().getParent().getLayout();
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
		v.setContentProvider(ArrayContentProvider.getInstance());
		if(p.getTargets() != null){
			v.setInput(p.getTargets().toArray());
		}

		
	}
	
	
	public void updateModel(Plan p) {
		v.setInput(p.getTargets().toArray());
		v.refresh();
	}


	public IStructuredSelection getSelection() {
		return (IStructuredSelection) v.getSelection();
	}
	

}
