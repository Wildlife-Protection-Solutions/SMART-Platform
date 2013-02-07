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

import java.text.Collator;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
	
	/**
	 * Creates a new plan viewer
	 * @param parent
	 */
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
		planViewer.setComparator(new ViewerComparator() {					
		    @Override
		    public int compare(Viewer viewer, Object e1, Object e2) {
		    	if (e1 instanceof Plan && e2 instanceof Plan){	        	
		            return Collator.getInstance().compare(((Plan) e1).getName(), (((Plan) e2).getName()));
		    	}else if (e1 instanceof Plan ){
		    		return 1;
		    	}else if (e2 instanceof Plan){
		    		return -1;
		    	}else{
		    		return Collator.getInstance().compare(e1.toString(), e2.toString()); 
		        }
		    }
		});
	}
	
	/**
	 * 
	 * @return the select plan in the plan viewer
	 */
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

	/**
	 * Set the selected plan
	 * @param selection
	 */
	public void setSelection(Object selection){
		planViewer.setSelection(new StructuredSelection(selection));
	}
	
	/**
	 * Set the root plans
	 * @param roots
	 */
	public void setRootPlans(Object[] roots){
		planViewer.setInput(roots);
		planViewer.refresh();
	}
	
	/**
	 * Refresh the viewer
	 */
	public void refresh(){
		planViewer.refresh();
	}
	
	/**
	 * 
	 * @return the viewer control
	 */

	public Control getControl(){
		return planViewer.getTree();
	}
	public TreeViewer getViewer(){
		return this.planViewer;
	}
	
}
