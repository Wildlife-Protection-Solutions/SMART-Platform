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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PlanTarget;

/**
 * Creates a new target list viewer.
 * 
 * @author jeffloun
 *
 */
public class TargetListViewer{
	
	private TableViewer v;
	

	public TargetListViewer(Composite parent) {
		Composite table = new Composite(parent, SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		
		v = new TableViewer(table, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		v.getTable().setHeaderVisible(true);
		v.getTable().setLinesVisible(true);
		
	
		TableViewerColumn viewerColumn = new TableViewerColumn(v,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();

		layout.setColumnData(column, new ColumnWeightData(34,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setText(Messages.TargetListViewer_TargetName_Label);
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
		column.setText(Messages.TargetListViewer_Summary_Label);
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				String x = ((PlanTarget)element).getSummary(Locale.getDefault());
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}			 
		});
		v.setContentProvider(ArrayContentProvider.getInstance());

		table.setLayout(layout);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	
	public TableViewer getViewer(){
		return this.v;
	}
	
	public void updateModel(List<PlanTarget> t) {
		if(t != null){
			v.setInput(t);
			v.refresh();
		}
	}


	public IStructuredSelection getSelection() {
		return (IStructuredSelection) v.getSelection();
	}


	public void initValues(List<PlanTarget> targets) {
		if(targets != null){
			v.setInput(targets.toArray());
		}
	}
	

}
