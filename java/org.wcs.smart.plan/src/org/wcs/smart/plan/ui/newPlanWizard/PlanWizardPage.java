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
package org.wcs.smart.plan.ui.newPlanWizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.wcs.smart.plan.model.Plan;


/**
 * An abstract class for new plan wizard pages.
 * 
 * @author Emily
 * @author jeffloun
 * @since 1.0.0
 */
public abstract class PlanWizardPage extends WizardPage {
	
	private List<IPlanItemChanged> listeners = new ArrayList<IPlanItemChanged>();

	/**
	 * @param pageName the name of the patrol wizard page
	 */
	protected PlanWizardPage(String pageName) {
		super(pageName);
	}

	/**
	 * Updates the current patrol with the new values inputed
	 * in the patrol page.
	 * 
	 * @param p patrol to update
	 * @return <code>true</code> of model updated; <code>false</code> if error 
	 */
	abstract boolean updateModel(Plan p);

	/**
	 * Updates the current page gui components with the values
	 * from the patrol
	 * 
	 * @param p patrol to use when updating gui components
	 * @param session the current hibernate session
	 */
	abstract void initModel(Plan p);
	
	/**
	 * Fires all registered listeners
	 */
	protected void fireChangeListeners(){
		for(IPlanItemChanged listener : listeners){
			listener.itemChanged();
		}
	}
	

}
