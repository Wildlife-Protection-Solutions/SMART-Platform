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
package org.wcs.smart.patrol.ui;

import org.eclipse.jface.wizard.WizardPage;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.ui.createpatrol.CreatePatrolWizard;
import org.wcs.smart.patrol.model.Patrol;

/**
 * An abstract class for new patrol wizard pages.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class NewPatrolWizardPage extends WizardPage {


	/**
	 * @param pageName the name of the patrol wizard page
	 */
	protected NewPatrolWizardPage(String pageName) {
		super(pageName);
	}

	public CreatePatrolWizard getWizardInternal(){
		return (CreatePatrolWizard) super.getWizard();
	}
	/**
	 * Updates the current patrol with the new values inputed
	 * in the patrol page.
	 * 
	 * @param p patrol to update
	 * @return <code>true</code> if model updated; <code>false</code> if error 
	 */
	public abstract boolean updateModel(Patrol p);

	/**
	 * Updates the current page gui components with the values
	 * from the patrol
	 * 
	 * @param p patrol to use when updating gui components
	 * @param session the current hibernate session
	 */
	public abstract void initModel(Patrol p, Session session);
	
	/**
	 * Called when the patrol is saved to the database.
	 * 
	 * @param p the patrol saved
	 * @param session current session in open transaction
	 * 
	 * @throws Exception if error occurs while saving 
	 */
	public void save(Patrol p, Session session) throws Exception{}
}
