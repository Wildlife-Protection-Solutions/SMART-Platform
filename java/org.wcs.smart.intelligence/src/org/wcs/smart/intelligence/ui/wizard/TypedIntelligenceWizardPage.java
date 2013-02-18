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
package org.wcs.smart.intelligence.ui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.panel.IInputChangeListener;
import org.wcs.smart.intelligence.ui.panel.IntelligenceComposite;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory.PanelType;

/**
 * Instance  of {@link IntelligenceWizardPage} that is based on a certain
 * single content panel that is created by {@link IntelligenceCompositeFactory}
 * based on provided {@link PanelType}
 * 
 * @author elitvin
 *
 */
public class TypedIntelligenceWizardPage extends IntelligenceWizardPage {

	private IntelligenceComposite composite;
	private PanelType panelType;

	/**
	 * @param pageName
	 */
	protected TypedIntelligenceWizardPage(PanelType type) {
		super(IntelligenceCompositeFactory.getInstance().getTitle(type));
		this.panelType = type;
	}

	/**
	 * Creates nested {@link IntelligenceComposite} based on provided {@link PanelType}
	 * 
	 * @param parent parent composite
	 */
	@Override
	public void createControl(Composite parent) {
		composite = IntelligenceCompositeFactory.getInstance().createComposite(parent, SWT.NONE, panelType);
		setPageComplete(composite.isDataValid());
		composite.addInputChangeListener(new ControlChangeListener());
		setControl(composite);
		setTitle(IntelligenceCompositeFactory.getInstance().getTitle(panelType));
		setMessage(composite.getMessage());
	}


	/**
	 * Updates the current intelligence with the new values inputed
	 * in the wizard page.
	 * 
	 * @param intelligence intelligence to update
	 * @return <code>true</code> of model updated; <code>false</code> if error 
	 */
	protected boolean updateModel(Intelligence intelligence) {
		return composite.updateModel(intelligence);
	}

	@Override
	protected void initFromModel(Intelligence intelligence) {
		composite.initFromModel(intelligence);
	}
	
	/**
	 * @author elitvin
	 */
	private class ControlChangeListener implements IInputChangeListener {
		@Override
		public void inputChanged(IntelligenceComposite source) {
			TypedIntelligenceWizardPage.this.setPageComplete(source.isDataValid());
			TypedIntelligenceWizardPage.this.setErrorMessage(source.getErrorMessage());
		}
	}

}
