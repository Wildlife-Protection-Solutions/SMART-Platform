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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.IPatrolItemChangeListener;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.ui.NewPatrolWizardPage;
import org.wcs.smart.patrol.ui.PatrolAttributeComposite;

/**
 * Wizard page to set the patrol id.
 * @author Jeff
 * @since 1.0.0
 */
public class PatrolAttributeWizardPage extends NewPatrolWizardPage implements IPatrolItemChangeListener{

	public static final String ID = "newpatrol.customattributes"; //$NON-NLS-1$
	
	private PatrolAttributeComposite attributeComposite = null;
	private List<PatrolAttribute> attributes;
	private ScrolledComposite scroll;
	
	/**
	 */
	public PatrolAttributeWizardPage() {
		super(ID); 
	}

	public void setAttributes(List<PatrolAttribute> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);

		attributeComposite = new PatrolAttributeComposite(attributes);
		attributeComposite.addChangeListener(this);
		Composite acomp = attributeComposite.createComponent(scroll, SWT.NONE);
		scroll.addListener(SWT.Resize, e->{
			scroll.setMinSize(scroll.getSize().x - scroll.getVerticalBar().getSize().x,  acomp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		});
		scroll.setContent(acomp);
		scroll.setMinSize(acomp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		super.setControl(scroll);
		
		setTitle(Messages.PatrolAttributeWizardPage_Title);
		setMessage(Messages.PatrolAttributeWizardPage_Message);
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean updateModel(Patrol p, Session session) {
		if (attributeComposite.updatePatrol(p, session)){
			setPageComplete(true);
			return true;
		}else{
			setPageComplete(false);
			return false;
		}
	}
	
	/**
	 * @see org.wcs.smart.patrol.ui.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public void initModel(Patrol p, Session session) {
		attributeComposite.setValues(p, session);
		scroll.layout(true,  true);
	}

	@Override
	public void itemChanged() {
		setPageComplete(attributeComposite.isValid());
	}

}