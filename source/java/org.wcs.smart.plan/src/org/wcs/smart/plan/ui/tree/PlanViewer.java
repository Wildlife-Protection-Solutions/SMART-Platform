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
package org.wcs.smart.plan.ui.tree;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.plan.model.Plan;

/**
 * A tree viewer for viewing plans in a tree
 * structure.
 * 
 * @author Emily
 *
 */
public class PlanViewer {

	private TreeViewer planViewer;
	
	public PlanViewer(Composite parent) {
		createControl(parent);

	}
	
	protected void createControl(Composite parent){
		planViewer = new TreeViewer(parent);
		
		planViewer.setContentProvider(new PlanContentProvider());
		planViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Plan){
					return ((Plan)element).getName() + " [" + ((Plan)element).getId() + "]";
				}
				return super.getText(element);
			}
			
		});
	}
	
	public Plan getSelectedPlan(){
		if (planViewer.getSelection().isEmpty()){
			return null;
		}
		Object selectedElement = ((StructuredSelection)planViewer.getSelection()).getFirstElement();
		if (selectedElement instanceof Plan){
			return (Plan) selectedElement;
		}
		return null;
	}

	public void setSelection(Object selection){
		planViewer.setSelection(new StructuredSelection(selection));
	}
	
	public void setRootPlans(Object[] roots){
		planViewer.setInput(roots);
		planViewer.refresh();
	}
	
	public void refresh(){
		planViewer.refresh();
	}
	
	public Composite getControl(){
		return planViewer.getTree();
	}
	
	
}
