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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
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
	private SurveyDesign current;
	
	public SurveyDesignDialog(Shell parentShell, SurveyDesign current) {
		super(parentShell);
		this.current = current;
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
		if (selection instanceof SurveyDesignEditorInput &&
			((SurveyDesignEditorInput)selection).getUuid() != null){
			sd = (SurveyDesign) session.load(SurveyDesign.class, ((SurveyDesignEditorInput)selection).getUuid());
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
		
		List<SurveyDesignEditorInput> sds = SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(session, null);
		Collections.sort(sds, new Comparator<SurveyDesignEditorInput>() {

			@Override
			public int compare(SurveyDesignEditorInput arg0,
					SurveyDesignEditorInput arg1) {
				if (arg0.getState().equals(arg1.getState())){
					return Collator.getInstance().compare(arg0.getName(), arg1.getName());
				}
				if (arg0.getState() == SurveyDesign.State.ACTIVE){
					return -1;
				}
				return 1;
			}
		});
		boolean hasAll = false;
		SurveyDesignEditorInput allItem = new SurveyDesignEditorInput(Messages.SurveyDesignDialog_AllDesignsLabel, null, null, null);
		List<Object> all = new ArrayList<Object>();
		all.addAll(sds);
		for (int i = 0; i < all.size(); i++){
			if (all.get(i) instanceof SurveyDesignEditorInput
					&& (((SurveyDesignEditorInput)all.get(i)).getState() == SurveyDesign.State.INACTIVE)){
				all.add(i, Messages.SurveyDesignDialog_InactiveSeparator);
				all.add(i, allItem);
				hasAll = true;
				break;
			}
		}
		if (!hasAll){
			all.add(allItem);
		}
		
		cmbViewer.setInput(all);
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (current != null){
			cmbViewer.setSelection(new StructuredSelection(new SurveyDesignEditorInput(null, current.getUuid(), null, null)));	
		}else{
			cmbViewer.setSelection(new StructuredSelection(allItem));
			
		}
		cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
				if (!(selection instanceof SurveyDesignEditorInput)){
					getButton(IDialogConstants.OK_ID).setEnabled(false);
				}else{
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		return composite;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}

}
