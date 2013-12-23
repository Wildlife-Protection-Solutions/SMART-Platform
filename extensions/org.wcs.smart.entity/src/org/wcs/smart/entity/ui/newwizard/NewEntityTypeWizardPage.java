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
package org.wcs.smart.entity.ui.newwizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;

/**
 * A new entity wizard page.
 * 
 * @author Emily
 *
 */
public class NewEntityTypeWizardPage extends WizardPage {

	private AbstractEntityComposite contents;
	private NewEntityTypeWizard wizard;
	
	public NewEntityTypeWizardPage(NewEntityTypeWizard wizard, AbstractEntityComposite contents){
		super(contents.getName());
		this.contents = contents;
		this.wizard = wizard;
	}
	
	public boolean canFinish(){
		if (contents instanceof AttributeNameField){
			return true;
		}
		return false;
	}
	
	public boolean canFlipToNextPage(){
		if (contents instanceof AttributeNameField){
			return false;
		}
			
		return getErrorMessage() == null;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 20;
		center.setLayout(gl);
		
		Composite x = contents.createComposite(center);
		x.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		
		contents.addChangeListener(new Listener(){

			@Override
			public void handleEvent(Event event) {
				validate();
			}});
		
		setTitle(contents.getName());
		setMessage(contents.getDescription());
		
		super.setControl(center);
	}

	public String validate(){
		String error = contents.validate();
		if (error == null){
			setErrorMessage(null);
		}else{
			setErrorMessage(error);
		}
		try{
			wizard.getContainer().updateButtons();
		}catch (Exception ex){
			//this may be called before the buttons have been setup to eat this exception
		}
		return error;
	}
	
	public void updateEntityType(EntityType type){
		contents.updateEntityType(type);
	}
	
	public void initPage(EntityType type, Session session){
		contents.initFields(type, session);
		validate();
	}
}
