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

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.plan.model.PlanTarget;



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
				String x = ((PlanTarget)element).getStatusDisplayString();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}			 
		});
		
		
		viewerColumn = new TableViewerColumn(v,SWT.NONE);
		column = viewerColumn.getColumn();
		layout.setColumnData(column, new ColumnWeightData(10,10, false));
		column.setText("");
		column.setResizable(true);
		column.setMoveable(true);
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
            	int colour = 0xFF00;
            	if( ! ((PlanTarget)cell.getElement()).computeStatus()){
            		colour = 0x00FF;
            	}
            		
            	            	
            	Image image;
          	    PaletteData palette = new PaletteData(0xFF, 0xFF00, 0xFF0000);
          	    ImageData imageData = new ImageData(10, 10, 24, palette);
            	   	for (int x = 0; x < 10; x++) {
            	   		for (int y = 0; y < 10; y++) {
            	   			imageData.setPixel(x, y, colour);
            	   		}
            	   	}
            	image = new Image(Display.getDefault(), imageData);
            	if(cell != null){
            		cell.setImage(image);
            	}

            }
		});

		
		
		v.setContentProvider(ArrayContentProvider.getInstance());
		

		table.setLayout(layout);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
		
	public TableViewer getViewer(){
		return this.v;
	}
	

	public IStructuredSelection getSelection() {
		return (IStructuredSelection) v.getSelection();
	}


	public void initValues(List<PlanTarget> targets) {
		if(targets != null){
			v.setInput(targets.toArray());
		}
		int totalCompleteTargets = 0;
		for (PlanTarget pt : targets){
			if (pt.computeStatus()){
				totalCompleteTargets++;
			}
		}
		lbl.setText("Total Targets Complete: " + totalCompleteTargets + "/" + targets.size());
	}
	
}
