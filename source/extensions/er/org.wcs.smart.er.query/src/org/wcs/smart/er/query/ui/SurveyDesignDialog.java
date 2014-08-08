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
package org.wcs.smart.er.query.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog for selecting survey design.
 * 
 * @author Emily
 *
 */
public class SurveyDesignDialog extends TitleAreaDialog{

	private Session session;
	private SurveyDesign sd;
	private ComboViewer cmbViewer;
	
	public SurveyDesignDialog(Shell parentShell) {
		super(parentShell);
	}
	
	
	@Override
	public boolean close(){
		if (session != null && session.isOpen()){
			session.close();
		}
		return super.close();
	}
	
	@Override
	public void okPressed(){
		Object selection =  ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
		if (selection instanceof SurveyDesign){
			sd = (SurveyDesign)selection;
		}else{
			sd = null;
		}
		super.okPressed();
	}
	
	/**
	 * 
	 * @return the selected survey design
	 */
	public SurveyDesign getSelectedDesign(){
		return this.sd;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		setTitle(Messages.SurveyDesignDialog_Title);
		getShell().setText(Messages.SurveyDesignDialog_Title);
		setMessage(Messages.SurveyDesignDialog_Message);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.SurveyDesignDialog_DesignLabel);
	
		session = HibernateManager.openSession();
		
		cmbViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		List<SurveyDesign> sds = SurveyHibernateManager.getInstance().getActiveSurveys(session);
		List<Object> all = new ArrayList<Object>();
		all.addAll(sds);
		all.add(Messages.SurveyDesignDialog_AllDesignsLabel);
		cmbViewer.setInput(all);
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (sds.size() > 0){
			cmbViewer.setSelection(new StructuredSelection(sds.get(0)));
		}
	
		return composite;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
