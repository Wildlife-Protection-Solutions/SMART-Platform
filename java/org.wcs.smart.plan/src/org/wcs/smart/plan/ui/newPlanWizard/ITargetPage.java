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

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.plan.model.PlanTarget;

/**
 * A page to create or update plan target information.
 * This page is added to the the TargetPropertyPage.
 *  
 * @author Emily
 *
 */
public interface ITargetPage {

	/**
	 * Creates a new target
	 * @return create a new empty target
	 */
	public PlanTarget createTarget();
	
	/**
	 * Updates the plan target with the values on this
	 * page
	 * 
	 * @param pt plan target to update
	 */
	public void updateTarget(PlanTarget pt);
	
	/**
	 * Initialized the ui components with the information
	 * in the plan target
	 * 
	 * @param pt
	 */
	public void initPage(PlanTarget pt);
	
	/**
	 * The gui name of the page
	 * @return
	 */
	public String getPageName();
	
	/**
	 * validates the target page
	 * @return <code>true</code> if validation successful, <code>false</code> otherwise
	 */
	public boolean validate();
	
	/**
	 * Creates the ui to represent the target
	 * @param parent
	 * @param style
	 * @return
	 */
	public Composite createComponent(Composite parent, int style);
}
