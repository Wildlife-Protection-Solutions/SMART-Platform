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
package org.wcs.smart.intelligence.ui.patrol;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;

/**
 * Wizard page for the new patrol wizard that collects
 * the information about intelligence information that motivated the patrol.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligencePatrolWizardPage extends NewPatrolWizardPage {

	private PatrolMotivationComposite content;
	private List<Intelligence> allIntelligences;

	public IntelligencePatrolWizardPage() {
		super("Intelligence Page");
	}

	@Override
	public void createControl(Composite parent) {
		allIntelligences = IntelligenceHibernateManager.getIntelligences();
		
		content = new PatrolMotivationComposite(parent, SWT.NONE);
		setTitle("Patrol Motivation");
		setMessage("Select if this patrol is based on intelligence");
		setControl(content);
	}

	@Override
	public boolean updateModel(Patrol p) {
		return true;
	}

	@Override
	public void initModel(Patrol p, Session session) {
    	List<Intelligence> current = new ArrayList<Intelligence>();
    	content.getSelectComposite().setItemsData(allIntelligences, current);
	}

}
