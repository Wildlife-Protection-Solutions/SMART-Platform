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
package org.wcs.smart.intelligence.ui.editor;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.job.SaveIntelligenceJob;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.panel.IInputChangeListener;
import org.wcs.smart.intelligence.ui.panel.IntelligenceComposite;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory;
import org.wcs.smart.intelligence.ui.panel.IntelligenceCompositeFactory.PanelType;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * EditItelligenceItemDialog
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class EditItelligenceItemDialog extends AbstractPropertyJHeaderDialog {

	private IntelligenceComposite content;
	
	private PanelType panelType;
	private Intelligence intelligence;

	IInputChangeListener inputChangeListener = new IInputChangeListener() {			
		@Override
		public void inputChanged(IntelligenceComposite source) {
			setChangesMade(true);
			EditItelligenceItemDialog.this.setErrorMessage(source.getErrorMessage());
			if (getButton(IDialogConstants.OK_ID) != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(source.isDataValid());
			}
		}
	};
	
	/**
	 * @param parent
	 * @param panelType
	 * @param intelligence
	 */
	public EditItelligenceItemDialog(Shell parent, PanelType panelType, Intelligence intelligence) {
		super(parent, IntelligenceCompositeFactory.getInstance().getTitle(panelType));
		this.panelType = panelType;
		this.intelligence = intelligence;
	}

	@Override
	protected Composite createContent(Composite parent) {
		content = IntelligenceCompositeFactory.getInstance().createComposite(parent, SWT.NONE, panelType);
		content.initFromModel(intelligence);
		content.addInputChangeListener(inputChangeListener);
		setChangesMade(false);
		setTitle(IntelligenceCompositeFactory.getInstance().getTitle(panelType));
		setMessage(content.getMessage());
		return content;
	}

	@Override
	protected boolean performSave() {
		content.updateModel(intelligence);
		
        SaveIntelligenceJob saveIntelligenceJob = new SaveIntelligenceJob(intelligence);    
    	saveIntelligenceJob.schedule();
    	try {
			saveIntelligenceJob.join(); //we don't want to close editor if save failed
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    	if (Status.OK_STATUS.equals(saveIntelligenceJob.getResult())) {
			setChangesMade(false);
			IntelligenceEventManager.getInstance().intelligenceChanged(0, intelligence);
			return true;
    	}
		return false;
	}
	
}
