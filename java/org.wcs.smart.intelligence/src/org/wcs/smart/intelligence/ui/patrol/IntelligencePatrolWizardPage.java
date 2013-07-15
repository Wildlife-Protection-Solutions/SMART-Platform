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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.ui.patrol.PatrolMotivationComposite.IPartolMotivationChangeListener;
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

	public IntelligencePatrolWizardPage() {
		super(Messages.IntelligencePatrolWizardPage_PageName);
	}

	@Override
	public void createControl(Composite parent) {
		content = new PatrolMotivationComposite(parent, SWT.NONE);
		setTitle(Messages.IntelligencePatrolWizardPage_PageTitle);
		setMessage(Messages.IntelligencePatrolWizardPage_Message);
		content.addInputChangeListener(new IPartolMotivationChangeListener() {
			@Override
			public void inputChanged() {
				if (content.getErrorMessage() == null) {
					setPageComplete(true);
					setErrorMessage(null);
				} else {
					setPageComplete(false);
					setErrorMessage(content.getErrorMessage());
				}
			}
		});
		setControl(content);
	}

	@Override
	public boolean updateModel(Patrol p, Session session) {
		return content.updateModel(p);
	}

	@Override
	public void initModel(Patrol p, Session session) {
		content.initFromModel(p, session, null);
	}

	@Override
	public void save(Patrol p, Session session) throws Exception {
		IntelligenceHibernateManager.savePatrolIntelligences(session, p, content.getCurrentIntelligences());
	}
}
